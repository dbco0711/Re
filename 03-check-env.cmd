@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist ".venv\Scripts\python.exe" (
  echo Run 01-setup.cmd first.
  pause
  exit /b 1
)
".venv\Scripts\python.exe" "%~dp0tools/team_env.py" check
pause
