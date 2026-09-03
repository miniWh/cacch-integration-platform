# -*- coding: utf-8 -*-
"""
核对目标服务器 startup.sh 的环境变量注入现状

用途：确认 IHR_APP_KEY / IHR_APP_SECRET 等敏感变量是否已注入，
      且 export 位置是否在 nohup java 行之前（追加到文件末尾 java 子进程继承不到）。

安全约定：脚本内不存储任何凭证，主机/账号/密码一律由环境变量传入，便于纳入版本库。

用法（在 Git Bash 中执行）：
    export DEPLOY_PWD='你的密码'
    export DEPLOY_HOST=10.80.68.10      # 可选，默认测试环境
    export DEPLOY_USER=zhf              # 可选，默认 zhf
    python scripts/ssh_probe_startup.py
"""
import os
import sys

import paramiko

HOST = os.environ.get("DEPLOY_HOST", "10.80.68.10")
USER = os.environ.get("DEPLOY_USER", "zhf")
PWD = os.environ.get("DEPLOY_PWD", "")


def main() -> int:
    if not PWD:
        print("[ERROR] 未提供密码：请先执行 export DEPLOY_PWD='你的密码'")
        return 2

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    print(f"[INFO] 连接 {USER}@{HOST} ...")
    client.connect(HOST, username=USER, password=PWD, timeout=20)

    def run(cmd: str) -> str:
        _in, out, err = client.exec_command(cmd)
        text = out.read().decode("utf-8", "replace")
        error = err.read().decode("utf-8", "replace").strip()
        if error:
            return f"{text}\n[stderr] {error}"
        return text

    print("=" * 60)
    print("[1] app 目录")
    print("=" * 60)
    print(run("ls -la /home/zhf/app 2>/dev/null | head -30"))

    print("=" * 60)
    print("[2] startup.sh 全文（含行号）")
    print("=" * 60)
    print(run(
        'find /home/zhf -maxdepth 4 -name "startup*.sh" 2>/dev/null '
        '-exec echo "===== {} =====" \\; -exec cat -n {} \\;'
    ))

    print("=" * 60)
    print("[3] 运行中的 java 进程实际环境变量（IHR / MIDEA / OA / CRM）")
    print("=" * 60)
    print(run(
        'PID=$(pgrep -f "java.*cacch" | head -1); '
        'if [ -n "$PID" ]; then echo "java pid=$PID"; '
        'tr "\\0" "\\n" < /proc/$PID/environ 2>/dev/null | grep -E "^(IHR|MIDEA|OA|CRM)" '
        '|| echo "(该进程无 IHR/MIDEA/OA/CRM 变量)"; '
        'else echo "(未找到运行中的 cacch java 进程)"; fi'
    ))

    print("=" * 60)
    print("[4] systemd 服务定义（如使用 systemd 托管）")
    print("=" * 60)
    print(run(
        'systemctl list-units --type=service --all 2>/dev/null | grep -i cacch '
        '|| echo "(无 cacch systemd 服务)"; '
        'for f in /etc/systemd/system/cacch*.service; do '
        '[ -f "$f" ] && echo "===== $f =====" && cat "$f"; done'
    ))

    client.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
