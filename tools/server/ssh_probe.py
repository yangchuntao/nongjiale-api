"""通过密码登录远程主机并采集基础信息。凭据从环境变量读取，不落盘。"""
import os
import sys

import paramiko

host = os.environ["SSH_HOST"]
user = os.environ["SSH_USER"]
password = os.environ["SSH_PASSWORD"]

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
try:
    client.connect(hostname=host, port=22, username=user, password=password,
                   timeout=15, banner_timeout=20, auth_timeout=20)
except paramiko.AuthenticationException:
    print("RESULT=AUTH_FAILED")
    sys.exit(2)
except Exception as exc:  # 网络或协议层失败
    print(f"RESULT=CONNECT_FAILED {type(exc).__name__}: {exc}")
    sys.exit(3)

print("RESULT=OK")

# 一次会话内跑完，输出用分隔符切开，避免多次连接
commands = [
    ("hostname", "hostname"),
    ("os", "cat /etc/os-release 2>/dev/null | grep -E '^(PRETTY_NAME|VERSION_ID)=' || uname -a"),
    ("kernel", "uname -r"),
    ("uptime", "uptime"),
    ("cpu", "nproc && grep -m1 'model name' /proc/cpuinfo 2>/dev/null | cut -d: -f2"),
    ("mem", "free -h | head -3"),
    ("disk", "df -h / | tail -1"),
    ("ip", "hostname -I 2>/dev/null"),
]
for label, cmd in commands:
    _, stdout, stderr = client.exec_command(cmd, timeout=20)
    out = stdout.read().decode("utf-8", "replace").strip()
    err = stderr.read().decode("utf-8", "replace").strip()
    print(f"--- {label} ---")
    print(out if out else (err or "(空)"))

client.close()
