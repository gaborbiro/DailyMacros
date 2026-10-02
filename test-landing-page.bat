@echo off
rem Runs the landing page (web/) and its Cloud Functions locally via the
rem Firebase Local Emulator Suite, so you can test it without deploying
rem anything to the real Firebase project. Nothing here touches production:
rem Firestore data lives only in memory for this run and is gone when you
rem stop it (Ctrl+C).
setlocal
cd /d "%~dp0"

echo Installing/verifying function dependencies...
pushd functions
call npm ci
set NPM_CI_ERR=%errorlevel%
popd
if not "%NPM_CI_ERR%"=="0" (
  echo.
  echo npm ci failed - see above.
  pause
  exit /b 1
)

echo.
echo Checking for leftover emulator processes from a previous run...
rem The Firestore emulator (a Java process) and the emulator hub (node) can
rem survive an unclean previous stop and keep holding these ports, which
rem would otherwise make every run after the first fail with "port taken".
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ports=4000,4400,4500,5000,5001,8080,9150; foreach($p in $ports){ Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | ForEach-Object { $proc = Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue; if($proc -and ($proc.ProcessName -eq 'node' -or $proc.ProcessName -eq 'java')){ Write-Host ('  Stopping leftover ' + $proc.ProcessName + ' (PID ' + $proc.Id + ') on port ' + $p); Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } } }"

echo.
echo Starting Firebase emulators: Hosting, Functions, Firestore...
echo   Site:        http://127.0.0.1:5000
echo   Emulator UI: http://127.0.0.1:4000
echo Press Ctrl+C here to stop everything.
echo (Running this script again later will stop this instance and start fresh - any local test data is lost either way, since it only ever lived in memory.)
echo.

start "" cmd /c "timeout /t 8 >nul & start http://127.0.0.1:5000"

call npx firebase-tools emulators:start --only hosting,functions,firestore --project dailymacros-9fab8

endlocal
