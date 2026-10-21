# 部署手册 · 百度云轻量应用服务器

> 适用版本：`nongjiale-api 0.0.1-SNAPSHOT`（Spring Boot 3.3.13 / Java 17）
> 目标环境：百度云轻量应用服务器，Ubuntu 22.04 LTS，单机部署
> 配套文件：`deploy/` 下的 `api.env.example`、`nongjiale-api.service`、`nginx-nongjiale.conf`、`deploy.sh`、`backup-mysql.sh`
> 最后更新：2026-10-20

本文从一台空服务器写到可对外访问，包含备份、回滚和常见坑。命令默认用 `root` 执行，应用进程用 `njl` 用户运行。

---

## 0. 部署拓扑

单机四件套，数据库和缓存都不对公网开放：

```
             公网 80 / 443
                   │
              ┌────▼────┐
              │  Nginx  │  静态前端 + /api 反向代理
              └────┬────┘
                   │ 127.0.0.1:8080
              ┌────▼─────────┐
              │ nongjiale-api│  systemd 托管，单实例
              └──┬────────┬──┘
     127.0.0.1:3306   127.0.0.1:6379
         ┌───▼───┐    ┌───▼───┐
         │MySQL 8│    │ Redis │  只做缓存
         └───────┘    └───────┘
```

三条硬约束，先记住：

1. **只能单实例**。登录令牌存在进程内存（`InMemoryTokenStore`），重启即失效，也没法多实例共享。要横向扩容必须先改令牌存储，不在本期范围。
2. **数据库结构只走 Flyway**。不要手工建表、不要开 MyBatis-Plus 自动建表、不要改已经执行过的迁移脚本。
3. **图片只收 URL**。系统没有上传功能，Banner 和门面图需要你自己有对象存储或图床。

---

## 1. 前置准备

### 1.1 规格与系统

| 项 | 建议 | 说明 |
|---|---|---|
| CPU / 内存 | **2 核 2G 起** | 1C1G 不建议：JVM + MySQL 会互相挤爆，MySQL 启动峰值就可能 OOM |
| 系统盘 | 40G 起 | jar 包约 50M，日志和备份会持续增长 |
| 系统 | Ubuntu 22.04 LTS 64 位 | 自带 OpenJDK 17，本文命令按此写；CentOS 差异见 §12 |
| 带宽 | 按访问量选 | 轻量套餐带宽固定，首次上传 jar 建议 `scp -C` 压缩传输 |

在控制台创建实例时选「系统镜像 → Ubuntu 22.04」，不要选带宝塔的应用镜像：宝塔会占用 8888 等端口并自带一套 Nginx/PHP 配置，和本文的 systemd + Nginx 方案容易打架。如果你已经在用宝塔，见 §12.3。

### 1.2 需要提前准备的信息

| 信息 | 用途 | 哪里拿 |
|---|---|---|
| 公网 IP | SSH、验收 | 控制台实例列表 |
| root 密码 | 首次登录 | 控制台「重置密码」 |
| 域名（可选） | HTTPS、小程序 | 域名服务商 |
| 前端构建产物 | 门户 / 运营 / 商家三个 dist | 对应前端仓库 `npm run build` |
| 微信小程序 AppID / Secret | 小程序登录 | 微信公众平台 → 开发管理 |

> **备案提醒**：域名解析到中国大陆服务器，必须完成 ICP 备案，否则 80/443 会被拦截。**直接用 IP 访问不需要备案**，所以第一次验收可以先走 `http://公网IP/`，域名备案下来后再切。

### 1.3 先做一次快照

控制台 → 轻量应用服务器 → 实例 → **快照** → 创建快照。装完环境再打一次。轻量服务器的快照是最省事的回滚手段，比事后修配置快得多。

---

## 2. 控制台防火墙与端口

### 2.1 放行端口

控制台 → 实例 → **防火墙**，只放行这些：

| 端口 | 协议 | 来源 | 用途 |
|---|---|---|---|
| 22 | TCP | 你的办公 IP（能固定就固定） | SSH |
| 80 | TCP | 0.0.0.0/0 | HTTP |
| 443 | TCP | 0.0.0.0/0 | HTTPS |

**不要放行** 3306（MySQL）、6379（Redis）、8080（应用）。这三个只监听 `127.0.0.1`，公网不需要也不应该能连。

如果前端暂时沿用测服的 43123 / 43124 / 43125 端口，需要额外放行这三个 TCP 端口。正式环境建议改走域名，见 §9.2。

### 2.2 系统内防火墙

Ubuntu 22.04 镜像默认 `ufw` 是 inactive，保持关闭即可，避免和云防火墙双重拦截：

```bash
ufw status          # 期望 inactive
systemctl status ufw --no-pager
```

如果你习惯开 `ufw`，那两边规则必须一致，且先放行 22 再 enable，否则会把自己关在门外。

---

## 3. 系统初始化

```bash
# 时区：必须 Asia/Shanghai，否则意向日期会整体偏 8 小时
timedatectl set-timezone Asia/Shanghai
timedatectl

apt update && apt -y upgrade

# 基础工具
apt install -y curl vim unzip git ca-certificates gnupg lsb-release
```

### 3.1 加 swap（2G 内存必做）

MySQL 首次启动和 Maven 构建都会短时吃内存，2G 机器没有 swap 很容易被 OOM Killer 杀掉：

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
echo 'vm.swappiness=10' > /etc/sysctl.d/99-swap.conf
sysctl --system
free -h
```

### 3.2 目录与运行用户

```bash
# 应用进程用独立用户，不用 root 跑 Java
useradd --system --create-home --shell /usr/sbin/nologin njl

mkdir -p /opt/nongjiale/{api,web,backup}
mkdir -p /opt/nongjiale/web/{portal,merchant,admin}
chown -R njl:njl /opt/nongjiale/api /opt/nongjiale/backup
chmod 755 /opt/nongjiale /opt/nongjiale/web

# 环境变量目录，只有 root 可读
mkdir -p /etc/nongjiale
chmod 700 /etc/nongjiale
```

最终目录：

```
/opt/nongjiale/
├── api/app.jar              应用（njl 用户可写）
├── api/app.jar.bak.*        上一版，回滚用
├── web/portal/              门户 dist
├── web/merchant/            商家后台 dist
├── web/admin/               运营后台 dist
└── backup/                  MySQL 备份
/etc/nongjiale/api.env       环境变量，chmod 600
```

---

## 4. 安装 JDK 17

```bash
apt install -y openjdk-17-jdk-headless
java -version      # 期望 openjdk version "17.0.x"
```

不要装 JDK 8：Spring Boot 3.x 启动直接报 `UnsupportedClassVersionError`。装 JDK 21 也能跑（编译目标是 17），但没必要引入版本差异。

---

## 5. 安装与配置 MySQL 8

### 5.1 安装

```bash
apt install -y mysql-server
mysql --version    # 期望 8.0.x
systemctl enable --now mysql
```

### 5.2 安全初始化

```bash
mysql_secure_installation
```

按提示：设 root 密码 → 删匿名用户 → **禁止 root 远程登录** → 删 test 库 → 重载权限。

### 5.3 建库建用户

```bash
mysql -uroot -p
```

```sql
-- 库必须是 utf8mb4，Flyway 建表会继承库的字符集
CREATE DATABASE njl CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- 只允许本机连接；密码换成你自己的强口令
CREATE USER 'njl'@'127.0.0.1' IDENTIFIED BY 'ChangeMe_2026!';

-- Flyway 需要 DDL 权限，所以给库级 ALL，不要只给 DML
GRANT ALL PRIVILEGES ON njl.* TO 'njl'@'127.0.0.1';
FLUSH PRIVILEGES;

-- 校验时区
SELECT @@global.time_zone, @@session.time_zone, NOW();
```

### 5.4 参数调整

编辑 `/etc/mysql/mysql.conf.d/mysqld.cnf`，在 `[mysqld]` 段加：

```ini
bind-address            = 127.0.0.1
default-time-zone       = '+08:00'
character-set-server    = utf8mb4
collation-server        = utf8mb4_0900_ai_ci
max_connections         = 200
innodb_buffer_pool_size = 512M
```

2G 机器 `innodb_buffer_pool_size` 给 512M；4G 可以给 1G。改完重启：

```bash
systemctl restart mysql
systemctl status mysql --no-pager
```

### 5.5 验证应用账号能连

```bash
mysql -h127.0.0.1 -unjl -p njl -e "SELECT DATABASE(), NOW();"
```

> **表结构不用手工建**。应用启动时 Flyway 会依次执行 `V1`…`V7`，建表 + 写入苏中种子 + 演示账号。第一次启动日志里能看到 `Successfully applied 7 migrations`。

---

## 6. 安装与配置 Redis

```bash
apt install -y redis-server
```

编辑 `/etc/redis/redis.conf`：

```conf
bind 127.0.0.1 ::1
protected-mode yes
requirepass ChangeMe_Redis_2026!
maxmemory 128mb
maxmemory-policy allkeys-lru
```

```bash
systemctl enable --now redis-server
redis-cli -a 'ChangeMe_Redis_2026!' ping    # 期望 PONG
```

几个必须知道的点：

- **令牌不进 Redis**。Redis 只存首页、门店详情、地区、站点配置四类缓存，挂了只影响速度和数据库压力，登录仍可用。
- 一定要设 `requirepass` 且只能 `127.0.0.1` 监听，公网裸奔的 Redis 半天就会被挖矿脚本扫到。
- 应用侧没有在 `application.yml` 写死密码，靠环境变量 `SPRING_DATA_REDIS_PASSWORD` 注入。**漏了这行，缓存会一直连接失败但并不影响启动**，表现为日志里反复报 `NOAUTH`。

---

## 7. 构建与上传

### 7.1 构建（在本地或 CI，不在服务器上构建）

```bash
# 发版前建议先跑测试（含 JaCoCo 行覆盖率 85% 门禁）
mvn test

# 出包，跳过测试避免重复跑
mvn clean package -DskipTests
ls -lh target/nongjiale-api-0.0.1-SNAPSHOT.jar
```

服务器是 2G 内存，跑 Maven 测试容易 OOM，**不要在生产机构建**。测试在本地或 CI 跑完，只把 jar 传上去。

### 7.2 上传

```bash
# 压缩传输，轻量带宽小，省时间
scp -C target/nongjiale-api-0.0.1-SNAPSHOT.jar root@公网IP:/opt/nongjiale/api/app.jar
ssh root@公网IP 'chown njl:njl /opt/nongjiale/api/app.jar'
```

或者直接用 `deploy/deploy.sh`，它包含构建、上传、备份旧包、重启和健康检查，见 §11.1。

---

## 8. 环境变量与 systemd

### 8.1 写 `/etc/nongjiale/api.env`

把仓库里的 `deploy/api.env.example` 上传到 `/etc/nongjiale/api.env`，再逐项替换占位值：

```bash
# 本机执行
scp deploy/api.env.example root@公网IP:/etc/nongjiale/api.env

# 服务器执行
vim /etc/nongjiale/api.env
chmod 600 /etc/nongjiale/api.env
chown root:root /etc/nongjiale/api.env
```

关键几项：

| 变量 | 必填 | 说明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | **是** | 必须为 `mysql`。不设会用默认 `local`，起的是**内存 H2，重启数据全丢** |
| `MYSQL_HOST` / `MYSQL_PORT` / `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` | 是 | 与 §5.3 一致 |
| `REDIS_HOST` / `REDIS_PORT` | 是 | `127.0.0.1` / `6379` |
| `SPRING_DATA_REDIS_PASSWORD` | 是 | §6 设的口令，漏了会刷 `NOAUTH` |
| `APP_CORS_ALLOWED_ORIGINS` | 是 | 逗号分隔，**必须改成生产前端地址**，默认只放行 localhost |
| `OPENAPI_ENABLED` | 是 | 生产保持 `false`。需要临时看 Swagger 再改 `true`，看完关掉 |
| `WECHAT_MINIAPP_APP_ID` / `WECHAT_MINIAPP_SECRET` | 小程序必填 | 为空时微信相关接口返回 503，进程仍能启动 |

`APP_CORS_ALLOWED_ORIGINS` 由 `AppProperties` 松散绑定，逗号分隔即可，例如：

```bash
APP_CORS_ALLOWED_ORIGINS=https://www.example.com,https://admin.example.com,https://merchant.example.com
```

### 8.2 安装 systemd 单元

```bash
# 本机执行：上传单元文件
scp deploy/nongjiale-api.service root@公网IP:/etc/systemd/system/nongjiale-api.service

# 服务器执行
systemctl daemon-reload
systemctl enable --now nongjiale-api
systemctl status nongjiale-api --no-pager
```

文件内容见 `deploy/nongjiale-api.service`。要点：

- `EnvironmentFile=/etc/nongjiale/api.env`，改配置只改这个文件再 `restart`。
- `User=njl`，不用 root 跑 Java。
- `Restart=always` + `RestartSec=5`，进程崩了自动拉起。
- JVM 参数**写死在 ExecStart 里**，避免 `EnvironmentFile` 的引号和空格被 systemd 拆错。2G 机器用 `-Xmx512m`，4G 改 `-Xmx1g`。
- `SuccessExitStatus=143`，否则正常停止会被当成失败。
- `-Duser.timezone=Asia/Shanghai -Dfile.encoding=UTF-8`，防止日期和中文乱码。

### 8.3 首次启动验证

```bash
curl -s http://127.0.0.1:8080/health
# {"status":"UP","service":"nongjiale-api"}

# 确认 Flyway 跑完
journalctl -u nongjiale-api -n 200 --no-pager | grep -iE "flyway|migrat|Started NongjialeApiApplication"
```

期望看到 `Successfully applied 7 migrations`（空库）和 `Started NongjialeApiApplication`。如果日志里出现 `H2` 或 `jdbc:h2:mem`，说明 profile 没切，回去检查 `SPRING_PROFILES_ACTIVE`。

再验三个业务接口：

```bash
curl -s http://127.0.0.1:8080/api/config/public
curl -s http://127.0.0.1:8080/api/regions

curl -s -X POST http://127.0.0.1:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phone":"17606838920","password":"123456"}'
```

登录返回 `{"code":0,...,"data":{"token":"..."}}` 即通。

---

## 9. 安装与配置 Nginx

```bash
apt install -y nginx
nginx -v
```

### 9.1 API 反向代理

把仓库里的 `deploy/nginx-nongjiale.conf` 复制到 `/etc/nginx/sites-available/nongjiale.conf`，把 `server_name` 改成你的域名或公网 IP，然后：

```bash
ln -sf /etc/nginx/sites-available/nongjiale.conf /etc/nginx/sites-enabled/nongjiale.conf
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx
```

配置要点：

- `upstream` 到 `127.0.0.1:8080`，并开 `keepalive`。
- 透传 `Host`、`X-Real-IP`、`X-Forwarded-For`、`X-Forwarded-Proto`，否则日志里全是 127.0.0.1，排查登录来源会很痛苦。
- `client_max_body_size 4m`。当前没有上传接口，4M 足够；以后加图片上传再调。
- Swagger 三个 `location` 默认注释掉，生产不要暴露。

验证：

```bash
curl -s http://127.0.0.1/health
curl -s http://公网IP/health
```

### 9.2 前端三种放法

前端不在这三个仓库里，按你实际情况选一种。**选完之后 `APP_CORS_ALLOWED_ORIGINS` 必须与它对齐**。

**方案 A：同域子路径（推荐，无跨域）**

```
https://example.com/          → 门户
https://example.com/admin/    → 运营
https://example.com/merchant/ → 商家
https://example.com/api/      → 后端
```

Nginx 里加 `location /admin/ { alias /opt/nongjiale/web/admin/; try_files $uri $uri/ /admin/index.html; }`，其余同理。前端构建时 `base` 要设成 `/admin/`，API base 改成 `/api`。**这个方案要改前端，但上线后完全没有 CORS 问题，最省心。**

**方案 B：三个子域名**

```
www.example.com      门户
admin.example.com    运营
merchant.example.com 商家
api.example.com      后端
```

每个域名一个 `server` 块，各自 `root` 到 `/opt/nongjiale/web/xxx`。CORS 填三个子域名。需要三个域名都备案、都签证书，成本最高。

**方案 C：沿用测服端口（过渡，最省事）**

```
http://公网IP:43123  门户
http://公网IP:43124  运营
http://公网IP:43125  商家
http://公网IP/       API（Nginx 80 反代）
```

`deploy/nginx-nongjiale.conf` 里已经写好 43123/43124/43125 三个 `server` 块。这个方案不用改前端，但要求防火墙额外放行 43123-43125，且没有 HTTPS，**只适合内测，不要长期对客**。

### 9.3 前端路由回退

三个前端都是 SPA，必须写 `try_files $uri $uri/ /index.html;`，否则用户刷新 `/admin/banners` 会 404。这是最常见的上线问题之一。

---

## 10. HTTPS、域名与小程序

### 10.1 域名与备案

1. 域名 A 记录指向轻量服务器公网 IP。
2. 大陆服务器必须先完成 ICP 备案，备案通过前 80/443 上的域名访问会被拦。
3. 备案期间可继续用 IP 验收，功能不受影响。

### 10.2 证书

百度云控制台可以申请免费 DV 证书，签发后下载 **Nginx 格式**（含 `.pem` 和 `.key`）：

```bash
mkdir -p /etc/nginx/ssl
# 上传后
chmod 600 /etc/nginx/ssl/*.key
```

在 `server` 块里加：

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    ssl_certificate     /etc/nginx/ssl/api.example.com.pem;
    ssl_certificate_key /etc/nginx/ssl/api.example.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # 其余 location 与 80 端口一致
}

# 80 全部跳 443
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}
```

```bash
nginx -t && systemctl reload nginx
curl -I https://api.example.com/health
```

### 10.3 小程序侧

- 微信公众平台 → 开发管理 → 开发设置 → **服务器域名**，把 `https://api.example.com` 加进 `request` 合法域名。必须是 **HTTPS + 已备案域名，不能是 IP**。
- 服务端把 `WECHAT_MINIAPP_APP_ID` / `WECHAT_MINIAPP_SECRET` 写进 `api.env`，`systemctl restart nongjiale-api`。
- 验证：`POST /api/auth/wechat/login` 传 `wx.login` 的 `code`，未配置时返回 503「微信登录未配置」，配置正确则返回游客令牌。
- **Secret 只放 `api.env`**，不要提交到仓库、不要贴到群里。泄露了去公众平台重置。

---

## 11. 上线检查、发布与回滚

### 11.1 发布脚本

本机（Windows Git Bash / macOS / Linux）执行：

```bash
export SERVER_IP=你的公网IP
export SSH_USER=root
./deploy/deploy.sh
```

脚本做四件事：`mvn clean package -DskipTests` → 上传 `app.jar.new` → 把旧包备份成 `app.jar.bak.时间戳` → 替换、`restart`、等 8 秒拉 `/health`。健康检查不过脚本会非零退出。

### 11.2 回滚

```bash
ssh root@公网IP
cd /opt/nongjiale/api
ls -lt app.jar.bak.* | head
cp app.jar.bak.20261020-113000 app.jar
chown njl:njl app.jar
systemctl restart nongjiale-api
curl -s http://127.0.0.1:8080/health
```

**如果这次发布带了新的 Flyway 迁移，回滚 jar 不会回滚表结构。** Flyway 没有自动降级，必须先把库恢复成发布前的备份，再回滚 jar，顺序不能反。

### 11.3 上线前 checklist

对客开放前逐项打勾：

- [ ] `SPRING_PROFILES_ACTIVE=mysql`，日志里没有 H2
- [ ] `APP_CORS_ALLOWED_ORIGINS` 已改成生产前端实际地址，没有 localhost
- [ ] `OPENAPI_ENABLED=false`，`/swagger-ui.html` 和 `/v3/api-docs` 外网不可访问
- [ ] 演示账号口令已全部更换（见 §11.4）
- [ ] `site_config` 的站名、主色、客服电话已改成真实值（运营后台「站点配置」）
- [ ] 首页 Banner 换成自己的图片 URL，`example.com` 的种子图已下线
- [ ] 测试门店（`test_data=true`）已清理或下架
- [ ] 域名 ICP 备案通过，证书有效，HTTP 已 301 到 HTTPS
- [ ] 防火墙只开 22 / 80 / 443，3306 / 6379 / 8080 外网不可达
- [ ] MySQL 备份 cron 已生效，且**实际恢复验证过一次**
- [ ] 微信 AppID / Secret 已配置，小程序 request 域名已加白
- [ ] 已创建服务器快照

### 11.4 更换演示账号口令

种子里的运营、商家、游客口令都是 `123456`，**上线前必须换**。系统没有「管理员自助改密」接口，直接改库：

```bash
apt install -y apache2-utils
htpasswd -bnBC 10 "" '新的强口令' | tr -d ':\n'
# 输出形如 $2y$10$xxxxxxxx...
```

> `htpasswd` 生成 `$2y$` 前缀，Spring Security 6 的 BCrypt 能直接校验。如果遇到兼容问题，把前缀 `$2y$` 换成 `$2a$` 即可（ASCII 口令下两者等价）。

```sql
UPDATE admin_user    SET password_hash='$2y$10$...' WHERE phone='17606838920';
UPDATE merchant_user SET password_hash='$2y$10$...' WHERE phone='17606838921';
UPDATE portal_user   SET password_hash='$2y$10$...' WHERE phone='17606838922';
```

注意 SQL 里的 `$` 在 bash 中会被展开，用 `mysql -uroot -p` 进交互式再粘，或者用单引号包整条语句。

---

## 12. 日常运维

### 12.1 备份

`deploy/backup-mysql.sh` 用 `mysqldump --single-transaction` 逻辑备份，gzip 压缩，保留 7 天。安装：

```bash
# 本机上传
scp deploy/backup-mysql.sh root@公网IP:/opt/nongjiale/backup-mysql.sh

# 服务器执行
chmod 700 /opt/nongjiale/backup-mysql.sh
/opt/nongjiale/backup-mysql.sh && ls -lh /opt/nongjiale/backup/

# 每天 03:17
echo '17 3 * * * /opt/nongjiale/backup-mysql.sh >> /var/log/njl-backup.log 2>&1' | crontab -
```

恢复演练（**一定要真跑一次**，否则等于没备份）：

```bash
# 先把应用停掉，避免写入
systemctl stop nongjiale-api
gunzip -c /opt/nongjiale/backup/njl-2026-10-20-0317.sql.gz | mysql -uroot -p njl
systemctl start nongjiale-api
curl -s http://127.0.0.1:8080/health
```

备份文件建议每周同步一份到对象存储或另一台机器，别和生产放同一块盘。

### 12.2 日志与内存

```bash
# 应用日志
journalctl -u nongjiale-api -f
journalctl -u nongjiale-api --since "1 hour ago" --no-pager

# 限制 journal 占用，防止把盘写满
# /etc/systemd/journald.conf
SystemMaxUse=1G
systemctl restart systemd-journald

# Nginx 日志由系统 logrotate 自动切，确认一下
cat /etc/logrotate.d/nginx

# 进程内存
ps -o pid,rss,cmd -C java
free -h
```

### 12.3 排障速查

| 现象 | 先查什么 | 常见原因 |
|---|---|---|
| 启动即退出 | `journalctl -u nongjiale-api -n 100` | profile 没切 mysql、数据库连不上、Flyway 校验失败 |
| `Migration checksum mismatch` | 对比 `flyway_schema_history` | 改过已执行的 V1-V7 脚本 |
| 502 Bad Gateway | `ss -lntp \| grep 8080` | 应用没起来 / 启动中 / 被 OOM 杀掉 |
| 前端报 CORS | 浏览器 Network 的 Origin | `APP_CORS_ALLOWED_ORIGINS` 没改，或少了协议/端口 |
| 登录接口一直 401 | 账号是否被停用 | 停用返回 403；令牌过期 401，重新登录 |
| 页面中文乱码 | `locale`、`-Dfile.encoding` | 缺 UTF-8 参数或库字符集不是 utf8mb4 |
| 意向日期差 8 小时 | `timedatectl`、MySQL 时区 | 系统时区或 `default-time-zone` 不对 |
| 缓存日志刷 `NOAUTH` | `SPRING_DATA_REDIS_PASSWORD` | 漏配 Redis 口令 |
| 磁盘满 | `df -h`、`du -sh /var/log` | journal 或备份没限制 |

宝塔面板用户：宝塔自带 Nginx 会占用 80/443，把本文的 `server` 块加到宝塔站点的配置文件里即可，**不要再装一个 Nginx**；应用仍建议用 systemd 托管，不要用宝塔的 Java 项目管理器（它的环境变量注入方式和本文不一致）。

---

## 13. 常见坑（按踩坑概率排序）

1. **没切 profile**。`application.yml` 默认 `local`，不设 `SPRING_PROFILES_ACTIVE=mysql` 就是内存 H2。数据在重启后全部消失，而且不会报错，极具迷惑性。
2. **改了 Flyway 已执行的脚本**。V1-V7 的 checksum 已记录，改一个空格都会启动失败。要加字段就新建 `V8__xxx.sql`。
3. **种子三市是 OPEN，但计数不够**。扬州/泰州/南通种子里就是开通态，可是 V3 只有 3 家店、1 篇挂在**省**级（不计入任何市）的攻略，达不到 N=5 / M=1。**一旦你关城，就开不回来了**，除非先补够 5 家上架非测试门店和 1 篇已发布且归属该市的攻略。测试关城请用其他 CLOSED 市，或提前补数。
4. **CORS 忘了改**。默认白名单只有 `localhost:43123/43124/43125`，生产前端一调就报跨域。改完必须重启进程。
5. **多实例**。令牌在内存，两个实例会互相不认。负载均衡 + 多副本这个方案在当前代码下不可行。
6. **重启后全员掉线**。令牌不持久化，发布重启后运营和商家都要重新登录，属预期行为，提前通知。
7. **以为能传图**。Banner、门面图、攻略封面都只收 URL，没有上传接口。没有对象存储就去开一个，否则运营无法配图。
8. **2G 内存用默认 JVM 堆**。Spring Boot 默认按物理内存推算，容易把 MySQL 挤掉。固定 `-Xmx512m`。
9. **时区不对**。系统、JVM、MySQL 三处都必须是 `Asia/Shanghai`，缺一处就会导致意向入住日期整体偏一天。
10. **Redis 裸奔**。不设密码、监听 `0.0.0.0`，几小时内就会被扫描并写入挖矿任务。
11. **域名没备案就绑 80**。表现是被运营商拦截，不是 Nginx 配错，别在 Nginx 上浪费时间。
12. **Swagger 忘了关**。生产暴露 `/v3/api-docs` 等于把全部接口和字段结构公开，`OPENAPI_ENABLED` 保持 `false`。

---

## 14. 附：验收命令清单

```bash
# 1. 服务与端口
systemctl status nongjiale-api --no-pager
ss -lntp | grep -E '8080|3306|6379|80|443'

# 2. 健康检查
curl -s http://127.0.0.1:8080/health

# 3. 经 Nginx
curl -s http://公网IP/health

# 4. 公开接口
curl -s http://公网IP/api/config/public
curl -s http://公网IP/api/regions
curl -s 'http://公网IP/api/stores?page=1&pageSize=5'

# 5. 三端登录（口令换成生产口令）
curl -s -X POST http://公网IP/api/admin/auth/login \
  -H 'Content-Type: application/json' -d '{"phone":"17606838920","password":"***"}'
curl -s -X POST http://公网IP/api/merchant/auth/login \
  -H 'Content-Type: application/json' -d '{"phone":"17606838921","password":"***"}'
curl -s -X POST http://公网IP/api/auth/login \
  -H 'Content-Type: application/json' -d '{"phone":"17606838922","password":"***"}'

# 6. 数据库有表、有数据
mysql -h127.0.0.1 -unjl -p njl -e "SHOW TABLES; SELECT version, description, success FROM flyway_schema_history;"

# 7. 缓存生效
redis-cli -a '口令' --scan --pattern 'njl:*' | head
```

---

## 15. CentOS / 其他系统差异

如果你选的是 CentOS 7.9 或 AlmaLinux：

| 项 | Ubuntu 22.04 | CentOS 7.9 |
|---|---|---|
| 包管理 | `apt` | `yum` |
| JDK 17 | `apt install openjdk-17-jdk-headless` | `yum install java-17-openjdk-headless` |
| MySQL 8 | `apt install mysql-server` | 先加 MySQL 官方 yum 源，再 `yum install mysql-community-server` |
| MySQL 配置 | `/etc/mysql/mysql.conf.d/mysqld.cnf` | `/etc/my.cnf` |
| Redis | `apt install redis-server`，服务名 `redis-server` | `yum install redis`，服务名 `redis` |
| Nginx | `apt install nginx`，配置在 `sites-available` | `yum install nginx`，配置在 `/etc/nginx/conf.d/` |
| 防火墙 | `ufw` | `firewall-cmd --add-service=http --permanent` |
| 时区 | `timedatectl set-timezone` | 同上，或 `ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime` |

CentOS 7 已停止维护，新机器优先选 Ubuntu 22.04。
