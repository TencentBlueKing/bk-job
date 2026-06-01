#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成作业平台服务间调用认证（JWT/RSA）所需的 PEM 密钥，并输出为 Helm values 所需的 Base64 形式。

与 src/backend/.../RSAUtils.java 约定一致：对整段 PEM 文本（含头尾与换行）再做一次 Base64，
对应 values.yaml 中的 job.security.privateKeyBase64 / job.security.publicKeyBase64
（Spring 配置路径为 job.security.service.private-key-base64 / public-key-base64）。

依赖：
- cryptography

用法：
  python3 generate_service_rsa_keys.py
  python3 generate_service_rsa_keys.py --bits 2048 --pretty
"""

from __future__ import annotations

import argparse
import base64
import json
import sys
from typing import Tuple

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa


def _generate_rsa_keypair(bits: int) -> Tuple[bytes, bytes]:
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=bits)
    public_key = private_key.public_key()
    private_pem = private_key.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption(),
    )
    public_pem = public_key.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo,
    )
    return private_pem, public_pem


def _pem_to_values_base64(pem: bytes) -> str:
    """与 Java 侧一致：对 PEM 原文做 Base64，部署时再解码得到 PEM。"""
    return base64.b64encode(pem).decode("ascii")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="生成 job.security.* Base64 PEM，输出单行 JSON 到标准输出。"
    )
    parser.add_argument(
        "--bits",
        type=int,
        default=2048,
        choices=(2048, 3072, 4096),
        help="RSA 模数位宽，默认 2048",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="将 JSON 格式化多行输出（默认单行便于管道处理）",
    )
    args = parser.parse_args()

    try:
        priv_pem, pub_pem = _generate_rsa_keypair(args.bits)
    except Exception as exc:
        print(f"生成 RSA 密钥失败: {exc}", file=sys.stderr)
        return 1

    payload = {
        "job.security.privateKeyBase64": _pem_to_values_base64(priv_pem),
        "job.security.publicKeyBase64": _pem_to_values_base64(pub_pem),
    }
    if args.pretty:
        print(json.dumps(payload, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
