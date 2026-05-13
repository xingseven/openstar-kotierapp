param(
    [string]$DeviceId = ""
)

$ErrorActionPreference = "Stop"

Write-Host "========================================"
Write-Host "  EasyTier Android 一键打包安装"
Write-Host "========================================"

$AndroidSdk = "$env:USERPROFILE\AppData\Local\Android\Sdk"
$Adb = "$AndroidSdk\platform-tools\adb.exe"
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# 1. 构建 Release APK
Write-Host ">>> [1/3] 开始构建 Release APK..."
Set-Location $ProjectDir
.\gradlew.bat assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 构建失败"
    exit 1
}
Write-Host ">>> [1/3] 构建完成 ✅"

# 2. 获取 APK 文件
$Apk = Get-ChildItem "$ProjectDir\app\build\outputs\apk\release\*-release.apk" | Select-Object -First 1
if (-not $Apk) {
    Write-Host "❌ 未找到 APK 文件"
    exit 1
}
Write-Host ">>> APK: $($Apk.Name)"

# 3. 检测设备并安装
if (-not $DeviceId) {
    Write-Host ">>> [2/3] 检测已连接的设备..."
    $Devices = @(
        & $Adb devices |
        Select-Object -Skip 1 |
        ForEach-Object {
            $line = $_.ToString().Trim()
            if ($line -match '^(?<serial>\S+)\s+device$') {
                $matches.serial
            }
        } |
        Where-Object { $_ }
    )
    $DeviceCount = $Devices.Count
    if ($DeviceCount -eq 0) {
        Write-Host "❌ 未检测到 Android 设备，请连接设备后重试"
        exit 1
    } elseif ($DeviceCount -gt 1) {
        Write-Host "⚠️  检测到多个设备:"
        $Devices | ForEach-Object { Write-Host "   - $_" }
        Write-Host "❌ 请使用 -DeviceId 指定目标设备"
        exit 1
    } else {
        $DeviceId = $Devices[0]
    }
}

if ($DeviceId -and -not (& $Adb devices | Select-String -Pattern "^$([regex]::Escape($DeviceId))\s+device$") ) {
    Write-Host "❌ 指定的设备 $DeviceId 不在线或未授权"
    exit 1
}

Write-Host ">>> [3/3] 正在安装到设备 $DeviceId ..."
& $Adb -s $DeviceId install -r $Apk.FullName
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ 安装失败"
    exit 1
}
Write-Host ">>> [3/3] 安装完成 ✅"
Write-Host "========================================"
Write-Host "  ✅ 打包安装成功！"
Write-Host "  版本: $($Apk.Name)"
Write-Host "  设备: $DeviceId"
Write-Host "========================================"
