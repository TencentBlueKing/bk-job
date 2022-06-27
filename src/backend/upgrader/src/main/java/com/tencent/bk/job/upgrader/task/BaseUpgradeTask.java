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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.jwt.BasicJwtManager;
import com.tencent.bk.job.common.util.jwt.JwtManager;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

@Slf4j
public abstract class BaseUpgradeTask implements IUpgradeTask {

    public Properties properties;

    BaseUpgradeTask() {
    }

    BaseUpgradeTask(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }

    @Override
    public void init() {
    }

    public String getJobAuthToken() {
        String securityPublicKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
        String securityPrivateKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PRIVATE_KEY_BASE64);
        JwtManager jwtManager;
        try {
            jwtManager = new BasicJwtManager(securityPrivateKeyBase64, securityPublicKeyBase64);
            // 迁移过程最大预估时间：3h
            return jwtManager.generateToken(3 * 60 * 60 * 1000);
        } catch (IOException | GeneralSecurityException e) {
            String msg = "Fail to generate jwt auth token";
            log.error(msg, e);
            throw new InternalException(msg, e, ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getDataStartVersion() {
        return this.getClass().getAnnotation(UpgradeTask.class).dataStartVersion();
    }

    @Override
    public String getTargetVersion() {
        return this.getClass().getAnnotation(UpgradeTask.class).targetVersion();
    }

    @Override
    public int getPriority() {
        return this.getClass().getAnnotation(UpgradeTask.class).priority();
    }
}
