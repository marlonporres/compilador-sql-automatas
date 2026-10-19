@echo off
setlocal
set "EDITOR_PROYECTO=%LOCALAPPDATA%\Programs\Microsoft VS Code\bin\code.cmd"
if exist "%EDITOR_PROYECTO%" (
    call "%EDITOR_PROYECTO%" --new-window "%~dp0SQLStudio.code-workspace"
    exit /b
)
set "EDITOR_PROYECTO=%ProgramFiles%\Microsoft VS Code\bin\code.cmd"
if exist "%EDITOR_PROYECTO%" (
    call "%EDITOR_PROYECTO%" --new-window "%~dp0SQLStudio.code-workspace"
    exit /b
)
where code.cmd >nul 2>&1
if errorlevel 1 (
    echo No se encontro Visual Studio Code.
    echo Puedes ejecutar la aplicacion con ABRIR_GUI.cmd.
    pause
    exit /b 2
)
call code.cmd --new-window "%~dp0SQLStudio.code-workspace"
