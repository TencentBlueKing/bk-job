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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbJobReq;
import com.tencent.bk.job.common.validation.CheckEnum;
import com.tencent.bk.job.common.validation.ValidRegexPattern;
import com.tencent.bk.job.manage.api.common.constants.rule.HighRiskGrammarActionEnum;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建高危语句规则请求
 */
@Data
@ApiModel("创建高危语句规则请求报文")
@EqualsAndHashCode(callSuper = true)
public class EsbCreateDangerousRuleV3Req extends EsbJobReq {
    /**
     * 表达式
     */
    @NotEmpty(message = "{validation.constraints.InvalidJobHighRiskGrammarRegex_empty.message}")
    @Length(max = 250, message = "{validation.constraints.InvalidJobHighRiskGrammarRegex_outOfLength.message}")
    @ValidRegexPattern(message = "{validation.constraints.InvalidJobHighRiskGrammarRegex_wrongExpr.message}")
    private String expression;

    /**
     * 脚本类型
     */
    @JsonProperty("script_language_list")
    @NotEmpty(message = "{validation.constraints.ScriptTypeList_empty.message}")
    private List<Byte> scriptTypeList;

    /**
     * 规则描述
     */
    @Length(max = 1000, message = "{validation.constraints.InvalidHighRiskRegularDescription_outOfLength.message}")
    private String description;

    /**
     * 处理动作
     */
    @NotNull(message = "{validation.constraints.InvalidHighRiskGrammarHandleAction.message}")
    @CheckEnum(enumClass = HighRiskGrammarActionEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.InvalidHighRiskGrammarHandleAction.message}")
    private Integer action;
}
