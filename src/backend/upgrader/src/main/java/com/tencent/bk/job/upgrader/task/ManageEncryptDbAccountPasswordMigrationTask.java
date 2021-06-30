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

package com.tencent.bk.job.upgrader.task;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.utils.HttpHelperFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.Properties;

import static com.tencent.bk.job.common.constant.ErrorCode.RESULT_OK;

/**
 * DB账号密码加密
 */
@Slf4j
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.3.4.0",
    targetExecuteTime = ExecuteTimeEnum.AFTER_UPDATE_JOB)
public class ManageEncryptDbAccountPasswordMigrationTask extends BaseUpgradeTask {

    public ManageEncryptDbAccountPasswordMigrationTask(Properties properties) {
        super(properties);
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        String jobManageHost = getProperties().getProperty("job-manage.host");
        if (StringUtils.isBlank(jobManageHost)) {
            log.error("job-manage.host is not configured");
            return 1;
        }
        String uri = "/migration/action/encryptDbAccountPassword";
        String url;
        if (!jobManageHost.endsWith("/")) {
            url = jobManageHost + uri;
        } else {
            url = jobManageHost.substring(0, jobManageHost.length() - 1) + uri;
        }

        String jobAuthToken = getProperties().getProperty("job.security.public-key-base64");
        if (StringUtils.isBlank(jobAuthToken)) {
            log.error("job.security.public-key-base64 is not configured");
            return 1;
        }
        jobAuthToken = Base64Util.decodeContentToStr(jobAuthToken);

        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("x-job-auth-token", jobAuthToken);
        headers[1] = new BasicHeader("Content-Type", "application/json");


        String key = getProperties().getProperty("job.encrypt.password");
        if (StringUtils.isBlank(key)) {
            log.error("job.encrypt.password is not configured");
            return 1;
        }

        try {
            String respStr = HttpHelperFactory.getDefaultHttpHelper().post(url, "{\"key\":\"" + key + "\"}",
                headers);
            if (StringUtils.isBlank(respStr)) {
                log.error("Fail:response is blank|uri={}", url);
                throw new ServiceException(ErrorCode.SERVICE_INTERNAL_ERROR, "Encrypt db account password fail");
            }
            ServiceResponse resp = JsonUtils.fromJson(respStr, ServiceResponse.class);
            if (resp == null) {
                log.error("Fail:parse respStr fail|uri={}", url);
                throw new ServiceException(ErrorCode.SERVICE_INTERNAL_ERROR, "Encrypt db account password fail");
            } else if (resp.getCode() != RESULT_OK) {
                log.error("Fail: code!=0");
                throw new ServiceException(ErrorCode.SERVICE_INTERNAL_ERROR, "Encrypt db account password fail");
            }
        } catch (Exception e) {
            throw new ServiceException(ErrorCode.SERVICE_INTERNAL_ERROR, "Encrypt db account password fail");
        }
        return 0;
    }
}
