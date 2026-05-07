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

package com.tencent.bk.job.manage.model.web.vo.globalsetting;

import com.tencent.bk.job.manage.model.esb.v3.response.EsbDangerousRuleV3DTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(description = "高危语句规则")
public class DangerousRuleVO {
    @Schema(description = "高危语句规则Id")
    private Long id;
    @Schema(description = "表达式")
    private String expression;
    @Schema(description = "脚本类型：SHELL(1), BAT(2), PERL(3), PYTHON(4),POWERSHELL(5), SQL(6)")
    private List<Byte> scriptTypeList;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "顺序：小的在上方")
    private Integer order;
    @Schema(description = "动作")
    private Integer action;
    @Schema(description = "启用状态,1:启用,2:停止")
    private Integer status;
    @Schema(description = "创建人")
    private String creator;
    @Schema(description = "创建时间，单位毫秒")
    private Long createTime;
    @Schema(description = "最近更新人")
    private String lastModifier;
    @Schema(description = "最近更新时间，单位毫秒")
    private Long lastModifyTime;

    public EsbDangerousRuleV3DTO toEsbDangerousRuleV3DTO() {
        EsbDangerousRuleV3DTO esbDangerousRuleV3DTO = new EsbDangerousRuleV3DTO();
        esbDangerousRuleV3DTO.setId(id);
        esbDangerousRuleV3DTO.setExpression(expression);
        esbDangerousRuleV3DTO.setScriptTypeList(scriptTypeList);
        esbDangerousRuleV3DTO.setDescription(description);
        esbDangerousRuleV3DTO.setAction(action);
        esbDangerousRuleV3DTO.setStatus(status);
        esbDangerousRuleV3DTO.setCreator(creator);
        esbDangerousRuleV3DTO.setCreateTime(createTime);
        esbDangerousRuleV3DTO.setLastModifyUser(lastModifier);
        esbDangerousRuleV3DTO.setLastModifyTime(lastModifyTime);
        return esbDangerousRuleV3DTO;
    }
}
