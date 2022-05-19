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

package com.tencent.bk.job.manage.model.dto.script;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTemplateVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

@Getter
@Setter
@ToString
@ApiModel("脚本引用的作业模板")
public class ScriptCitedTaskTemplateDTO {
    @ApiModelProperty("业务ID")
    private Long appId;
    @ApiModelProperty("脚本版本ID")
    private Long scriptVersionId;
    @ApiModelProperty("脚本版本号")
    private String scriptVersion;
    @ApiModelProperty("脚本状态")
    private JobResourceStatusEnum scriptStatus;
    @ApiModelProperty("脚本状态描述")
    private String scriptStatusDesc;
    @ApiModelProperty("作业模板ID")
    private Long taskTemplateId;
    @ApiModelProperty("作业模板名称")
    private String taskTemplateName;

    public ScriptCitedTemplateVO toVO() {
        ScriptCitedTemplateVO scriptCitedTemplateVO = new ScriptCitedTemplateVO();
        if (appId != null && !appId.equals(PUBLIC_APP_ID)) {
            AppScopeMappingService appScopeMappingService =
                ApplicationContextRegister.getBean(AppScopeMappingService.class);
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
            scriptCitedTemplateVO.setScopeType(resourceScope.getType().getValue());
            scriptCitedTemplateVO.setScopeId(resourceScope.getId());
        }
        scriptCitedTemplateVO.setScriptVersion(scriptVersion);
        scriptCitedTemplateVO.setScriptStatus(scriptStatus.getValue());
        scriptCitedTemplateVO.setScriptStatusDesc(scriptStatusDesc);
        scriptCitedTemplateVO.setTaskTemplateId(taskTemplateId);
        scriptCitedTemplateVO.setTaskTemplateName(taskTemplateName);
        return scriptCitedTemplateVO;
    }
}
