package com.easytier.ui.pages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.easytier.ui.components.CompactTopBar
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.easytier.data.NetworkConfig
import com.easytier.ui.components.AppIcon
import com.easytier.ui.components.AppIcons
import com.easytier.service.EasyTierService
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneClickPage() {
    val context = LocalContext.current as Activity
    val scope = rememberCoroutineScope()

    var isHostMode by remember { mutableStateOf(true) }
    var isRunning by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var hostConfig by remember { mutableStateOf<NetworkConfig?>(null) }
    var guestCode by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }

    // 用于在协程中更新状态的辅助引用
    val setLoading: (Boolean) -> Unit = { isLoading = it }
    val setRunning: (Boolean) -> Unit = { isRunning = it }
    val setCode: (String) -> Unit = { generatedCode = it }
    val setConfig: (NetworkConfig?) -> Unit = { hostConfig = it }
    val setStatus: (String, Boolean) -> Unit = { msg, err -> statusMessage = msg; statusIsError = err }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cfg = hostConfig ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                EasyTierService.startVpnService(context, cfg.instanceName, cfg.ipv4.ifEmpty { "10.144.144.1" }, 24)
                setRunning(true); setLoading(false)
                setStatus("已成功加入网络 ${cfg.networkName}", false)
            }
        } else {
            scope.launch {
                EasyTierService.stopNetwork(cfg.instanceName)
                setConfig(null)
                setStatus("VPN 授权被拒绝", true); setLoading(false)
            }
        }
    }

    // VPN 授权检查：未授权则弹窗，已授权直接启动
    fun requestVpnAndStart(cfg: NetworkConfig) {
        setConfig(cfg)
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            EasyTierService.startVpnService(context, cfg.instanceName, cfg.ipv4.ifEmpty { "10.144.144.1" }, 24)
            setRunning(true); setLoading(false)
        }
    }

    Scaffold(
        topBar = { CompactTopBar("一键联机") }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 模式切换
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
            Box(Modifier.weight(1f)) {
                ModeButton(
                    selected = isHostMode,
                    icon = AppIcons.Lan,
                    label = "创建网络（房主）",
                    onClick = { isHostMode = true; statusMessage = "" }
                )
            }
            Box(Modifier.weight(1f)) {
                ModeButton(
                    selected = !isHostMode,
                    icon = AppIcons.GroupAdd,
                    label = "加入网络（房客）",
                    onClick = { isHostMode = false; statusMessage = "" }
                )
            }
        }
    }

            if (isHostMode) {
                HostMode(
                    isRunning = isRunning,
                    isLoading = isLoading,
                    generatedCode = generatedCode,
                    hostConfig = hostConfig,
                    statusMessage = statusMessage,
                    statusIsError = statusIsError,
                    onStart = {
                        scope.launch {
                            setLoading(true); setStatus("", false)
                            val (netId, password) = generateRoomCredentials()
                            val config = NetworkConfig(
                                hostname = "Host-${randomSuffix(4)}",
                                networkName = netId,
                                networkSecret = password,
                                dhcp = false, privateMode = true,
                                ipv4 = "11.45.14.1"
                            )
                            val result = EasyTierService.startNetwork(config)
                            if (!result.success) {
                                setLoading(false); setStatus("启动失败: ${result.errorMessage}", true); return@launch
                            }
                            val encoded = encodeConnectionCode(netId, password)
                            setCode(encoded)

                            requestVpnAndStart(config)
                            setStatus("网络已启动，分享下方编码给好友", false)
                        }
                    },
                    onStop = {
                        scope.launch {
                            setLoading(true)
                            EasyTierService.stopVpnService(context)
                            hostConfig?.let { EasyTierService.stopNetwork(it.instanceName) }
                            setRunning(false); setLoading(false); setCode("")
                            setConfig(null); setStatus("网络已停止", false)
                        }
                    },
                    onCopy = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("code", generatedCode))
                        setStatus("已复制到剪贴板", false)
                    }
                )
            } else {
                GuestMode(
                    guestCode = guestCode,
                    onGuestCodeChange = { guestCode = it },
                    isRunning = isRunning,
                    isLoading = isLoading,
                    hostConfig = hostConfig,
                    statusMessage = statusMessage,
                    statusIsError = statusIsError,
                    onJoin = {
                        scope.launch {
                            val code = guestCode.trim()
                            if (code.isBlank()) {
                                setStatus("请输入联机编码", true); return@launch
                            }
                            setLoading(true); setStatus("", false)
                            try {
                                val (netId, password) = decodeConnectionCode(code)
                                if (netId.isEmpty() || password.isEmpty()) {
                                    setLoading(false); setStatus("联机码无效", true); return@launch
                                }
                                val config = NetworkConfig(
                                    hostname = "Guest-${randomSuffix(4)}",
                                    networkName = netId,
                                    networkSecret = password,
                                    dhcp = true, privateMode = true,
                                    servers = mutableListOf("wss://qtet-public.070219.xyz", "tcp://qtet-public2.070219.xyz:27773")
                                )
                                val result = EasyTierService.startNetwork(config)
                                if (!result.success) {
                                    setLoading(false); setStatus("加入失败: ${result.errorMessage}", true); return@launch
                                }
                                requestVpnAndStart(config)
                            } catch (e: Exception) {
                                android.util.Log.e("OneClick", "join failed", e)
                                setLoading(false); setStatus("编码解析失败: ${e.message}", true)
                            }
                        }
                    },
                    onLeave = {
                        scope.launch {
                            setLoading(true)
                            EasyTierService.stopVpnService(context)
                            hostConfig?.let { EasyTierService.stopNetwork(it.instanceName) }
                            setRunning(false); setLoading(false); setConfig(null)
                            setStatus("网络已停止", false)
                        }
                    }
                )
            }

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = if (statusIsError) Color(0xFFEF5350) else Color(0xFF4CAF50),
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// ── UI 子组件 ──

@Composable
private fun ModeButton(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "modeBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface, label = "modeText"
    )
    Surface(onClick = onClick, shape = RoundedCornerShape(10.dp), color = bgColor, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
        }
    }
}

@Composable
private fun HostMode(
    isRunning: Boolean, isLoading: Boolean, generatedCode: String,
    hostConfig: NetworkConfig?, statusMessage: String, statusIsError: Boolean,
    onStart: () -> Unit, onStop: () -> Unit, onCopy: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppIcon(
                if (isRunning) AppIcons.Wifi else AppIcons.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isRunning) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(if (isRunning) "网络运行中" else "点击下方按钮创建网络", fontWeight = FontWeight.SemiBold)
            if (isRunning && hostConfig != null) {
                Text("网络: ${hostConfig.networkName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { if (isRunning) onStop() else onStart() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = if (isRunning) ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350))
                         else ButtonDefaults.buttonColors()
            ) {
                AppIcon(if (isRunning) AppIcons.Stop else AppIcons.Play, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(when { isLoading && isRunning -> "停止中..."; isLoading -> "启动中..."
                     isRunning -> "停止网络"; else -> "启动网络" })
            }
        }
    }

    if (generatedCode.isNotEmpty()) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(AppIcons.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("联机编码", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    IconButton(onClick = onCopy) {
                        AppIcon(AppIcons.Copy, contentDescription = "复制", modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SelectionContainer { Text(generatedCode, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(10.dp)) }
                }
                Spacer(Modifier.height(6.dp))
                Text("将此编码发送给好友，对方在一键联机页面选择\"加入网络\"并粘贴即可", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GuestMode(
    guestCode: String, onGuestCodeChange: (String) -> Unit,
    isRunning: Boolean, isLoading: Boolean, hostConfig: NetworkConfig?,
    statusMessage: String, statusIsError: Boolean,
    onJoin: () -> Unit, onLeave: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("输入联机编码", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = guestCode, onValueChange = onGuestCodeChange,
                placeholder = { Text("粘贴房主分享的编码") },
                modifier = Modifier.fillMaxWidth().height(88.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onJoin, enabled = !isLoading && !isRunning,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                AppIcon(AppIcons.Login, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isLoading) "加入中..." else "加入网络")
            }
        }
    }

    if (isRunning && hostConfig != null) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(AppIcons.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("已加入网络", fontWeight = FontWeight.SemiBold)
                    Text(hostConfig.networkName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLeave) { Text("离开") }
            }
        }
    }
}

// ── Qt-EasyTier 兼容 Base32 ──
// 字母表匹配 Qt 版：不含 I、O，包含 8、9
private val BASE32 = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

private fun base32Encode(data: ByteArray): String {
    val r = StringBuilder()
    var buf = 0; var bits = 0
    for (b in data) {
        buf = (buf shl 8) or (b.toInt() and 0xFF)
        bits += 8
        while (bits >= 5) {
            r.append(BASE32[(buf shr (bits - 5)) and 0x1F])
            bits -= 5
        }
    }
    if (bits > 0) r.append(BASE32[(buf shl (5 - bits)) and 0x1F])
    return r.toString()
}

private fun base32Decode(s: String): ByteArray {
    val r = mutableListOf<Byte>()
    var buf = 0; var bits = 0
    for (ch in s.uppercase()) {
        val idx = BASE32.indexOf(ch); if (idx < 0) continue
        buf = (buf shl 5) or idx; bits += 5
        if (bits >= 8) { r.add(((buf shr (bits - 8)) and 0xFF).toByte()); bits -= 8 }
    }
    return r.toByteArray()
}

// ── Qt-EasyTier 兼容联机码 ──
private const val NET_ID_PREFIX = "QE"

private fun generateRoomCredentials(): Pair<String, String> {
    val charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789*&#$!"
    val netIdBytes = ByteArray(7).apply {
        this[0] = 'Q'.code.toByte(); this[1] = 'E'.code.toByte()
        for (i in 2..6) this[i] = charset[Random.nextInt(charset.length)].code.toByte()
    }
    val pwdBytes = ByteArray(8) { charset[Random.nextInt(charset.length)].code.toByte() }
    val netId = "QtET-OneClick-" + netIdBytes.decodeToString()
    val password = pwdBytes.decodeToString()
    return Pair(netId, password)
}

private fun encodeConnectionCode(networkId: String, password: String): String {
    val pureId = networkId.removePrefix("QtET-OneClick-")
    val encId = base32Encode(pureId.toByteArray())
    val encPwd = base32Encode(password.toByteArray())
    // 格式: XXXX-XXXX-XXXX-XXXXX-XXXXX-XXX (12+13=25 chars)
    return listOf(
        encId.substring(0,4), encId.substring(4,8), encId.substring(8,12),
        encPwd.substring(0,5), encPwd.substring(5,10), encPwd.substring(10,13)
    ).joinToString("-")
}

private fun decodeConnectionCode(code: String): Pair<String, String> {
    val clean = code.uppercase().replace(Regex("[^A-Z2-9]"), "")
    if (clean.length != 25) return Pair("", "")
    val encId = clean.substring(0, 12)
    val encPwd = clean.substring(12, 25)
    val idData = base32Decode(encId)
    val pwdData = base32Decode(encPwd)
    if (idData.size != 7 || pwdData.size != 8) return Pair("", "")
    if (idData[0] != 'Q'.code.toByte() || idData[1] != 'E'.code.toByte()) return Pair("", "")
    return Pair("QtET-OneClick-" + idData.decodeToString(), pwdData.decodeToString())
}

private fun randomSuffix(length: Int): String {
    val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { chars[Random.nextInt(chars.length)] }.joinToString("")
}
