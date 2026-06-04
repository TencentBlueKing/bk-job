#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成SM2加密算法的公私钥对
依赖：
- bk-crypto-python-sdk
"""

import argparse
import json
import sys
from bkcrypto.contrib.basic.ciphers import get_asymmetric_cipher
from bkcrypto import constants
from bkcrypto.asymmetric.ciphers import BaseAsymmetricCipher
from bkcrypto.asymmetric.options import SM2AsymmetricOptions
from base64 import b64encode
from tongsuopy.crypto import serialization


def generate_sm2_keypair():
    """
    生成一套SM2密钥对：
    1. Job后端（SM2Utils）使用的原始公私钥
    2. 前端（cryptoJsSdk）使用的PEM公钥
    """

    # 1 生成SM2 Cipher对象，None表示随机生成密钥
    sm2_cipher: BaseAsymmetricCipher = get_asymmetric_cipher(
        cipher_type=constants.AsymmetricCipherType.SM2.value,
        cipher_options={
            constants.AsymmetricCipherType.SM2.value: SM2AsymmetricOptions(
                private_key_string=None
            )
        }
    )

    private_key_obj = sm2_cipher.config.private_key
    public_key_obj = sm2_cipher.config.public_key

    # 2.生成原始公私钥
    # 原始公钥 (uncompressed, 65 bytes)
    # 公钥点的 x,y 拼接，加上前缀 0x04
    numbers = public_key_obj.public_numbers()
    x_bytes = numbers.x.to_bytes(32, byteorder="big")
    y_bytes = numbers.y.to_bytes(32, byteorder="big")
    raw_public_bytes = b"\x04" + x_bytes + y_bytes
    raw_public_base64 = b64encode(raw_public_bytes).decode("utf-8")

    # 原始私钥 (32 bytes)
    raw_private_bytes = private_key_obj.private_numbers().private_value.to_bytes(32, byteorder="big")
    raw_private_base64 = b64encode(raw_private_bytes).decode("utf-8")

    # 3. 生成PEM公私钥
    public_pem = public_key_obj.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    ).decode("utf-8")

    private_pem = private_key_obj.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.TraditionalOpenSSL,
        encryption_algorithm=serialization.NoEncryption()
    ).decode("utf-8")

    return {
        "job.encrypt.sm2PublicKey": raw_public_base64,
        "job.encrypt.sm2PrivateKey": raw_private_base64,
        "pemSM2PublicKey": public_pem,
        "pemSM2PrivateKey": private_pem,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="生成SM2密钥对，并输出JSON到标准输出"
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="格式化输出JSON，默认输出单行JSON便于管道处理",
    )
    args = parser.parse_args()

    try:
        payload = generate_sm2_keypair()
    except Exception as exc:
        print(f"生成SM2密钥对失败: {exc}", file=sys.stderr)
        return 1

    if args.pretty:
        print(json.dumps(payload, indent=2, ensure_ascii=False))
    else:
        print(json.dumps(payload, ensure_ascii=False, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
