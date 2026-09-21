#!/usr/bin/env python3
"""
部署前 JAR 完整性校验

背景:
    IDEA 与 Maven 共用 target/classes。IDEA 自动构建失败时会写出 stub class ——
    类签名与 @Component 注解都在，但方法体被替换为 `throw new Error(...)`。
    这类 class 若撞上 Maven 的 spring-boot repackage 时间窗就会被打进部署 JAR，
    服务启动时表现为「某 MapStruct Converter bean 找不到」的偶发启动失败。

做法:
    解包 JAR 中 BOOT-INF/classes 下的 class 文件，逐个用 javap -c 反汇编，
    出现 java/lang/Error 即判定为 IDEA stub，中止部署。

用法:
    python verify_jar_integrity.py [jar路径] [--impl-only] [--javap <javap.exe路径>]

退出码:
    0 = 通过 / 1 = 发现 stub class / 2 = 参数或环境问题
"""

import os
import subprocess
import sys
import tempfile
import zipfile
from concurrent.futures import ThreadPoolExecutor

PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_JAR = os.path.join(
    PROJECT_DIR, "cacch-integration-web", "target", "cacch-integration-web.jar"
)
FALLBACK_JAVAP = r"D:\Software\Java\jdk21.0.10_7\bin\javap.exe"
MAX_WORKERS = 8


def resolve_javap(cli_path):
    """确定 javap 路径:命令行 > JAVA_HOME > 兜底硬编码"""
    if cli_path:
        if not os.path.exists(cli_path):
            print(f"!! 指定的 javap 不存在: {cli_path}")
            sys.exit(2)
        return cli_path
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = os.path.join(java_home, "bin", "javap.exe")
        if os.path.exists(candidate):
            return candidate
    if os.path.exists(FALLBACK_JAVAP):
        return FALLBACK_JAVAP
    print("!! 找不到 javap.exe，请设置 JAVA_HOME 或用 --javap 指定")
    sys.exit(2)


def is_stub(javap, cls_file):
    """反汇编单个 class，返回 (是否为 stub, 备注)

    注意:javap 输出含中文常量时按平台编码(GBK)输出，不能用 UTF-8 解码，
    因此统一按字节匹配特征串 java/lang/Error。
    """
    try:
        result = subprocess.run(
            [javap, "-c", "-p", cls_file],
            capture_output=True, timeout=60
        )
    except subprocess.TimeoutExpired:
        return False, "javap 超时"
    output = (result.stdout or b"") + (result.stderr or b"")
    return (b"java/lang/Error" in output), None


def main():
    args = sys.argv[1:]
    jar_path = DEFAULT_JAR
    javap_path = None
    impl_only = False

    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--impl-only":
            impl_only = True
        elif arg == "--javap":
            i += 1
            if i >= len(args):
                print("!! --javap 缺少参数")
                sys.exit(2)
            javap_path = args[i]
        elif arg.startswith("-"):
            print(f"!! 未知参数: {arg}")
            sys.exit(2)
        else:
            jar_path = arg
        i += 1

    if not os.path.exists(jar_path):
        print(f"!! JAR 不存在: {jar_path}")
        print("   请先执行 mvn clean package -DskipTests")
        sys.exit(2)

    javap = resolve_javap(javap_path)
    jar_size_mb = os.path.getsize(jar_path) / (1024 * 1024)
    print(f"[校验] JAR: {jar_path} ({jar_size_mb:.1f} MB)")
    print(f"[校验] javap: {javap}")

    tmp_dir = tempfile.mkdtemp(prefix="jar-verify-")
    stubs, checked = [], 0

    with zipfile.ZipFile(jar_path) as zf:
        names = [n for n in zf.namelist()
                 if n.startswith("BOOT-INF/classes/") and n.endswith(".class")]
        if impl_only:
            names = [n for n in names if n.endswith("Impl.class")]

        if not names:
            print("!! JAR 中未找到任何 BOOT-INF/classes 下的 class，产物异常")
            sys.exit(1)

        files = []
        for idx, name in enumerate(names):
            data = zf.read(name)
            if not data:
                stubs.append(f"{name} (0 字节)")
                continue
            cls_file = os.path.join(tmp_dir, f"{idx}.class")
            with open(cls_file, "wb") as f:
                f.write(data)
            files.append((name, cls_file))

    with ThreadPoolExecutor(max_workers=MAX_WORKERS) as pool:
        results = list(pool.map(lambda item: is_stub(javap, item[1]), files))
        for (name, _), (stub, _) in zip(files, results):
            checked += 1
            if stub:
                stubs.append(name)

    print(f"[校验] 检查 class 数: {checked}")

    if stubs:
        print("")
        print(f"!! 发现 {len(stubs)} 个 IDEA 编译失败 stub class（方法体为 throw new Error）:")
        for s in stubs:
            print(f"   - {s}")
        print("")
        print("!! 产物被污染，已中止部署。处理方式:")
        print("     1) 关闭 IDEA 的 Build project automatically，或勾选")
        print("        Settings → Build Tools → Maven → Runner → Delegate IDE build/run actions to Maven")
        print("     2) 重新执行 mvn clean package -DskipTests 后再部署")
        return 1

    print("[校验] OK: 未发现 stub class，产物完整")
    return 0


if __name__ == "__main__":
    sys.exit(main())
