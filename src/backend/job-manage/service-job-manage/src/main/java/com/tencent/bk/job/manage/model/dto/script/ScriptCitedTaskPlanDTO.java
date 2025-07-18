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

package com.tencent.bk.job.manage.model.dto.script;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.manage.model.web.vo.ScriptCitedTaskPlanVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@ApiModel("引用脚本的作业执行方案")
public class ScriptCitedTaskPlanDTO {
    @ApiModelProperty("业务ID")
    private Long appId;
    @ApiModelProperty("脚本版本号")
    private String scriptVersion;
    @ApiModelProperty("脚本状态")
    private Integer scriptStatus;
    @ApiModelProperty("脚本状态描述")
    private String scriptStatusDesc;
    @ApiModelProperty("作业执行方案对应的作业模板ID")
    private Long taskTemplateId;
    @ApiModelProperty("作业执行方案ID")
    private Long taskPlanId;
    @ApiModelProperty("作业执行方案名称")
    private String taskPlanName;

    public ScriptCitedTaskPlanVO toVO() {
        ScriptCitedTaskPlanVO scriptCitedTaskPlanVO = new ScriptCitedTaskPlanVO();
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        scriptCitedTaskPlanVO.setScopeType(resourceScope.getType().getValue());
        scriptCitedTaskPlanVO.setScopeId(resourceScope.getId());
        scriptCitedTaskPlanVO.setScriptVersion(scriptVersion);
        scriptCitedTaskPlanVO.setScriptStatus(scriptStatus);
        scriptCitedTaskPlanVO.setScriptStatusDesc(scriptStatusDesc);
        scriptCitedTaskPlanVO.setTaskTemplateId(taskTemplateId);
        scriptCitedTaskPlanVO.setTaskPlanId(taskPlanId);
        scriptCitedTaskPlanVO.setTaskPlanName(taskPlanName);
        return scriptCitedTaskPlanVO;
    }
}
