#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成 job-gateway 开启 HTTPS 所需的自签名证书材料，并输出为 Helm values 所需的 Base64 形式。

输出 values.yaml 中以下 4 个配置项的值：
- gatewayConfig.server.ssl.p12.base64Content：PKCS12 格式 keystore 的单行 Base64 编码内容，
  含服务端私钥、服务端证书与自签名 CA 证书，条目别名为 job_server.p12（与 server.ssl.key-alias 约定一致）
- gatewayConfig.server.ssl.keystore.password：上述 keystore 的口令
- gatewayConfig.server.ssl.truststore.base64Content：JKS 格式 truststore 的单行 Base64 编码内容，
  含上述自签名 CA 证书，条目别名为 ca
- gatewayConfig.server.ssl.truststore.password：上述 truststore 的口令

依赖：
- cryptography

用法：
  python3 generate_gateway_tls_cert.py
  python3 generate_gateway_tls_cert.py --san bk-job-gateway --san 127.0.0.1 --days 3650 --pretty
"""

from __future__ import annotations

import argparse
import base64
import datetime
import hashlib
import ipaddress
import json
import secrets
import string
import struct
import sys
import time
from typing import List, Tuple

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

# keystore 中私钥条目的别名，需与 values.yaml 中 server.ssl.key-alias 保持一致
KEY_ALIAS = "job_server.p12"
# truststore 中 CA 证书条目的别名
TRUSTSTORE_ALIAS = "ca"
# 默认签发的服务端证书主体名与 SAN，按需通过 --san 指定实际访问的域名或 IP
SERVER_CN = "bk-job-gateway"
CA_CN = "JOBROOTCA"
DEFAULT_SANS = (SERVER_CN, "localhost")
# 口令仅使用字母与数字，避免在 YAML、Shell 与 JVM 参数中出现转义问题
PASSWORD_ALPHABET = string.ascii_letters + string.digits
PASSWORD_LENGTH = 24


def _generate_password() -> str:
    return "".join(secrets.choice(PASSWORD_ALPHABET) for _ in range(PASSWORD_LENGTH))


def _build_name(common_name: str) -> x509.Name:
    return x509.Name(
        [
            x509.NameAttribute(NameOID.COUNTRY_NAME, "CN"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "Tencent"),
            x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "BlueKing"),
            x509.NameAttribute(NameOID.COMMON_NAME, common_name),
        ]
    )


def _build_san(names: List[str]) -> x509.SubjectAlternativeName:
    """SAN 中的条目按内容自动识别为 IP 或域名。"""
    entries: List[x509.GeneralName] = []
    for name in names:
        try:
            entries.append(x509.IPAddress(ipaddress.ip_address(name)))
        except ValueError:
            entries.append(x509.DNSName(name))
    return x509.SubjectAlternativeName(entries)


def _generate_ca(bits: int, days: int) -> Tuple[rsa.RSAPrivateKey, x509.Certificate]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=bits)
    subject = _build_name(CA_CN)
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (
        x509.CertificateBuilder()
        .subject_name(subject)
        .issuer_name(subject)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now)
        .not_valid_after(now + datetime.timedelta(days=days))
        .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
        .add_extension(
            x509.KeyUsage(
                digital_signature=True,
                content_commitment=False,
                key_encipherment=False,
                data_encipherment=False,
                key_agreement=False,
                key_cert_sign=True,
                crl_sign=True,
                encipher_only=False,
                decipher_only=False,
            ),
            critical=True,
        )
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
        .sign(key, hashes.SHA256())
    )
    return key, cert


def _generate_server_cert(
    ca_key: rsa.RSAPrivateKey,
    ca_cert: x509.Certificate,
    bits: int,
    days: int,
    sans: List[str],
) -> Tuple[rsa.RSAPrivateKey, x509.Certificate]:
    key = rsa.generate_private_key(public_exponent=65537, key_size=bits)
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (
        x509.CertificateBuilder()
        .subject_name(_build_name(SERVER_CN))
        .issuer_name(ca_cert.subject)
        .public_key(key.public_key())
        .serial_number(x509.random_serial_number())
        .not_valid_before(now)
        .not_valid_after(now + datetime.timedelta(days=days))
        .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
        .add_extension(
            x509.KeyUsage(
                digital_signature=True,
                content_commitment=False,
                key_encipherment=True,
                data_encipherment=False,
                key_agreement=False,
                key_cert_sign=False,
                crl_sign=False,
                encipher_only=False,
                decipher_only=False,
            ),
            critical=True,
        )
        .add_extension(
            x509.ExtendedKeyUsage([x509.oid.ExtendedKeyUsageOID.SERVER_AUTH,
                                   x509.oid.ExtendedKeyUsageOID.CLIENT_AUTH]),
            critical=False,
        )
        .add_extension(_build_san(sans), critical=False)
        .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
        .add_extension(
            x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_cert.public_key()),
            critical=False,
        )
        .sign(ca_key, hashes.SHA256())
    )
    return key, cert


def _build_pkcs12_keystore(
    server_key: rsa.RSAPrivateKey,
    server_cert: x509.Certificate,
    ca_cert: x509.Certificate,
    password: str,
) -> bytes:
    return pkcs12.serialize_key_and_certificates(
        name=KEY_ALIAS.encode("utf-8"),
        key=server_key,
        cert=server_cert,
        cas=[ca_cert],
        encryption_algorithm=serialization.BestAvailableEncryption(password.encode("utf-8")),
    )


def _write_java_utf(text: str) -> bytes:
    """等价于 Java DataOutputStream#writeUTF：2 字节长度 + UTF-8 内容。"""
    raw = text.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def _build_jks_truststore(ca_cert: x509.Certificate, password: str) -> bytes:
    """
    写出仅含一条 trustedCertEntry 的 JKS truststore。

    JKS 无公开的格式规范文档，此处字节布局与 JDK 的 sun.security.provider.JavaKeyStore
    读写实现保持一致，以保证 keytool 与 JVM 能正常识别：
      magic(0xFEEDFEED) | version(2) | 条目数 | [条目...] | SHA-1 校验码
    其中 trustedCertEntry 的条目结构为：
      tag(2) | 别名 | 创建时间(毫秒) | 证书类型 | 证书长度 | 证书 DER 内容
    """
    body = bytearray()
    body += b"\xfe\xed\xfe\xed"
    body += struct.pack(">i", 2)
    body += struct.pack(">i", 1)
    body += struct.pack(">i", 2)
    body += _write_java_utf(TRUSTSTORE_ALIAS)
    body += struct.pack(">q", int(time.time() * 1000))
    body += _write_java_utf("X.509")
    cert_der = ca_cert.public_bytes(serialization.Encoding.DER)
    body += struct.pack(">i", len(cert_der))
    body += cert_der

    # 校验码：SHA-1(口令的 UTF-16BE 编码 + 固定盐 + 上述全部内容)
    digest = hashlib.sha1()
    digest.update(password.encode("utf-16-be"))
    digest.update("Mighty Aphrodite".encode("utf-8"))
    digest.update(bytes(body))
    return bytes(body) + digest.digest()


def main() -> int:
    parser = argparse.ArgumentParser(
        description="生成 job-gateway HTTPS 证书材料，输出单行 JSON 到标准输出。"
    )
    parser.add_argument(
        "--bits",
        type=int,
        default=2048,
        choices=(2048, 3072, 4096),
        help="RSA 模数位宽，默认 2048",
    )
    parser.add_argument(
        "--days",
        type=int,
        default=3650,
        help="证书有效期天数，默认 3650",
    )
    parser.add_argument(
        "--san",
        action="append",
        metavar="NAME",
        help=f"服务端证书的 SAN，可重复指定；不指定时为 {', '.join(DEFAULT_SANS)}。"
             f"客户端若校验主机名，需在此加入实际访问的域名或 IP",
    )
    parser.add_argument(
        "--keystore-password",
        help="keystore 口令，默认随机生成 24 位字母数字口令",
    )
    parser.add_argument(
        "--truststore-password",
        help="truststore 口令，默认随机生成 24 位字母数字口令",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="将 JSON 格式化多行输出（默认单行便于管道处理）",
    )
    args = parser.parse_args()

    if args.days <= 0:
        print("证书有效期天数必须为正整数", file=sys.stderr)
        return 1

    keystore_password = args.keystore_password or _generate_password()
    truststore_password = args.truststore_password or _generate_password()
    sans = args.san or list(DEFAULT_SANS)

    try:
        ca_key, ca_cert = _generate_ca(args.bits, args.days)
        server_key, server_cert = _generate_server_cert(ca_key, ca_cert, args.bits, args.days, sans)
        keystore = _build_pkcs12_keystore(server_key, server_cert, ca_cert, keystore_password)
        truststore = _build_jks_truststore(ca_cert, truststore_password)
    except Exception as exc:
        print(f"生成 job-gateway HTTPS 证书失败: {exc}", file=sys.stderr)
        return 1

    payload = {
        "gatewayConfig.server.ssl.p12.base64Content": base64.b64encode(keystore).decode("ascii"),
        "gatewayConfig.server.ssl.keystore.password": keystore_password,
        "gatewayConfig.server.ssl.truststore.base64Content": base64.b64encode(truststore).decode("ascii"),
        "gatewayConfig.server.ssl.truststore.password": truststore_password,
    }
    if args.pretty:
        print(json.dumps(payload, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
