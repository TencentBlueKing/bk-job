/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.util.file;

import org.mozilla.intl.chardet.nsDetector;
import org.mozilla.intl.chardet.nsPSMDetector;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 编码探测
 *
 * @version 1.0
 * @time 2017/6/23.
 */
public class CharsetDetectHelper {

    private boolean found = false;
    private String result;
    private int lang;

    /**
     * lang参数的值见以下常量
     *
     * @param lang
     * @see nsPSMDetector.ALL
     * @see nsPSMDetector.JAPANESE
     * @see nsPSMDetector.CHINESE
     * @see nsPSMDetector.SIMPLIFIED_CHINESE
     * @see nsPSMDetector.TRADITIONAL_CHINESE
     */
    public CharsetDetectHelper(int lang) {
        this.lang = lang;
    }

    public CharsetDetectHelper() {
        this(nsPSMDetector.CHINESE);
    }

    public String[] detectCharset(InputStream in) throws IOException {
        nsDetector det = new nsDetector(lang);
        det.Init(charset -> {
            found = true;
            result = charset;
        });
        BufferedInputStream imp = new BufferedInputStream(in);
        byte[] buf = new byte[1024];
        int len;
        boolean isAscii = true;
        while ((len = imp.read(buf, 0, buf.length)) != -1) {
            if (isAscii) {
                isAscii = det.isAscii(buf, len);
            }
            if (!isAscii) {
                if (det.DoIt(buf, len, false)) {
                    break;
                }
            }
        }
        imp.close();
        in.close();
        det.DataEnd();
        return judgeCharset(det, isAscii);
    }

    public String[] detectCharset(byte[] buf) throws IOException {
        if (buf == null) {
            throw new IOException("buf is empty");
        }
        nsDetector det = new nsDetector(lang);
        det.Init(charset -> {
            found = true;
            result = charset;
        });
        int len = buf.length;
        boolean isAscii = det.isAscii(buf, len);
        if (!isAscii) {
            det.DoIt(buf, len, false);
        }
        det.DataEnd();
        return judgeCharset(det, isAscii);
    }

    private String[] judgeCharset(nsDetector det, boolean isAscii) {
        String[] probCharsets;
        if (isAscii) {
            found = true;
            probCharsets = new String[]{"ASCII"};
        } else if (found) {
            probCharsets = new String[]{result};
        } else {
            probCharsets = det.getProbableCharsets();
        }
        return probCharsets;
    }
}
