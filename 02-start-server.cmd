@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist ".venv\Scripts\python.exe" (
  echo Run 01-setup.cmd first.
  pause
  exit /b 1
)
".venv\Scripts\python.exe" "%~dp0tools/team_env.py" start
if errorlevel 1 (
  echo.
  echo Server failed. Copy the error above and send it to B.
  pause
)
