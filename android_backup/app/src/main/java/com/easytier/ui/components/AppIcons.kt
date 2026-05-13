package com.easytier.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object AppIcons {
    val Add = Icons.Rounded.AddCircle
    val ArrowBack = Icons.AutoMirrored.Rounded.ArrowBack
    val CheckCircle = Icons.Rounded.CheckCircle
    val ChevronRight = Icons.Rounded.ChevronRight
    val Cloud = Icons.Rounded.Cloud
    val CloudOff = Icons.Rounded.CloudOff
    val Computer = Icons.Rounded.Computer
    val Copy = Icons.Rounded.ContentCopy
    val Delete = Icons.Rounded.DeleteOutline
    val DeleteSweep = Icons.Rounded.DeleteSweep
    val Dns = Icons.Rounded.Dns
    val Edit = Icons.Rounded.Edit
    val ExpandMore = Icons.Rounded.ExpandMore
    val Flash = Icons.Rounded.FlashOn
    val GroupAdd = Icons.Rounded.GroupAdd
    val Lan = Icons.Rounded.Lan
    val Login = Icons.AutoMirrored.Rounded.Login
    val MonitorHeart = Icons.Rounded.MonitorHeart
    val Play = Icons.Rounded.PlayArrow
    val Save = Icons.Rounded.Save
    val Settings = Icons.Rounded.Settings
    val Share = Icons.Rounded.Share
    val Star = Icons.Rounded.Star
    val Stop = Icons.Rounded.Stop
    val Terminal = Icons.Rounded.Terminal
    val Tune = Icons.Rounded.Tune
    val VpnKey = Icons.Rounded.VpnKey
    val Wifi = Icons.Rounded.Wifi
    val WifiOff = Icons.Rounded.WifiOff
}

@Composable
fun AppIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}