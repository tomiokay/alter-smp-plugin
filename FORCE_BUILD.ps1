# Force Build Script for Legendary Weapons SMP
# This script forcefully removes locks and builds the JAR

Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Force Building Legendary Weapons SMP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop all Gradle daemons
Write-Host "Step 1: Stopping Gradle daemons..." -ForegroundColor Yellow
& "$PSScriptRoot\gradlew.bat" --stop 2>$null
Start-Sleep -Seconds 2

# Step 2: Kill any remaining Java processes
Write-Host "Step 2: Killing Java processes..." -ForegroundColor Yellow
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2

# Step 3: Force delete build directory
Write-Host "Step 3: Removing build directory..." -ForegroundColor Yellow
$buildPath = Join-Path $PSScriptRoot "build"
if (Test-Path $buildPath) {
    Remove-Item -Path $buildPath -Recurse -Force -ErrorAction Stop
    Write-Host "  Build directory removed successfully!" -ForegroundColor Green
} else {
    Write-Host "  Build directory doesn't exist (OK)" -ForegroundColor Gray
}
Start-Sleep -Seconds 1

# Step 4: Build the plugin
Write-Host ""
Write-Host "Step 4: Building plugin..." -ForegroundColor Yellow
Write-Host "This may take 30-60 seconds..." -ForegroundColor Gray
Write-Host ""

& "$PSScriptRoot\gradlew.bat" build --no-daemon

# Check if build succeeded
$jarPath = Join-Path $PSScriptRoot "build\libs\LegendaryWeaponsSMP-1.0.0.jar"
if (Test-Path $jarPath) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host " BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Plugin JAR created at:" -ForegroundColor Cyan
    Write-Host "  $jarPath" -ForegroundColor White
    Write-Host ""
    Write-Host "File size: $((Get-Item $jarPath).Length / 1KB) KB" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Go to https://client.pebblehost.com" -ForegroundColor White
    Write-Host "  2. Open File Manager > plugins folder" -ForegroundColor White
    Write-Host "  3. Upload this JAR file" -ForegroundColor White
    Write-Host "  4. Restart your server" -ForegroundColor White
    Write-Host ""
    Write-Host "See PEBBLEHOST_QUICK_START.txt for details!" -ForegroundColor Cyan
} else {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host " BUILD FAILED!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please check the error messages above." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Common fixes:" -ForegroundColor Yellow
    Write-Host "  1. Close ALL terminals, IDEs, and file explorers" -ForegroundColor White
    Write-Host "  2. Run this script as Administrator" -ForegroundColor White
    Write-Host "  3. Restart your computer if problem persists" -ForegroundColor White
}

Write-Host ""
Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
