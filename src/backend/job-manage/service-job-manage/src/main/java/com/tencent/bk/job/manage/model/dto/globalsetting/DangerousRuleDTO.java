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

package com.tencent.bk.job.manage.model.dto.globalsetting;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.manage.common.consts.EnableStatusEnum;
import com.tencent.bk.job.manage.common.consts.RuleMatchHandleActionEnum;
import com.tencent.bk.job.manage.model.db.DangerousRuleDO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.DangerousRuleVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局设置项
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
public class DangerousRuleDTO {
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
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;
    /**
     * 更新人
     */
    private String lastModifier;
    /**
     * 更新时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    /**
     * 处理动作
     *
     * @see RuleMatchHandleActionEnum
     */
    private Integer action;
    /**
     * 规则启用状态
     *
     * @see EnableStatusEnum
     */
    private Integer status;

    public DangerousRuleDTO(Long id, String expression, String description, Integer priority, Integer scriptType,
                            String creator, Long createTime, String lastModifier, Long lastModifyTime,
                            Integer action, Integer status) {
        this.id = id;
        this.expression = expression;
        this.description = description;
        this.priority = priority;
        this.scriptType = scriptType;
        this.creator = creator;
        this.createTime = createTime;
        this.lastModifier = lastModifier;
        this.lastModifyTime = lastModifyTime;
        this.action = action;
        this.status = status;
    }

    public static DangerousRuleVO toVO(DangerousRuleDTO dangerousRule) {
        if (dangerousRule == null) {
            return null;
        }
        DangerousRuleVO dangerousRuleVO = new DangerousRuleVO();
        dangerousRuleVO.setId(dangerousRule.getId());
        dangerousRuleVO.setExpression(dangerousRule.getExpression());
        dangerousRuleVO.setScriptTypeList(decodeScriptType(dangerousRule.getScriptType()));
        dangerousRuleVO.setDescription(dangerousRule.getDescription());
        dangerousRuleVO.setOrder(dangerousRule.getPriority());
        dangerousRuleVO.setAction(dangerousRule.getAction());
        dangerousRuleVO.setStatus(dangerousRule.getStatus());
        return dangerousRuleVO;
    }

    public static List<Byte> decodeScriptType(int scriptType) {
        List<Byte> scriptTypeList = new ArrayList<>();
        if (scriptType > 0) {
            for (byte i = 0; i < 8; i++) {
                int current = 1 << i;
                if ((current & scriptType) == current) {
                    scriptTypeList.add((byte) (i + 1));
                }
            }
        }
        return scriptTypeList;
    }

    public static int encodeScriptType(List<Byte> scriptTypeList) {
        int scriptType = 0;
        if (CollectionUtils.isNotEmpty(scriptTypeList)) {
            for (Byte type : scriptTypeList) {
                if (type > 0 && type <= 8) {
                    scriptType |= 1 << (type - 1);
                }
            }
        }
        return scriptType;
    }

    public boolean isEnabled() {
        return this.status != null && this.status.equals(EnableStatusEnum.ENABLED.getValue());
    }

    /**
     * DTO -> DO
     *
     * @return 高危规则DO
     */
    public DangerousRuleDO toDangerousRuleDO() {
        DangerousRuleDO dangerousRuleDO = new DangerousRuleDO();
        dangerousRuleDO.setId(this.id);
        dangerousRuleDO.setExpression(this.expression);
        dangerousRuleDO.setPriority(this.priority);
        dangerousRuleDO.setScriptType(this.scriptType);
        dangerousRuleDO.setCreator(this.creator);
        dangerousRuleDO.setCreateTime(this.createTime);
        dangerousRuleDO.setLastModifier(this.lastModifier);
        dangerousRuleDO.setLastModifyTime(this.lastModifyTime);
        dangerousRuleDO.setAction(this.action);
        dangerousRuleDO.setStatus(this.status);
        dangerousRuleDO.setDescription(this.description);
        return dangerousRuleDO;
    }
}
