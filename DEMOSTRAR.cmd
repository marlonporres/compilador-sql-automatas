@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo.ps1" %*
if errorlevel 1 echo La demostracion no pudo completarse. Revisa el mensaje anterior.
pause
