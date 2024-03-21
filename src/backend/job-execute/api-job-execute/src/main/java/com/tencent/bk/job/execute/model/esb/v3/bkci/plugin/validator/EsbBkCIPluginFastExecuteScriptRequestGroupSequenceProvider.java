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

package com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.validator;

import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v3.bkci.plugin.EsbBkCIPluginFastExecuteScriptRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * 主机联合校验
 */
@Slf4j
public class EsbBkCIPluginFastExecuteScriptRequestGroupSequenceProvider
    implements DefaultGroupSequenceProvider<EsbBkCIPluginFastExecuteScriptRequest> {

    @Override
    public List<Class<?>> getValidationGroups(EsbBkCIPluginFastExecuteScriptRequest request) {
        List<Class<?>> validationGroups = new ArrayList<>();
        validationGroups.add(EsbBkCIPluginFastExecuteScriptRequest.class);
        if (request != null) {
            // 优先级 accountId > accountAlias
            if (request.getAccountId() != null) {
                validationGroups.add(ValidationGroups.Account.AccountId.class);
            } else if (request.getAccountAlias() != null) {
                validationGroups.add(ValidationGroups.Account.AccountAlias.class);
            } else {
                validationGroups.add(ValidationGroups.Account.AccountId.class);
            }

            // 脚本优先级 scriptVersionId > scriptId > scriptContent
            if (request.getScriptVersionId() != null) {
                validationGroups.add(ValidationGroups.Script.ScriptVersionId.class);
            } else if (request.getScriptId() != null) {
                validationGroups.add(ValidationGroups.Script.ScriptId.class);
            } else if (request.getContent() != null) {
                validationGroups.add(ValidationGroups.Script.ScriptContent.class);
            } else {
                validationGroups.add(ValidationGroups.Script.ScriptVersionId.class);
            }
        }
        return validationGroups;
    }
}
