#!/usr/bin/env bash
set -e

# Android 项目一键打包安装脚本
# Usage: ./build-and-install.sh [device_id]

ANDROID_SDK="$HOME/AppData/Local/Android/Sdk"
ADB="$ANDROID_SDK/platform-tools/adb"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================"
echo "  EasyTier Android 一键打包安装"
echo "========================================"

# 1. 构建 Release APK
echo ">>> [1/3] 开始构建 Release APK..."
cd "$PROJECT_DIR"
./gradlew assembleRelease --no-daemon
echo ">>> [1/3] 构建完成 ✅"

# 2. 获取 APK 路径
APK=$(find "$PROJECT_DIR/app/build/outputs/apk/release" -name "*-release.apk" | head -1)
if [ -z "$APK" ]; then
    echo "❌ 未找到 APK 文件"
    exit 1
fi
echo ">>> APK: $APK"

# 3. 检测设备并安装
DEVICE="${1:-}"
if [ -z "$DEVICE" ]; then
    echo ">>> [2/3] 检测已连接的设备..."
    DEVICE_LIST=$("$ADB" devices | grep -v "List" | grep "device$" | awk '{print $1}')
    DEVICE_COUNT=$(echo "$DEVICE_LIST" | grep -c . || true)
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo "❌ 未检测到 Android 设备，请连接设备后重试"
        exit 1
    elif [ "$DEVICE_COUNT" -gt 1 ]; then
        echo "⚠️  检测到多个设备:"
        echo "$DEVICE_LIST"
        echo "❌ 请传入设备号后重试"
        exit 1
    else
        DEVICE=$DEVICE_LIST
    fi
fi

echo ">>> [3/3] 正在安装到设备 $DEVICE ..."
"$ADB" -s "$DEVICE" install -r "$APK"
echo ">>> [3/3] 安装完成 ✅"
echo "========================================"
echo "  ✅ 打包安装成功！"
echo "  版本: $(basename "$APK")"
echo "  设备: $DEVICE"
echo "========================================"
