export const getStringByteCount = (str) =>  {
  let byteCount = 0;

  for (let i = 0; i < str.length; i++) {
    const codePoint = str.codePointAt(i);

    if (codePoint <= 0x7F) {
      byteCount += 1;
    } else if (codePoint <= 0x7FF) {
      byteCount += 2;
    } else if (codePoint <= 0xFFFF) {
      byteCount += 3;
    } else {
      byteCount += 4;
    }

    // 如果当前字符是代理对的第一个部分，跳过下一个代码单元
    if (codePoint >= 0x10000) {
      i = i + 1;
    }
  }

  return byteCount;
};
