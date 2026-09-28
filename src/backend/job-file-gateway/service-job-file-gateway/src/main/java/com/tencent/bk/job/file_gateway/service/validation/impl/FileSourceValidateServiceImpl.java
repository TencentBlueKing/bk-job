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
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.http.HttpUrlSafetyUtils;
import com.tencent.bk.job.file_gateway.config.ArtifactoryConfig;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import com.tencent.bk.job.file_gateway.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file_gateway.consts.FileSourceWhiteInfoTypeConsts;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.service.validation.FileSourceValidateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@Slf4j
@Service
public class FileSourceValidateServiceImpl implements FileSourceValidateService {

    private final ArtifactoryConfig artifactoryConfig;
    private final FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO;
    private HttpUrlSafetyUtils.HostResolver hostResolver = this::resolveHost;

    @Autowired
    public FileSourceValidateServiceImpl(ArtifactoryConfig artifactoryConfig,
                                         FileSourceWhiteInfoDAO fileSourceWhiteInfoDAO) {
        this.artifactoryConfig = artifactoryConfig;
        this.fileSourceWhiteInfoDAO = fileSourceWhiteInfoDAO;
    }

    @Override
    public void checkFileSource(String fileSourceTypeCode, Map<String, Object> fileSourceInfoMap) {
        if (FileSourceTypeEnum.isBlueKingArtifactory(fileSourceTypeCode)) {
            checkBkArtifactoryBaseUrl(getString(fileSourceInfoMap, FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL));
            return;
        }
        rejectUnsupportedType(fileSourceTypeCode);
    }

    @Override
    public void checkBkArtifactoryBaseUrl(String baseUrl) {
        String host = HttpUrlSafetyUtils.parseHttpUrlHost(baseUrl);
        if (host == null) {
            rejectBkArtifactory(baseUrl, "invalid http(s) URL or host");
        }
        String envHost = HttpUrlSafetyUtils.parseHttpUrlHost(artifactoryConfig.getArtifactoryBaseUrl());
        boolean envMatch = HttpUrlSafetyUtils.isHostOrChildHost(host, envHost);
        if (envMatch && !HttpUrlSafetyUtils.isResolvedToLocalHostAddress(host, hostResolver)) {
            return;
        }
        boolean existsWhiteInfo = fileSourceWhiteInfoDAO.exists(
            FileSourceWhiteInfoTypeConsts.BK_ARTIFACTORY_BASE_URL,
            baseUrl
        );
        if (!existsWhiteInfo) {
            String reason = envMatch
                ? "host matches current env artifactory domain but resolved to loopback/any-local address "
                + "or failed to resolve, "
                + "and not in whitelist"
                : "host is not current env artifactory domain/subdomain and not in whitelist";
            rejectBkArtifactory(baseUrl, reason);
        }
    }

    private void rejectBkArtifactory(String baseUrl, String reason) {
        log.warn("BkArtifactory baseUrl rejected: reason={}, baseUrl={}",
            reason, HttpUrlSafetyUtils.toLogSafeUrl(baseUrl, 512));
        throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
    }

    private void rejectUnsupportedType(String fileSourceTypeCode) {
        log.warn("FileSource rejected: reason=file source type not supported, fileSourceTypeCode={}",
            LogUtil.sanitizeForLog(fileSourceTypeCode, 64));
        throw new InvalidParamException(ErrorCode.FILE_SOURCE_TYPE_NOT_SUPPORTED,
            new String[]{fileSourceTypeCode});
    }

    @Override
    public void validateWhiteBaseUrl(String baseUrl) {
        if (!HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl(baseUrl)) {
            throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
        }
    }

    InetAddress[] resolveHost(String host) throws UnknownHostException {
        return InetAddress.getAllByName(host);
    }

    void setHostResolver(HttpUrlSafetyUtils.HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    private static String getString(Map<String, Object> fileSourceInfoMap, String key) {
        if (fileSourceInfoMap == null) {
            return null;
        }
        Object value = fileSourceInfoMap.get(key);
        return value == null ? null : String.valueOf(value);
    }
}
