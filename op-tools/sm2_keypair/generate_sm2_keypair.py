#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成SM2加密算法的公私钥对
依赖：
- bk-crypto-python-sdk
"""

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

    print('1.生成原始公私钥，可直接用于后端Java SM2Utils使用')

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

    print("原始公钥:")
    print(raw_public_base64)
    print("原始私钥:")
    print(raw_private_base64)

    # 3. 生成PEM公私钥
    print('2.生成PEM公私钥，可直接用于前端cryptoJsSdk使用')

    public_pem = public_key_obj.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    ).decode("utf-8")

    private_pem = private_key_obj.private_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PrivateFormat.TraditionalOpenSSL,
        encryption_algorithm=serialization.NoEncryption()
    ).decode("utf-8")

    print("PEM公钥：")
    print(public_pem)
    print("PEM私钥：")
    print(private_pem)


if __name__ == "__main__":
    generate_sm2_keypair()
