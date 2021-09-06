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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.manage.api.iam.IamScriptCallbackResource;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IamScriptCallbackResourceImpl implements IamScriptCallbackResource {
    private ScriptCallbackHelper scriptCallbackHelper;

    @Autowired
    public IamScriptCallbackResourceImpl(ScriptService scriptService) {
        this.scriptCallbackHelper = new ScriptCallbackHelper(scriptService, new ScriptCallbackHelper.IGetBasicInfo() {
            @Override
            public Pair<ScriptQueryDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
                return getBasicQueryConditionImpl(callbackRequest);
            }

            @Override
            public boolean isPublicScript() {
                return false;
            }
        });
    }

    private Pair<ScriptQueryDTO, BaseSearchCondition> getBasicQueryConditionImpl(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        ScriptQueryDTO scriptQuery = new ScriptQueryDTO();
        scriptQuery.setAppId(searchCondition.getAppIdList().get(0));
        return Pair.of(scriptQuery, baseSearchCondition);
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        return scriptCallbackHelper.doCallback(callbackRequest);
    }
}
