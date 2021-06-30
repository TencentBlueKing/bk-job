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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptStepV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskScriptStepDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskScriptStepVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * @since 3/10/2019 11:12
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskScriptStepDTO {
    private Long id;

    private Long templateId;

    private Long planId;

    private Long instanceId;

    private Long stepId;

    private TaskScriptSourceEnum scriptSource;

    private String scriptId;

    private Long scriptVersionId;

    private String content;

    private ScriptTypeEnum language;

    private String scriptParam;

    private Long timeout;

    private Long account;

    private TaskTargetDTO executeTarget;

    private Boolean secureParam;

    private Integer status;

    private Boolean ignoreError;

    public static TaskScriptStepVO toVO(TaskScriptStepDTO scriptStep) {
        if (scriptStep == null) {
            return null;
        }
        TaskScriptStepVO scriptStepVO = new TaskScriptStepVO();
        scriptStepVO.setScriptSource(scriptStep.getScriptSource().getType());
        scriptStepVO.setScriptId(scriptStep.getScriptId());
        scriptStepVO.setScriptVersionId(scriptStep.getScriptVersionId());
        if (StringUtils.isNotBlank(scriptStep.getContent())) {
            scriptStepVO
                .setContent(Base64.encodeBase64String(scriptStep.getContent().getBytes(StandardCharsets.UTF_8)));
        } else {
            scriptStepVO.setContent(null);
        }
        scriptStepVO.setScriptLanguage(scriptStep.getLanguage().getValue());
        scriptStepVO.setScriptParam(scriptStep.getScriptParam());
        scriptStepVO.setTimeout(scriptStep.getTimeout());
        scriptStepVO.setAccount(scriptStep.getAccount());
        scriptStepVO.setExecuteTarget(TaskTargetDTO.toVO(scriptStep.getExecuteTarget()));
        scriptStepVO.setSecureParam(scriptStep.getSecureParam() ? 1 : 0);
        scriptStepVO.setStatus(scriptStep.getStatus());
        scriptStepVO.setIgnoreError(scriptStep.getIgnoreError() ? 1 : 0);
        return scriptStepVO;
    }

    public static TaskScriptStepDTO fromVO(Long stepId, TaskScriptStepVO scriptStepVO) {
        if (scriptStepVO == null) {
            return null;
        }
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setStepId(stepId);
        scriptStep.setScriptSource(TaskScriptSourceEnum.valueOf(scriptStepVO.getScriptSource()));
        scriptStep.setScriptId(scriptStepVO.getScriptId());
        scriptStep.setScriptVersionId(scriptStepVO.getScriptVersionId());
        if (StringUtils.isNotBlank(scriptStepVO.getContent())) {
            scriptStep.setContent(new String(Base64.decodeBase64(scriptStepVO.getContent()), StandardCharsets.UTF_8));
        } else {
            scriptStep.setContent(null);
        }
        scriptStep.setLanguage(ScriptTypeEnum.valueOf(scriptStepVO.getScriptLanguage()));
        scriptStep.setScriptParam(scriptStepVO.getScriptParam());
        if (scriptStepVO.getTimeout() == null) {
            scriptStep.setTimeout(60L);
        } else {
            scriptStep.setTimeout(scriptStepVO.getTimeout());
        }
        scriptStep.setAccount(scriptStepVO.getAccount());
        scriptStep.setExecuteTarget(TaskTargetDTO.fromVO(scriptStepVO.getExecuteTarget()));
        scriptStep.setSecureParam(scriptStepVO.getSecureParam() == 1);
        scriptStep.setStatus(scriptStepVO.getStatus());
        scriptStep.setIgnoreError(scriptStepVO.getIgnoreError() == 1);
        return scriptStep;
    }

    public static EsbScriptStepV3DTO toEsbScriptInfoV3(TaskScriptStepDTO scriptStepInfo) {
        if (scriptStepInfo == null) {
            return null;
        }
        EsbScriptStepV3DTO esbScriptStep = new EsbScriptStepV3DTO();
        esbScriptStep.setType(scriptStepInfo.getScriptSource().getType());
        esbScriptStep.setScriptId(scriptStepInfo.getScriptId());
        esbScriptStep.setScriptVersionId(scriptStepInfo.getScriptVersionId());
        if (StringUtils.isNotBlank(scriptStepInfo.getContent())) {
            esbScriptStep.setContent(Base64Util.encodeContentToStr(scriptStepInfo.getContent()));
        }
        esbScriptStep.setLanguage(scriptStepInfo.getLanguage().getValue());
        if (StringUtils.isNotBlank(scriptStepInfo.getScriptParam())) {
            esbScriptStep.setScriptParam(Base64Util.encodeContentToStr(scriptStepInfo.getScriptParam()));
        }
        esbScriptStep.setScriptTimeout(scriptStepInfo.getTimeout());
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setId(scriptStepInfo.getAccount());
        esbScriptStep.setAccount(account);
        esbScriptStep.setServer(TaskTargetDTO.toEsbServerV3(scriptStepInfo.getExecuteTarget()));
        esbScriptStep.setSecureParam(scriptStepInfo.getSecureParam() ? 1 : 0);
        return esbScriptStep;
    }

    public static ServiceTaskScriptStepDTO toServiceScriptInfo(TaskScriptStepDTO scriptStepInfo) {
        if (scriptStepInfo == null) {
            return null;
        }
        ServiceTaskScriptStepDTO serviceScriptStep = new ServiceTaskScriptStepDTO();
        serviceScriptStep.setScriptId(scriptStepInfo.getScriptId());
        serviceScriptStep.setScriptVersionId(scriptStepInfo.getScriptVersionId());
        serviceScriptStep.setScriptStatus(scriptStepInfo.getStatus());
        serviceScriptStep.setScriptSource(scriptStepInfo.getScriptSource().getType());
        serviceScriptStep.setType(scriptStepInfo.getLanguage().getValue());
        serviceScriptStep.setContent(scriptStepInfo.getContent());
        serviceScriptStep.setScriptParam(scriptStepInfo.getScriptParam());
        serviceScriptStep.setScriptTimeout(scriptStepInfo.getTimeout());

        serviceScriptStep.setAccount(new ServiceAccountDTO());
        serviceScriptStep.getAccount().setId(scriptStepInfo.getAccount());

        serviceScriptStep.setExecuteTarget(scriptStepInfo.getExecuteTarget().toServiceTaskTargetDTO());
        serviceScriptStep.setSecureParam(scriptStepInfo.getSecureParam());
        serviceScriptStep.setIgnoreError(scriptStepInfo.getIgnoreError());
        return serviceScriptStep;
    }
}
