"""检查已部署服务的健康状态：API 健康接口、站点可达性、容器配置与数据目录。"""
import os

import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect(hostname=os.environ["SSH_HOST"], port=22, username=os.environ["SSH_USER"],
               password=os.environ["SSH_PASSWORD"], timeout=15, banner_timeout=20, auth_timeout=20)

checks = [
    ("api-health", "curl -s --max-time 8 http://127.0.0.1:8080/health; echo; curl -s -o /dev/null -w 'regions=%{http_code}\\n' --max-time 8 http://127.0.0.1:8080/api/regions"),
    ("web", "for p in 43123 43124 43125; do curl -s -o /dev/null -w \"port$p=%{http_code}\\n\" --max-time 6 http://127.0.0.1:$p/; done"),
    ("layouts", "ls -1 /opt/nongjiale 2>/dev/null; echo '--- compose ---'; ls -lh /opt/nongjiale/*.yml /opt/nongjiale/*.yaml 2>/dev/null; echo '--- jar time ---'; ls -lh /opt/nongjiale/api/ 2>/dev/null"),
    ("env", "docker inspect njl-api --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep -Ev 'PASSWORD|SECRET|TOKEN' | head -20"),
    ("mysql-db", "docker exec njl-mysql mysql -uroot -p\"$(docker inspect njl-mysql --format '{{range .Config.Env}}{{println .}}{{end}}' | grep MYSQL_ROOT_PASSWORD | cut -d= -f2)\" -N -e \"select table_schema, count(*) from information_schema.tables group by table_schema;\" 2>/dev/null | head -10"),
    ("logs", "docker logs njl-api --tail 6 2>&1 | tail -6"),
]

for label, cmd in checks:
    _, stdout, stderr = client.exec_command(cmd, timeout=40)
    out = stdout.read().decode("utf-8", "replace").strip()
    print(f"===== {label} =====")
    print(out if out else "(空)")

client.close()
