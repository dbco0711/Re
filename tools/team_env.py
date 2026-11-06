"""Windows team environment helper; no credentials or real user data."""
from pathlib import Path
import importlib.metadata
import os
import socket
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
PYTHON = ROOT / ".venv" / "Scripts" / "python.exe"
VERSION = (3, 11, 9)


def run(args):
    subprocess.run([str(a) for a in args], cwd=ROOT, check=True)


def version():
    if sys.version_info[:3] != VERSION:
        raise RuntimeError("Required Python 3.11.9; found " + sys.version.split()[0] +
                           ". Do not remove existing environments. Ask B to check the Python path.")


def check():
    version()
    if Path(sys.prefix).resolve() != (ROOT / ".venv").resolve():
        raise RuntimeError("Use this project's .venv Python; run 01-setup.cmd first.")
    errors = []
    for line in (ROOT / "requirements.txt").read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, expected = line.split("==", 1)
        try:
            actual = importlib.metadata.version(name)
        except importlib.metadata.PackageNotFoundError:
            actual = "not installed"
        if actual != expected:
            errors.append(f"{name}: expected {expected}, found {actual}")
    if errors:
        raise RuntimeError("Run 01-setup.cmd. Package mismatch:\n" + "\n".join(errors))
    run([sys.executable, "-m", "pip", "check"])
    print("PASS: Python 3.11.9, project .venv, all pinned dependencies.", flush=True)


def setup():
    version()
    env = ROOT / ".venv"
    if env.exists():
        if not PYTHON.is_file():
            raise RuntimeError("Existing .venv is incomplete. Preserved; ask B to inspect it.")
        run([PYTHON, "-c", "import sys; assert sys.version_info[:3] == (3,11,9), 'Existing .venv requires inspection by B'"])
        print("Reusing existing .venv (not deleting or recreating).", flush=True)
    else:
        run([sys.executable, "-m", "venv", env])
    run([PYTHON, "-m", "pip", "install", "-r", ROOT / "requirements.txt"])
    run([PYTHON, __file__, "check"])


def start():
    check()
    with socket.socket() as probe:
        try:
            probe.bind(("127.0.0.1", 8000))
        except OSError as error:
            raise RuntimeError("Port 8000 is busy or blocked. Do not stop unknown processes. "
                               "If Re is already running, use its existing window; otherwise ask B.") from error
    print("Keep this window open. Stop with Ctrl+C.\n"
          "Health: http://127.0.0.1:8000/health\n"
          "Demo:   http://127.0.0.1:8000/v1/demo-card", flush=True)
    run([PYTHON, "-m", "uvicorn", "backend.main:app", "--host", "127.0.0.1", "--port", "8000"])


if __name__ == "__main__":
    os.chdir(ROOT)
    try:
        action = sys.argv[1] if len(sys.argv) == 2 else ""
        if action not in {"setup", "check", "start"}:
            raise RuntimeError("Usage: team_env.py setup|check|start")
        {"setup": setup, "check": check, "start": start}[action]()
    except KeyboardInterrupt:
        print("Stopped.")
    except (RuntimeError, OSError, subprocess.CalledProcessError) as error:
        print("ERROR:", error, file=sys.stderr)
        sys.exit(1)
