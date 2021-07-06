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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 作业查询基础类
 */
@Slf4j
public class JobQueryCommonV3Processor {
    @Autowired
    protected ExecuteAuthService executeAuthService;

    @Autowired
    protected AuthService authService;

    @Autowired
    private MessageI18nService i18nService;

    /**
     * 查看作业实例鉴权
     *
     * @param username     用户名
     * @param appId        业务ID
     * @param taskInstance 作业实例
     * @return 鉴权结果
     */
    protected <T> EsbResp<T> authViewTaskInstance(String username, Long appId, TaskInstanceDTO taskInstance) {
        if (taskInstance == null) {
            return EsbResp.buildCommonFailResp(ErrorCode.TASK_INSTANCE_NOT_EXIST, i18nService);
        }
        if (!appId.equals(taskInstance.getAppId())) {
            return EsbResp.buildCommonFailResp(ErrorCode.TASK_INSTANCE_NOT_EXIST, i18nService);
        }

        AuthResult authResult = executeAuthService.authViewTaskInstance(username, appId, taskInstance);
        if (!authResult.isPass()) {
            return authService.buildEsbAuthFailResp(authResult.getRequiredActionResources());
        }
        return EsbResp.buildSuccessResp(null);
    }
}
