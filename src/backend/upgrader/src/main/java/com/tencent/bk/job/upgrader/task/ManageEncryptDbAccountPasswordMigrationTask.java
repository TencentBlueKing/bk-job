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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import com.tencent.bk.job.upgrader.utils.HttpHelperFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.List;
import java.util.Properties;

/**
 * DB账号密码加密
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class)
})
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
        String jobManageServerAddress = getProperties().getProperty(
            ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS
        );
        String jobManageHost = "";
        if (!jobManageServerAddress.startsWith("http://") && !jobManageServerAddress.startsWith("https://")) {
            jobManageHost = "http://" + jobManageServerAddress;
        }
        String uri = "/migration/action/encryptDbAccountPassword";
        String url;
        if (!jobManageHost.endsWith("/")) {
            url = jobManageHost + uri;
        } else {
            url = jobManageHost.substring(0, jobManageHost.length() - 1) + uri;
        }

        String jobAuthToken = getProperties().getProperty(
            ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64
        );
        if (StringUtils.isBlank(jobAuthToken)) {
            log.error("{} is not configured", ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
            return 1;
        }
        jobAuthToken = Base64Util.decodeContentToStr(jobAuthToken);

        Header[] headers = new Header[2];
        headers[0] = new BasicHeader("x-job-auth-token", jobAuthToken);
        headers[1] = new BasicHeader("Content-Type", "application/json");


        String key = getProperties().getProperty(ParamNameConsts.CONFIG_PROPERTY_JOB_ENCRYPT_PASSWORD);
        if (StringUtils.isBlank(key)) {
            log.error("{} is not configured", ParamNameConsts.CONFIG_PROPERTY_JOB_ENCRYPT_PASSWORD);
            return 1;
        }

        try {
            String respStr = HttpHelperFactory.getDefaultHttpHelper().post(url, "", headers);
            if (StringUtils.isBlank(respStr)) {
                log.error("Fail:response is blank|uri={}", url);
                throw new InternalException(ErrorCode.INTERNAL_ERROR, "Encrypt db account password fail");
            }
            InternalResponse<List<Long>> resp = JsonUtils.fromJson(respStr,
                new TypeReference<InternalResponse<List<Long>>>() {
                });
            if (resp == null) {
                log.error("Fail:parse respStr fail|uri={}", url);
                throw new InternalException(ErrorCode.INTERNAL_ERROR, "Encrypt db account password fail");
            } else if (!resp.isSuccess()) {
                log.error("Fail: code!=0");
                throw new InternalException(ErrorCode.INTERNAL_ERROR, "Encrypt db account password fail");
            }
            log.info("Encrypt db account password successfully!accountIds: {}", resp.getData());
        } catch (Exception e) {
            log.error("Fail: caught exception", e);
            throw new InternalException(ErrorCode.INTERNAL_ERROR, "Encrypt db account password fail");
        }
        return 0;
    }
}
