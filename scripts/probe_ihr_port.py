# -*- coding: utf-8 -*-
"""
本地探测 IHR 网关 10.80.87.11 的真实 HTTPS 端口

背景：用户报 404 Not Found，怀疑是端口不对（ESB 用 776，IP 直连走默认 443）。
本脚本在本地通过 TCP/SSL 握手 + HTTP GET 探测一系列候选端口，输出每个端口的：
  - 是否能完成 TCP 三次握手
  - SSL 握手结果（返回的证书 subject / issuer / notAfter）
  - GET /openapi/oauth/token 的 HTTP 状态码与响应体前 200 字符

安全约定：脚本内不存储任何凭证，仅做端口探测；IHR_APP_KEY/SECRET 若提供则用于
Basic Auth 测试 token 端点。

用法：
  python scripts/probe_ihr_port.py            # 只做 TCP/SSL/HTTP 探测（无凭证）
  python scripts/probe_ihr_port.py --token   # 用环境变量 IHR_APP_KEY/IHR_APP_SECRET 测 token
"""
import argparse
import os
import socket
import ssl
import sys
import urllib.request
import urllib.error
from datetime import datetime, timezone

HOST = "10.80.87.11"

# 候选端口（按经验排序：IHR/ESB 776、内网常见 HTTPS 443、自建 8080/8443、内网常见 9001/9090）
CANDIDATE_PORTS = [443, 776, 80, 8080, 8443, 9001, 9090, 10443]


def probe_tcp(host: str, port: int, timeout: float = 3.0) -> tuple[bool, str]:
    """TCP 三次握手探测"""
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True, "TCP OK"
    except (socket.timeout, ConnectionRefusedError, OSError) as e:
        return False, f"{type(e).__name__}: {e}"


def probe_ssl(host: str, port: int, timeout: float = 5.0) -> tuple[bool, str]:
    """SSL 握手 + 返回服务端证书的 subject / issuer / 有效期"""
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    try:
        with socket.create_connection((host, port), timeout=timeout) as raw:
            with ctx.wrap_socket(raw, server_hostname=host) as sock:
                der = sock.getpeercert(binary_form=True)
                if not der:
                    return True, "(no peer cert returned)"
                # 解析 X.509 DER（用 cryptography 库兜底，缺失则只给指纹 + 长度）
                info = _parse_cert(der)
                return True, info
    except (socket.timeout, ssl.SSLError, OSError) as e:
        return False, f"{type(e).__name__}: {e}"


def _parse_cert(der: bytes) -> str:
    """解析 X.509 证书：优先用 cryptography 库，没有则只给 fingerprint + 长度"""
    import hashlib
    sha256 = hashlib.sha256(der).hexdigest()
    base = f"sha256={sha256[:32]}... len={len(der)}B"
    try:
        from cryptography import x509
        from cryptography.hazmat.backends import default_backend
        cert = x509.load_der_x509_certificate(der, default_backend())
        subj_cn = ""
        try:
            attrs = cert.subject.get_attributes_for_oid(x509.NameOID.COMMON_NAME)
            subj_cn = attrs[0].value if attrs else ""
        except Exception:
            pass
        issuer_cn = ""
        try:
            attrs = cert.issuer.get_attributes_for_oid(x509.NameOID.COMMON_NAME)
            issuer_cn = attrs[0].value if attrs else ""
        except Exception:
            pass
        nb = cert.not_valid_before_utc.isoformat() if hasattr(cert, "not_valid_before_utc") else str(cert.not_valid_before)
        na = cert.not_valid_after_utc.isoformat() if hasattr(cert, "not_valid_after_utc") else str(cert.not_valid_after)
        expired = " [EXPIRED]" if cert.not_valid_after_utc < datetime.now(timezone.utc) else ""
        return f"subject={subj_cn} issuer={issuer_cn} notBefore={nb} notAfter={na}{expired} | {base}"
    except ImportError:
        return f"(cryptography 库未安装，仅指纹) {base}"
    except Exception as e:
        return f"(解析失败: {type(e).__name__}: {e}) {base}"


def probe_http(host: str, port: int, path: str = "/openapi/oauth/token", timeout: float = 5.0) -> tuple[int | None, str]:
    """HTTP GET 探测：返回状态码 + 响应体前 200 字符"""
    last_err = ""
    for scheme in ("https", "http"):
        url = f"{scheme}://{host}:{port}{path}"
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        try:
            req = urllib.request.Request(url, method="GET")
            with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
                body = resp.read(200).decode("utf-8", "replace")
                return resp.status, f"[{scheme}] {body}"
        except urllib.error.HTTPError as e:
            body = ""
            try:
                body = e.read(200).decode("utf-8", "replace")
            except Exception:
                pass
            return e.code, f"[{scheme}] {body or '(empty body)'}"
        except (urllib.error.URLError, socket.timeout, ssl.SSLError, OSError) as e:
            last_err = f"{type(e).__name__}: {e}"
            continue
    return None, f"both http/https failed: {last_err}"


def probe_http_token(host: str, port: int, app_key: str, app_secret: str,
                     timeout: float = 5.0) -> tuple[int | None, str]:
    """HTTP POST 测 token 端点（带 Basic Auth）"""
    import base64
    auth = "Basic " + base64.b64encode(f"{app_key}:{app_secret}".encode()).decode()
    path = "/openapi/oauth/token?grant_type=client_credentials&scope=client"
    last_err = ""
    for scheme in ("https", "http"):
        url = f"{scheme}://{host}:{port}{path}"
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        try:
            req = urllib.request.Request(url, method="POST", data=b"",
                                          headers={"Authorization": auth, "Content-Type": "application/x-www-form-urlencoded"})
            with urllib.request.urlopen(req, timeout=timeout, context=ctx) as resp:
                body = resp.read(300).decode("utf-8", "replace")
                return resp.status, f"[{scheme}] {body}"
        except urllib.error.HTTPError as e:
            body = ""
            try:
                body = e.read(300).decode("utf-8", "replace")
            except Exception:
                pass
            return e.code, f"[{scheme}] {body or '(empty body)'}"
        except (urllib.error.URLError, socket.timeout, ssl.SSLError, OSError) as e:
            last_err = f"{type(e).__name__}: {e}"
            continue
    return None, f"both schemes failed: {last_err}"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--token", action="store_true",
                    help="用 IHR_APP_KEY/IHR_APP_SECRET 真实测 token 端点（默认仅 GET）")
    ap.add_argument("--host", default=HOST)
    args = ap.parse_args()

    app_key = os.environ.get("IHR_APP_KEY", "")
    app_secret = os.environ.get("IHR_APP_SECRET", "")
    do_token = args.token or bool(app_key and app_secret)

    if do_token and (not app_key or not app_secret):
        print("[ERROR] --token 需要 IHR_APP_KEY / IHR_APP_SECRET 环境变量")
        return 2

    print(f"探测目标: {args.host}")
    print(f"候选端口: {CANDIDATE_PORTS}")
    print(f"模式: {'POST /token with Basic' if do_token else 'GET /openapi/oauth/token'}")
    print()

    results = []
    for port in CANDIDATE_PORTS:
        tcp_ok, tcp_msg = probe_tcp(args.host, port)
        line = f"  [TCP]  {port:5d}  {'✓' if tcp_ok else '✗'}  {tcp_msg}"
        print(line)
        if not tcp_ok:
            results.append((port, "tcp_closed", None, ""))
            continue
        ssl_ok, ssl_msg = probe_ssl(args.host, port)
        print(f"  [SSL]  {port:5d}  {'✓' if ssl_ok else '✗'}  {ssl_msg}")
        if do_token:
            code, body = probe_http_token(args.host, port, app_key, app_secret)
        else:
            code, body = probe_http(args.host, port)
        print(f"  [HTTP] {port:5d}  status={code}  body={body[:200]}")
        results.append((port, "tcp_ok", code, body[:200]))
        print()

    print("=" * 60)
    print("汇总")
    print("=" * 60)
    for port, state, code, body in results:
        marker = " ← 可用" if code and code < 500 else ""
        print(f"  port={port:5d}  state={state:10s}  http_status={code}{marker}")

    return 0


if __name__ == "__main__":
    sys.exit(main())