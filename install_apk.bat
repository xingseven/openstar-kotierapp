@echo off
chcp 65001 >nul
python3 "%~dp0install_apk.py" "%1"
pause
