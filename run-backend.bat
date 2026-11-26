@echo off
title Ashwin Sinha Portfolio - Java Backend Server
echo ===================================================
echo   Compiling and Starting Java Backend Server...
echo ===================================================

cd /d "%~dp0\backend"

if not exist "bin" mkdir bin

echo [1/2] Compiling Java source files...
"C:\Program Files\Java\jdk-25.0.4.1\bin\javac.exe" -d bin src\com\ashwin\backend\PortfolioServer.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Please check Java installation.
    pause
    exit /b %ERRORLEVEL%
)

echo [2/2] Starting PortfolioServer on http://localhost:8080 ...
start http://localhost:8080

"C:\Program Files\Java\jdk-25.0.4.1\bin\java.exe" -cp bin com.ashwin.backend.PortfolioServer
pause
