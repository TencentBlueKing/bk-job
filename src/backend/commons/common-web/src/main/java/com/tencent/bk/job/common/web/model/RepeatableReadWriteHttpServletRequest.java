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

package com.tencent.bk.job.common.web.model;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 由于HttpServletRequest的InputStream只允许读取一次，为了能够重复读写body，需要用装饰模式来包装
 */
@Slf4j
public class RepeatableReadWriteHttpServletRequest extends HttpServletRequestWrapper {
    private String body;

    public RepeatableReadWriteHttpServletRequest(HttpServletRequest request) {
        super(request);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                char[] charBuffer = new char[1024];
                int bytesRead;
                while ((bytesRead = br.read(charBuffer)) > 0) {
                    stringBuilder.append(charBuffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            log.error("Read HttpServletRequest stream fail", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("Close HttpServletRequest stream fail", e);
                }
            }
        }
        body = stringBuilder.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        // Check if this is a JSON request that needs XSS filtering
        if (StrUtil.startWithIgnoreCase(getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            // Apply XSS filtering to the body content
            String filteredBody = filterXss(body);
            final ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(filteredBody.getBytes(StandardCharsets.UTF_8));
            
            // Return a properly implemented ServletInputStream
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
    
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
    
                @Override
                public boolean isReady() {
                    return true;
                }
    
                @Override
                public void setReadListener(ReadListener listener) {
                    // No implementation needed as we're using a ByteArrayInputStream
                    // which is always immediately available
                }
            };
        } else {
            // For non-JSON content, use the original body without filtering
            final ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
            
            return new ServletInputStream() {
                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
    
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }
    
                @Override
                public boolean isReady() {
                    return true;
                }
    
                @Override
                public void setReadListener(ReadListener listener) {
                    // No implementation needed
                }
            };
        }
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream(), StandardCharsets.UTF_8));
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String newBody) {
        this.body = newBody;
    }
}
