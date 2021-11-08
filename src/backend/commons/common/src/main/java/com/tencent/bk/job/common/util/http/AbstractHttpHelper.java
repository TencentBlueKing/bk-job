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

package com.tencent.bk.job.common.util.http;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class AbstractHttpHelper {
    private final String CHARSET = "UTF-8";

    abstract CloseableHttpClient getHttpClient();

    /**
     * 提交POST请求，并返回数据，用encoding参数解析
     *
     * @param url         提交的地址
     * @param content     提交的内容字符串
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return
     */
    public String post(String url, String content, String contentType) {
        return post(url, CHARSET, content, contentType);
    }

    /**
     * 提交POST请求，并返回数据，用encoding参数解析
     *
     * @param url         提交的地址
     * @param charset     字符集，用于解析返回的字符串
     * @param content     提交的内容字符串
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return 返回字符串
     */
    public String post(String url, String charset, String content, String contentType) {
        try {
            byte[] resp = post(url, content.getBytes(charset), contentType);
            if (null == resp) {
                return null;
            }
            return new String(resp, charset);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * 提交POST请求，并返回数据（默认采用encoding常量指定的字符集解析）
     *
     * @param url     提交的地址
     * @param content 提交的内容字符串
     * @return 返回字符串
     */
    public String post(String url, String content) {
        return post(url, CHARSET, content, "application/x-www-form-urlencoded");
    }

    /**
     * 支持自定义头的POST请求
     *
     * @param url     提交的地址
     * @param content 提交的内容字符串
     * @param headers 自定义请求头
     * @return
     */
    public String post(String url, String content, Header... headers) {
        return post(url, CHARSET, content, headers);
    }

    public String post(String url, String charset, String content, List<Header> headerList) throws Exception {
        Header[] headers = new Header[headerList.size()];
        return post(url, charset, content, headerList.toArray(headers));
    }


    /**
     * 支持自定义头的POST请求
     *
     * @param url     提交的地址
     * @param charset 字符集，用于解析返回的字符串
     * @param content 提交的内容字符串
     * @param headers 自定义请求头
     * @return
     */
    public String post(String url, String charset, String content, Header... headers) {
        log.debug("post:url={},charset={},content={},headers={}", url, charset, content, headers);
        try {
            byte[] resp = post(url, new ByteArrayEntity(content.getBytes(charset)), headers);
            if (null == resp) {
                return null;
            }
            return new String(resp, charset);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * 提交POST请求，并返回字节数组
     *
     * @param url         提交的地址
     * @param content     提交的内容字节数据
     * @param contentType 默认传null则为"application/x-www-form-urlencoded"
     * @return 返回字节数组
     */
    public byte[] post(String url, byte[] content, String contentType) {
        return post(url, new ByteArrayEntity(content), contentType);
    }

    /**
     * 提交POST请求，并返回字节数组
     *
     * @param url           提交的地址
     * @param requestEntity 封装好的请求实体
     * @param contentType   默认传null则为"application/x-www-form-urlencoded"
     * @return 返回字节数组
     */
    public byte[] post(String url, HttpEntity requestEntity, String contentType) {
        return post(url, requestEntity,
            new BasicHeader("Content-Type", contentType == null ? "application/x-www-form-urlencoded" : contentType));
    }

    public byte[] post(String url, HttpEntity requestEntity, Header... headers) {
        HttpPost post = new HttpPost(url);
        // 设置为长连接，服务端判断有此参数就不关闭连接。
        post.setHeader("Connection", "Keep-Alive");
        post.setHeaders(headers);
        post.setEntity(requestEntity);
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(post)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                log.info(
                    "Post request fail, statusCode={}, errorReason={}, url={}, headers={}",
                    statusCode,
                    message,
                    url,
                    headers
                );
                throw new InternalException(ErrorCode.API_ERROR, message);
            }
            HttpEntity entity = httpResponse.getEntity();
            return EntityUtils.toByteArray(entity);
        } catch (IOException e) {
            log.error("Post request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * GET请求，并返回字符串
     *
     * @param url 提交的地址
     * @return
     */
    public String get(String url) {
        return get(url, (Header[]) null);
    }

    public String get(String url, List<Header> headerList) {
        Header[] headers = new Header[headerList.size()];
        return get(url, headerList.toArray(headers));
    }

    public String get(String url, Header[] header) {
        return get(true, url, header);
    }

    public String get(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        try (CloseableHttpResponse response = getHttpClient().execute(get)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, CHARSET);
        } catch (IOException e) {
            log.error("Get request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    public CloseableHttpResponse getRawResp(boolean keepAlive, String url, Header[] header) {
        HttpGet get = new HttpGet(url);
        if (keepAlive) {
            get.setHeader("Connection", "Keep-Alive");
        }
        if (header != null && header.length > 0) {
            get.setHeaders(header);
        }
        try {
            return getHttpClient().execute(get);
        } catch (IOException e) {
            log.error("Get request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    public String put(String url, HttpEntity requestEntity, Header... headers) {
        HttpPut put = new HttpPut(url);
        // 设置为长连接，服务端判断有此参数就不关闭连接。
        put.setHeader("Connection", "Keep-Alive");
        put.setHeaders(headers);
        put.setEntity(requestEntity);
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(put)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                log.info("Put request fail, statusCode={}, errorReason={}", statusCode, message);
                throw new InternalException(ErrorCode.API_ERROR, message);
            }
            HttpEntity entity = httpResponse.getEntity();
            return new String(EntityUtils.toByteArray(entity), CHARSET);
        } catch (IOException e) {
            log.error("Put request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    public String delete(String url, String content, Header... headers) {
        FakeHttpDelete delete = new FakeHttpDelete(url);
        try (CloseableHttpResponse httpResponse = getHttpClient().execute(delete)) {
            HttpEntity requestEntity = new ByteArrayEntity(content.getBytes(CHARSET));
            delete.setEntity(requestEntity);
            delete.setHeaders(headers);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                String message = httpResponse.getStatusLine().getReasonPhrase();
                log.info("Delete request fail, url={}, statusCode={}, errorReason={}", url, statusCode, message);
                throw new InternalException(ErrorCode.API_ERROR, String.format("url=%s,statusCode=%s" +
                    "，message=%s", url, statusCode, message));
            }
            HttpEntity entity = httpResponse.getEntity();
            byte[] respBytes = EntityUtils.toByteArray(entity);
            if (respBytes == null) {
                return null;
            }
            return new String(respBytes, CHARSET);
        } catch (IOException e) {
            log.error("Delete request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    private static class FakeHttpDelete extends HttpPost {
        public FakeHttpDelete(String url) {
            super(url);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }
    }
}
