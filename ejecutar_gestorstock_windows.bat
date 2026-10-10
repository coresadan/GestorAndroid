@echo off
title GestorStock - Control de Inventario y Ventas (Windows)
echo ========================================================
echo       Iniciando GestorStock para Windows...
echo ========================================================
cd /d "%~dp0"
if exist "%~dp0desktop\build\compose\binaries\main\app\GestorStock\GestorStock.exe" (
    start "" "%~dp0desktop\build\compose\binaries\main\app\GestorStock\GestorStock.exe"
) else (
    echo El ejecutable no fue encontrado en la ruta esperada.
    echo Compilando aplicacion de escritorio...
    call .\gradlew.bat :desktop:createDistributable
    if exist "%~dp0desktop\build\compose\binaries\main\app\GestorStock\GestorStock.exe" (
        start "" "%~dp0desktop\build\compose\binaries\main\app\GestorStock\GestorStock.exe"
    ) else (
        echo Error al generar el ejecutable.
        pause
    )
)
