import cryptoJsSdk from '@blueking/crypto-js-sdk';

export const encrypt = (algorithm, publicKey, message) => {
  if (message === '******' || !message) {
    return message;
  }
  if (algorithm.toUpperCase() === 'SM2') {
    const sm2 = new cryptoJsSdk.SM2();
    // 获取实际的公钥，类型为 hex 字符串
    const pkey = cryptoJsSdk.helper.asn1.decode(publicKey);
    return cryptoJsSdk.helper.encode.hexToBase64(sm2.encrypt(pkey, cryptoJsSdk.helper.encode.strToHex(message)));
  }

  if (algorithm.toUpperCase() === 'SM4') {
    // 生成 iv，用于 sm4 加密
    const iv = cryptoJsSdk.sm4.createIV();
    const transformKey = (key) => {
      let bufferText = Buffer.from(key, 'utf8');
      // 小于 16 字节，则不足的字节补 0
      if (bufferText.length < 16) {
        const addBuf = Buffer.alloc(16 - bufferText.length);
        for (let i = 0; i < 16 - bufferText.length; i++) {
          addBuf.fill(0);
        }
        bufferText = Buffer.concat([bufferText, addBuf]);
      } else if (bufferText.length > 16) {
        // 大于 16 字节，取前 16 字节
        bufferText = bufferText.subarray(0, 16);
      }
      return bufferText;
    };
    const encryptMessage = cryptoJsSdk.sm4.encrypt(
      cryptoJsSdk.helper.encode.strToBuf(message),
      transformKey(publicKey),
      Array.from(iv),
      'CTR',
    );
    const encryptRet = Buffer.concat([iv, Buffer.from(encryptMessage)]);
    return cryptoJsSdk.helper.encode.bufToHex(encryptRet);
  }
};
