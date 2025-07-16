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
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.file_gateway.config.ArtifactoryConfig;
import com.tencent.bk.job.file_gateway.consts.FileSourceWhiteInfoTypeConsts;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.service.validation.FileSourceValidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        // 1.默认允许对接当前环境的蓝鲸制品库或其子域名
        if (isUrlOrChildUrlOfCurrentEnv(baseUrl)) {
            return;
        }
        // 2.对接其他环境蓝鲸制品库需要添加白名单
        boolean existsWhiteInfo = fileSourceWhiteInfoDAO.exists(
            FileSourceWhiteInfoTypeConsts.BK_ARTIFACTORY_BASE_URL,
            baseUrl
        );
        if (!existsWhiteInfo) {
            throw new InvalidParamException(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID);
        }
    }

    /**
     * 判断URL是否为当前环境制品库的地址或子域名地址
     *
     * @param url 目标URL
     * @return 布尔值
     */
    private boolean isUrlOrChildUrlOfCurrentEnv(String url) {
        String baseUrlOfCurrentEnv = artifactoryConfig.getArtifactoryBaseUrl();
        if (baseUrlOfCurrentEnv.equals(url)) {
            return true;
        }
        String urlWithoutScheme = StringUtil.removeHttpOrHttpsSchemeOfUrl(baseUrlOfCurrentEnv);
        int indexOfPath = urlWithoutScheme.indexOf("/");
        String domain = urlWithoutScheme;
        if (indexOfPath != -1) {
            domain = urlWithoutScheme.substring(0, indexOfPath);
        }
        String suffix = "." + domain.trim();
        return url.endsWith(suffix);
    }
}
