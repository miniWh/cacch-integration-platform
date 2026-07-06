#!/usr/bin/env python3
"""
cacch-integration-platform 自动化部署脚本
用法: python deploy.py [test|prod]
  test  → 测试环境 10.80.68.10 (默认)
  prod  → 正式环境 10.80.68.11
"""

import os
import sys
import time
import paramiko

# ==================== 配置 ====================
PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
LOCAL_JAR = os.path.join(PROJECT_DIR, "cacch-integration-web", "target", "cacch-integration-web.jar")
REMOTE_TMP = "/tmp/cacch-integration-web.jar"
REMOTE_APP_DIR = "/home/zhf/app"
REMOTE_JAR = f"{REMOTE_APP_DIR}/cacch-integration-web.jar"

SERVERS = {
    "test": {"host": "10.80.68.10", "user": "zhf", "pass": "zhf", "name": "测试环境"},
    "prod": {"host": "10.80.68.11", "user": "zhf", "pass": "zhf", "name": "正式环境"},
}

# ==================== 工具函数 ====================
def ssh_cmd(client, cmd, timeout=15):
    """执行远程命令，返回 (stdout, stderr)"""
    stdin, stdout, stderr = client.exec_command(cmd, timeout=timeout)
    out = stdout.read().decode("utf-8", errors="replace").strip()
    err = stderr.read().decode("utf-8", errors="replace").strip()
    return out, err

def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}")

def fail(msg):
    print(f"\n{'='*50}")
    print(f"  ❌ 部署失败: {msg}")
    print(f"{'='*50}\n")
    sys.exit(1)

# ==================== 主流程 ====================
def deploy(env="test"):
    server = SERVERS.get(env)
    if not server:
        fail(f"未知环境 '{env}'，请使用 test 或 prod")

    # 1. 检查本地 JAR
    log(f"📍 {server['name']} — {server['host']}")
    log(f"📦 检查本地 JAR ...")
    if not os.path.exists(LOCAL_JAR):
        fail(f"JAR 文件不存在: {LOCAL_JAR}\n   请先执行 mvn clean package -DskipTests")
    jar_size = os.path.getsize(LOCAL_JAR) / (1024 * 1024)
    log(f"   ✅ {os.path.basename(LOCAL_JAR)} ({jar_size:.1f} MB)")

    # 2. SSH 连接
    log(f"🔗 连接 {server['host']} ...")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    try:
        client.connect(server["host"], username=server["user"], password=server["pass"], timeout=10)
    except Exception as e:
        fail(f"SSH 连接失败: {e}")
    log("   ✅ 已连接")

    try:
        # 3. 上传 JAR
        log(f"📤 上传 JAR 到 {server['host']}:/tmp/ ...")
        sftp = client.open_sftp()
        sftp.put(LOCAL_JAR, REMOTE_TMP, callback=lambda done, total: None)
        sftp.close()
        uploaded_size_mb = int(client.exec_command(f"stat -c%s {REMOTE_TMP}")[1].read().decode().strip()) / (1024*1024) if True else 0
        log(f"   ✅ 上传完成")

        # 4. 停止旧进程
        log("🛑 停止旧进程 ...")
        out, _ = ssh_cmd(client, f"pgrep -f 'cacch-integration-web.jar'")
        old_pids = [p for p in out.split("\n") if p.strip()]
        if old_pids:
            for pid in old_pids:
                ssh_cmd(client, f"kill {pid}")
                log(f"   ⏳ 已发送终止信号, PID: {pid}")
            time.sleep(3)
            # 检查是否还在
            out2, _ = ssh_cmd(client, f"pgrep -f 'cacch-integration-web.jar'")
            still_alive = [p for p in out2.split("\n") if p.strip()]
            if still_alive:
                log("   ⚠️ 优雅终止超时，强制 kill -9 ...")
                for pid in still_alive:
                    ssh_cmd(client, f"kill -9 {pid}")
            log(f"   ✅ 已停止")
        else:
            log("   ℹ️ 没有运行中的进程")

        # 5. 备份旧 JAR → 部署新 JAR
        log("📋 部署新 JAR ...")
        ssh_cmd(client, f"test -f {REMOTE_JAR} && cp {REMOTE_JAR} {REMOTE_JAR}.bak.$(date +%Y%m%d_%H%M) 2>/dev/null; true")
        ssh_cmd(client, f"cp {REMOTE_TMP} {REMOTE_JAR}")
        log("   ✅ 已部署")

        # 6. 确保 startup.sh 中 JAVA_HOME 正确
        log("🔧 检查启动配置 ...")
        out, _ = ssh_cmd(client, f"head -15 {REMOTE_APP_DIR}/startup.sh")
        if "/usr/lib/jvm/java-21" not in out and "jdk-21" not in out:
            log("   ⚠️ JAVA_HOME 可能不正确，正在修复 ...")
            ssh_cmd(client, f"sed -i 's|JAVA_HOME=.*|JAVA_HOME=/usr/lib/jvm/java-21-openjdk|' {REMOTE_APP_DIR}/startup.sh")
        log("   ✅ 配置就绪")

        # 7. 清理 /tmp 中的临时 JAR
        ssh_cmd(client, f"rm -f {REMOTE_TMP}")

        # 8. 重启 — kill 进程让 systemd 自动拉起
        log("🔄 重启服务 ...")
        out, _ = ssh_cmd(client, f"pgrep -f 'cacch-integration-web.jar'")
        remaining = [p for p in out.split("\n") if p.strip()]
        if remaining:
            for pid in remaining:
                ssh_cmd(client, f"kill {pid}")
        time.sleep(5)

        # 9. 等待启动完成
        log("⏳ 等待服务启动 ...")
        max_wait = 30
        for i in range(max_wait):
            out, _ = ssh_cmd(client, "curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/actuator/health 2>/dev/null")
            if out == "200":
                elapsed = 5 + i
                log(f"   ✅ 服务已启动 (耗时约 {elapsed}s)")
                break
            time.sleep(1)
        else:
            fail("服务启动超时，请登录服务器检查日志: tail -50 /home/zhf/app/logs/app.log")

        # 10. 健康检查
        out, _ = ssh_cmd(client, "curl -s http://localhost:8081/actuator/health")
        if '"status":"UP"' not in out:
            fail(f"健康检查异常: {out}")

        log(f"🎉 {server['name']}部署成功!")
        log(f"🌐 http://{server['host']}:8081/actuator/health")
        print(f"\n{'='*50}")
        print(f"  ✅ 部署完成 — {server['name']}")
        print(f"  🌐 http://{server['host']}:8081/")
        print(f"{'='*50}\n")

    finally:
        client.close()

if __name__ == "__main__":
    env = sys.argv[1] if len(sys.argv) > 1 else "test"
    deploy(env)
