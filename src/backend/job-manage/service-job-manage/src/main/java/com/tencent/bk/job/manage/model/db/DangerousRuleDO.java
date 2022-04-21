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

package com.tencent.bk.job.manage.model.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis 缓存规则DO
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DangerousRuleDO {
    /**
     * id
     */
    private Long id;
    /**
     * 表达式
     */
    private String expression;
    /**
     * 描述
     */
    private String description;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * 脚本类型
     */
    private Integer scriptType;
    /**
     * 创建人
     */
    private String creator;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 更新人
     */
    private String lastModifier;
    /**
     * 更新时间
     */
    private Long lastModifyTime;
    /**
     * 处理动作
     */
    private Integer action;
    /**
     * 规则启用状态
     */
    private Integer status;

    /**
     * DO -> DTO
     * @return 高危规则
     */
    public DangerousRuleDTO toDangerousRuleDTO() {
        DangerousRuleDTO dangerousRuleDTO = new DangerousRuleDTO();
        dangerousRuleDTO.setId(this.id);
        dangerousRuleDTO.setExpression(this.expression);
        dangerousRuleDTO.setPriority(this.priority);
        dangerousRuleDTO.setScriptType(this.scriptType);
        dangerousRuleDTO.setDescription(this.description);
        dangerousRuleDTO.setCreator(this.creator);
        dangerousRuleDTO.setCreateTime(this.createTime);
        dangerousRuleDTO.setLastModifier(this.lastModifier);
        dangerousRuleDTO.setLastModifyTime(this.lastModifyTime);
        dangerousRuleDTO.setAction(this.action);
        dangerousRuleDTO.setStatus(this.status);
        return dangerousRuleDTO;
    }
}
