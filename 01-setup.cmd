@echo off
chcp 65001 >nul
cd /d "%~dp0"
python "%~dp0tools/team_env.py" setup
if errorlevel 1 (
  echo.
  echo Setup failed. Copy the error above and send it to B.
) else (
  echo.
  echo Setup complete. Next: double-click 02-start-server.cmd
)
pause
