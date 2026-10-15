@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Building Devil Bone Gambler...
call gradle build
if errorlevel 1 (
    echo.
    echo Build failed with errorlevel %errorlevel%
    pause
    exit /b 1
)
echo.
echo Build successful!
echo.
echo Output:
dir /b build\libs\DevilBoneGambler-v*.jar
echo.
echo Full path: %cd%\build\libs\
pause

