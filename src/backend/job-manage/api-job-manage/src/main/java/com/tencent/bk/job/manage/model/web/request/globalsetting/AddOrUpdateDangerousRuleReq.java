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

package com.tencent.bk.job.manage.model.web.request.globalsetting;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.group.GroupSequenceProvider;

import com.tencent.bk.job.manage.validation.common.Update;
import com.tencent.bk.job.manage.validation.provider.DangerousRuleGroupSequenceProvider;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@GroupSequenceProvider(DangerousRuleGroupSequenceProvider.class)
@AllArgsConstructor
@NoArgsConstructor
@Data
@ApiModel("新增、更新高危语句规则")
public class AddOrUpdateDangerousRuleReq {
    @ApiModelProperty("高危语句规则Id，新增传-1")
    private Long id;
    @ApiModelProperty("表达式")
    @NotEmpty(message = "{validation.constraints.InvalidJobHighRiskGrammarRegex_empty.message}")
    @Size(max = 250, message = "{validation.constraints.InvalidJobHighRiskGrammarRegex_outOfRange.message}")
    private String expression;
    @ApiModelProperty("脚本类型：SHELL(1), BAT(2), PERL(3), PYTHON(4),POWERSHELL(5), SQL(6)")
    @NotEmpty(message = "{validation.constraints.ScriptTypeList_empty.message}")
    private List<Byte> scriptTypeList;
    @ApiModelProperty("描述")
    @Size(max = 1000, message = "{validation.constraints.InvalidRegularDescription_outOfRange.message}")
    private String description;
    @ApiModelProperty("处理动作,1:扫描,2:拦截")
    @NotNull(message = "{validation.constraints.InvalidHandleAction_empty.message}")
    @Range(min = 1L, max= 2L,
        message = "{validation.constraints.InvalidHandleAction_outOfRange.message}")
    private Integer action;
    @ApiModelProperty("规则启停状态，1:启用,2:停止")
    @NotNull(message = "{validation.constraints.InvalidRuleUseStatus_empty.message}", groups = {Update.class})
    @Range(min = 0L, max= 1L,
        message = "{validation.constraints.InvalidRuleUseStatus_outOfRange.message}", groups = {Update.class})
    private Integer status;
}
