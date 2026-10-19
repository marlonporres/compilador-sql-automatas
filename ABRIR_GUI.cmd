@echo off
setlocal
chcp 65001 >nul
title SQL Studio - Compilador SQL
if not exist "%~dp0gui.ps1" (
    echo Falta gui.ps1. Abre ABRIR_GUI.cmd dentro de la carpeta completa del proyecto.
    pause
    exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0gui.ps1" -Modo gui
if errorlevel 1 (
    echo.
    echo No se pudo iniciar. Conserva los mensajes anteriores para el diagnostico.
    pause
    exit /b 2
)
