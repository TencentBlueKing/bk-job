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

package com.tencent.bk.job.manage.api.web.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.web.WebVersionLogResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;

@RestController
@Slf4j
public class WebVersionLogResourceImpl implements WebVersionLogResource {
    private static final String LOG_DIR = "versionLog/";
    private static final String LOG_FILE_NAME_PREFIX = "bundledVersionLog";
    private static final String LOG_FILE_NAME_SUFFIX = ".json";
    private static final String LOG_FILE_NAME_SEPARATOR = "_";

    @Override
    public Response<Object> getVersionLog() {
        String userLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        String fileName = getFileNameByLang(userLang);
        String logFilePath = buildFilePath(fileName);
        log.debug("Get version log, userLang={}, logFilePath={}", userLang, logFilePath);
        ClassPathResource resource = new ClassPathResource(logFilePath);
        if (!resource.exists()) {
            log.warn("Version log file not found: {}", logFilePath);
            throw new NotFoundException(ErrorCode.VERSION_LOG_FILE_NOT_FOUND, new String[]{fileName});
        }

        try (InputStream inputStream = resource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            Object versionLog = mapper.readValue(inputStream, Object.class);
            return Response.buildSuccessResp(versionLog);
        } catch (IOException e) {
            log.error("Reading version log file failure, path={}", logFilePath, e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * 按语言获取日志文件名称
     */
    private String getFileNameByLang(String userLang) {
        if (LocaleUtils.LANG_EN.equals(userLang)
            || LocaleUtils.LANG_EN_US.equals(userLang)) {
            return buildFileName(LocaleUtils.LANG_EN);
        }
        if (LocaleUtils.LANG_ZH.equals(userLang)
            || LocaleUtils.LANG_ZH_CN.equals(userLang)) {
            return buildFileName(LocaleUtils.LANG_ZH_CN);
        }
        return buildFileName(null);
    }

    /**
     * 构建版本日志文件路径
     */
    private String buildFilePath(String fileName) {
        return LOG_DIR + fileName;
    }

    /**
     * 版本日志文件名称，格式：bundledVersionLog[_语言后缀].json
     */
    private String buildFileName(String langSuffix) {
        if (StringUtils.isBlank(langSuffix)) {
            return LOG_FILE_NAME_PREFIX + LOG_FILE_NAME_SUFFIX;
        }
        return LOG_FILE_NAME_PREFIX
            + LOG_FILE_NAME_SEPARATOR
            + langSuffix
            + LOG_FILE_NAME_SUFFIX;
    }
}
