/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.file_gateway.service.validation.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.file_gateway.config.ArtifactoryConfig;
import com.tencent.bk.job.file_gateway.consts.FileSourceWhiteInfoTypeConsts;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.service.validation.FileSourceValidateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

@Slf4j
@Service
public class FileSourceValidateServiceImpl implements FileSourceValidateService {

    private final ArtifactoryConfig artifactoryConfig;
    private final FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO;

    @Autowired
    public FileSourceValidateServiceImpl(ArtifactoryConfig artifactoryConfig,
                                         FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO) {
        this.artifactoryConfig = artifactoryConfig;
        this.fileSourceWhiteInfoDAO = fileSourceWhiteInfoDAO;
    }

    @Override
    public void checkBkArtifactoryBaseUrl(String baseUrl) {
        // 1.仅允许 http/https 协议
        String host = parseHttpUrlHost(baseUrl);
        if (host == null) {
            throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
        }
        // 2.默认允许对接当前环境的蓝鲸制品库或其子域名，但解析结果不能指向环回/内网/链路本地地址
        if (isHostOrChildHostOfCurrentEnv(host) && !isResolvedToInternalAddress(host)) {
            return;
        }
        // 3.其他情况（含当前环境制品库地址未配置）必须添加白名单
        boolean existsWhiteInfo = fileSourceWhiteInfoDAO.exists(
            FileSourceWhiteInfoTypeConsts.BK_ARTIFACTORY_BASE_URL,
            baseUrl
        );
        if (!existsWhiteInfo) {
            log.info("BkArtifactory baseUrl is not allowed: {}", baseUrl);
            throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
        }
    }

    /**
     * 解析 http/https URL 的 host
     *
     * @param url 目标URL
     * @return 小写的 host，URL 非法或协议不是 http/https 时返回 null
     */
    private String parseHttpUrlHost(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        URL parsedUrl;
        try {
            parsedUrl = new URL(url.trim());
        } catch (MalformedURLException e) {
            log.info("Invalid url: {}", url);
            return null;
        }
        String protocol = parsedUrl.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return null;
        }
        String host = parsedUrl.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        return host.toLowerCase(Locale.ROOT);
    }

    /**
     * 判断 host 是否为当前环境制品库的域名或其子域名，当前环境制品库地址未配置时一律返回 false
     *
     * @param host 目标 host
     * @return 布尔值
     */
    private boolean isHostOrChildHostOfCurrentEnv(String host) {
        String envHost = parseHttpUrlHost(artifactoryConfig.getArtifactoryBaseUrl());
        if (envHost == null) {
            return false;
        }
        if (host.equals(envHost)) {
            return true;
        }
        String[] hostLabels = host.split("\\.", -1);
        String[] envHostLabels = envHost.split("\\.", -1);
        if (hostLabels.length <= envHostLabels.length) {
            return false;
        }
        int offset = hostLabels.length - envHostLabels.length;
        for (int i = 0; i < envHostLabels.length; i++) {
            if (!envHostLabels[i].equals(hostLabels[offset + i])) {
                return false;
            }
        }
        for (int i = 0; i < offset; i++) {
            if (hostLabels[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断 host 解析出的地址中是否存在环回、内网、链路本地等地址，无法解析时按不安全处理
     *
     * @param host 目标 host
     * @return 布尔值
     */
    private boolean isResolvedToInternalAddress(String host) {
        InetAddress[] addresses;
        try {
            addresses = resolveHost(host);
        } catch (UnknownHostException e) {
            log.info("Fail to resolve host: {}", host);
            return true;
        }
        if (addresses == null || addresses.length == 0) {
            return true;
        }
        for (InetAddress address : addresses) {
            if (isInternalAddress(address)) {
                log.info("Host {} is resolved to internal address {}", host, address.getHostAddress());
                return true;
            }
        }
        return false;
    }

    InetAddress[] resolveHost(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    static boolean isInternalAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isSiteLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        // IPv6 唯一本地地址（ULA）
        return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
