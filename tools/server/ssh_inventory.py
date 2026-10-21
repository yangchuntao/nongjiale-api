"""采集服务器上已部署服务的现状：Java、数据库、Web 服务、Docker 容器、开放端口。"""
import os

import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(hostname=os.environ["SSH_HOST"], port=22, username=os.environ["SSH_USER"],
               password=os.environ["SSH_PASSWORD"], timeout=15, banner_timeout=20, auth_timeout=20)

checks = [
    ("docker", "docker --version 2>/dev/null; echo '--- containers ---'; docker ps -a --format '{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null"),
    ("java", "java -version 2>&1 | head -3; echo '--- jars ---'; ls -lh /opt 2>/dev/null; find /opt /root /home -maxdepth 3 -name '*.jar' 2>/dev/null | head -10"),
    ("nginx", "nginx -v 2>&1; systemctl is-active nginx 2>/dev/null"),
    ("mysql", "systemctl is-active mysql 2>/dev/null; mysql --version 2>/dev/null"),
    ("redis", "systemctl is-active redis-server 2>/dev/null; redis-cli --version 2>/dev/null"),
    ("listen", "ss -tlnp 2>/dev/null | head -20"),
    ("svc", "systemctl list-units --type=service --state=running --no-pager --no-legend 2>/dev/null | awk '{print $1}' | head -25"),
    ("port80", "curl -s -o /dev/null -w 'localhost80=%{http_code}\\n' --max-time 5 http://127.0.0.1/ 2>/dev/null"),
]

for label, cmd in checks:
    _, stdout, stderr = client.exec_command(cmd, timeout=25)
    out = stdout.read().decode("utf-8", "replace").strip()
    print(f"===== {label} =====")
    print(out if out else "(空)")

client.close()
