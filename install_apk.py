import subprocess
import sys
import os
import shutil

def find_adb():
    # 1. 系统 PATH 里直接找
    adb = shutil.which("adb")
    if adb:
        return adb

    # 2. 常见安装位置
    candidates = [
        os.path.join(os.environ.get("LOCALAPPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"),
        os.path.join(os.environ.get("APPDATA", ""), "Android", "Sdk", "platform-tools", "adb.exe"),
        r"C:\Android\Sdk\platform-tools\adb.exe",
        r"C:\Program Files\Android\Sdk\platform-tools\adb.exe",
        r"C:\Program Files (x86)\Android\Sdk\platform-tools\adb.exe",
        os.path.expanduser("~/Android/Sdk/platform-tools/adb"),       # Linux/Mac
        os.path.expanduser("~/Library/Android/sdk/platform-tools/adb"), # Mac
        "/usr/local/bin/adb",
        "/usr/bin/adb",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return path

    return None

adb_path = find_adb()

if len(sys.argv) > 1:
    apk = sys.argv[1].strip('"')
else:
    apk = input("APK path: ").strip().strip('"')

if not apk:
    input("Error: no APK path. Press Enter to exit.")
    sys.exit(1)

if not adb_path:
    input("Error: ADB not found. Please install Android SDK platform-tools or add adb to PATH. Press Enter to exit.")
    sys.exit(1)

print(f"ADB: {adb_path}")
print(f"APK: {apk}")
print()

print("Checking device...")
r = subprocess.run([adb_path, "devices"], capture_output=True, text=True)
connected = [l for l in r.stdout.splitlines() if l.strip() and "List of" not in l]
if not connected:
    input("Error: No Android device connected. Press Enter to exit.")
    sys.exit(1)

print("Device connected. Installing...")
r = subprocess.run([adb_path, "install", "-r", apk])
print()
if r.returncode != 0:
    input("FAILED. Press Enter to exit.")
else:
    input("SUCCESS - APK installed! Press Enter to exit.")
