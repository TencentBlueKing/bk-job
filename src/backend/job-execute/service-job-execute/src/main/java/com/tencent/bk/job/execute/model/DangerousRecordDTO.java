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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.execute.model.web.vo.DangerousRecordVO;
import com.tencent.bk.job.execute.model.web.vo.ScriptCheckResultItemVO;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Map;
import java.util.stream.Collectors;

@Data
public class DangerousRecordDTO {
    /**
     * ID
     */
    private Long id;
    /**
     * 规则ID
     */
    private Long ruleId;
    /**
     * 规则表达式
     */
    private String ruleExpression;
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 业务名称
     */
    private String appName;
    /**
     * 操作人
     */
    private String operator;
    /**
     * 脚本语言
     *
     * @see com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum
     */
    private Integer scriptLanguage;
    /**
     * 脚本内容
     */
    private String scriptContent;
    /**
     * 记录创建时间
     */
    private Long createTime;
    /**
     * 触发方式
     *
     * @see com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum
     */
    private Integer startupMode;
    /**
     * 调用应用方
     */
    private String client;
    /**
     * 高危脚本处理
     */
    private Integer action;
    /**
     * 脚本检查结果
     */
    private ScriptCheckResultDTO checkResult;
    /**
     * 扩展数据
     */
    private Map<String, String> extData;

    public DangerousRecordVO toDangerousRecordVO() {
        DangerousRecordVO recordVO = new DangerousRecordVO();
        recordVO.setId(id);
        recordVO.setRuleId(ruleId);
        recordVO.setRuleExpression(ruleExpression);
        recordVO.setStartupMode(startupMode);
        recordVO.setOperator(operator);
        recordVO.setAction(action);
        recordVO.setClient(client);
        recordVO.setScriptLanguage(scriptLanguage);
        recordVO.setScriptContent(Base64Util.encodeContentToStr(scriptContent));

        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        recordVO.setScopeType(resourceScope.getType().getValue());
        recordVO.setScopeId(resourceScope.getId());

        // TODO:发布后去除
        recordVO.setAppName(appName);
        recordVO.setScopeName(appName);
        recordVO.setCreateTime(createTime);
        if (checkResult != null && CollectionUtils.isNotEmpty(checkResult.getResults())) {
            recordVO.setCheckResultItems(checkResult.getResults().stream().map(scriptCheckItem -> {
                ScriptCheckResultItemVO checkResultItemVO = new ScriptCheckResultItemVO();
                checkResultItemVO.setLine(scriptCheckItem.getLine());
                checkResultItemVO.setLevel(scriptCheckItem.getLevel());
                checkResultItemVO.setDescription(scriptCheckItem.getDescription());
                return checkResultItemVO;
            }).collect(Collectors.toList()));
        }
        return recordVO;
    }
}
