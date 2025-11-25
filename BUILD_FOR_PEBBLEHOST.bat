@echo off
echo ========================================
echo  Building Legendary Weapons SMP Plugin
echo  For PebbleHost Deployment
echo ========================================
echo.

echo Step 1: Stopping Gradle daemons...
call gradlew.bat --stop
echo.

echo Step 2: Building plugin...
call gradlew.bat build --no-daemon
echo.

if exist "build\libs\LegendaryWeaponsSMP-1.0.0.jar" (
    echo ========================================
    echo  BUILD SUCCESSFUL!
    echo ========================================
    echo.
    echo Plugin location:
    echo   %CD%\build\libs\LegendaryWeaponsSMP-1.0.0.jar
    echo.
    echo Next steps:
    echo   1. Go to your PebbleHost control panel
    echo   2. Open File Manager
    echo   3. Navigate to 'plugins' folder
    echo   4. Upload: build\libs\LegendaryWeaponsSMP-1.0.0.jar
    echo   5. Restart your server
    echo.
    echo See DEPLOYMENT.md for detailed instructions!
    echo.
) else (
    echo ========================================
    echo  BUILD FAILED!
    echo ========================================
    echo.
    echo Please check the error messages above.
    echo.
    echo Common fixes:
    echo   1. Close all IDEs and terminals
    echo   2. Delete the 'build' folder manually
    echo   3. Run this script again
    echo.
)

pause
