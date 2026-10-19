@echo off
setlocal
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0gui.ps1" -Modo empaquetar
if errorlevel 1 (
    pause
    exit /b 2
)
echo.
echo Abre output\SQLStudio\ABRIR.cmd. Copia toda la carpeta SQLStudio para trasladarla.
pause
