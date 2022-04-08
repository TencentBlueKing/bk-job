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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;

import java.io.IOException;
import java.util.List;

@Slf4j
public class ExtHttpHelper {
    private final String CHARSET = "UTF-8";

    private final HttpHelper httpHelper;

    protected ExtHttpHelper(HttpHelper httpHelper) {
        this.httpHelper = httpHelper;
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
        return httpHelper.post(url, requestEntity, headers).getRight();
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
        return httpHelper.get(keepAlive, url, header).getRight();
    }

    public CloseableHttpResponse getRawResp(boolean keepAlive, String url, Header[] header) {
        return httpHelper.getRawResp(keepAlive, url, header);
    }

    public String put(String url, HttpEntity requestEntity, Header... headers) {
        return httpHelper.put(url, requestEntity, headers).getRight();
    }

    /**
     * 发起PUT请求并获取响应
     *
     * @param url        地址
     * @param charset    字符集名称
     * @param content    body内容
     * @param headerList 请求头列表
     * @return 响应字符串
     */
    public String put(String url, String charset, String content, List<Header> headerList) {
        Header[] headers = new Header[headerList.size()];
        try {
            return put(url, new ByteArrayEntity(content.getBytes(charset)), headerList.toArray(headers));
        } catch (IOException e) {
            log.error("Put request fail", e);
            throw new InternalException(e, ErrorCode.API_ERROR);
        }
    }

    /**
     * 发起DELETE请求并获取响应
     *
     * @param url     地址
     * @param content body内容
     * @param headers 请求头数组
     * @return 响应字符串
     */
    public String delete(String url, String content, Header... headers) {
        return httpHelper.delete(url, content, headers).getRight();
    }

    /**
     * 发起DELETE请求并获取响应
     *
     * @param url        地址
     * @param content    body内容
     * @param headerList 请求头列表
     * @return 响应字符串
     */
    public String delete(String url, String content, List<Header> headerList) {
        Header[] headers = new Header[headerList.size()];
        return httpHelper.delete(url, content, headerList.toArray(headers)).getRight();
    }

    /**
     * 发起DELETE请求并获取响应
     *
     * @param url        地址
     * @param headerList 请求头列表
     * @return 响应字符串
     */
    public String delete(String url, List<Header> headerList) {
        return delete(url, null, headerList);
    }
}
