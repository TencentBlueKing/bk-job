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

package com.tencent.bk.job.manage.model.web.vo.task;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.check.*;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.json.LongTimestampDeserializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @since 19/11/2019 11:27
 */
@Slf4j
@Getter
@Setter
@ToString
@ApiModel("执行方案信息")
public class TaskPlanVO {

    /**
     * 执行方案 ID
     */
    @ApiModelProperty(value = "执行方案 ID")
    private Long id;

    /**
     * 业务 ID
     */
    @ApiModelProperty(value = "业务 ID")
    private Long appId;

    /**
     * 模版 ID
     */
    @ApiModelProperty(value = "模版 ID")
    private Long templateId;

    /**
     * 执行方案名称
     */
    @ApiModelProperty(value = "执行方案名称")
    private String name;

    /**
     * 模板名称
     */
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 创建者
     */
    @ApiModelProperty(value = "创建者")
    private String creator;

    /**
     * 创建时间
     */
    @ApiModelProperty(value = "创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long createTime;

    /**
     * 最后修改人
     */
    @ApiModelProperty(value = "最后更新者")
    private String lastModifyUser;

    /**
     * 最后修改时间
     */
    @ApiModelProperty(value = "最后更新时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    @JsonDeserialize(using = LongTimestampDeserializer.class)
    private Long lastModifyTime;

    /**
     * 是否收藏
     */
    @ApiModelProperty(value = "是否收藏 0 - 未收藏 1 - 收藏")
    private Integer favored;

    /**
     * 是否需要更新
     */
    @ApiModelProperty(value = "是否需要更新")
    private Boolean needUpdate;

    /**
     * 是否关联定时任务
     */
    @ApiModelProperty(value = "是否关联定时任务")
    private Boolean hasCronJob;

    /**
     * 执行方案变量列表
     */
    @ApiModelProperty(value = "执行方案变量列表 详细信息独有")
    private List<TaskVariableVO> variableList;

    /**
     * 执行方案脚本列表
     */
    @ApiModelProperty(value = "执行方案脚本列表 详细信息独有")
    private List<TaskStepVO> stepList;

    /**
     * 执行方案版本
     */
    @ApiModelProperty(value = "执行方案版本")
    private String version;

    /**
     * 作业模版版本
     */
    @ApiModelProperty(value = "作业模版版本")
    private String templateVersion;

    /**
     * 关联定时任务数量
     */
    @ApiModelProperty(value = "关联定时任务数量")
    private Long cronJobCount;

    /**
     * 是否有查看权限
     */
    @ApiModelProperty(value = "是否有查看权限")
    private Boolean canView;

    /**
     * 是否有编辑权限
     */
    @ApiModelProperty(value = "是否有编辑权限")
    private Boolean canEdit;

    /**
     * 是否有删除权限
     */
    @ApiModelProperty(value = "是否有删除权限")
    private Boolean canDelete;

    public boolean validateForImport() {
        // 模板名称检查
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            this.setName(stringCheckHelper.checkAndGetResult(this.getName()));
        } catch (StringCheckException e) {
            log.warn("Template name is invalid:", e);
            return false;
        }
        boolean isCreate = false;
        if (CollectionUtils.isNotEmpty(stepList)) {
            for (TaskStepVO step : stepList) {
                if (!step.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Validate step failed!");
                    return false;
                }
                try {
                    StringCheckHelper stepCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                        new IlegalCharChecker(), new MaxLengthChecker(60));
                    step.setName(stepCheckHelper.checkAndGetResult(step.getName()));
                } catch (StringCheckException e) {
                    log.warn("Step name is invalid:", e);
                    return false;
                }
            }
        }
        if (CollectionUtils.isNotEmpty(variableList)) {
            Set<String> variableNameList = new HashSet<>();
            for (TaskVariableVO variable : variableList) {
                if (!variable.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Validate variable failed!");
                    return false;
                }
                if (variableNameList.contains(variable.getName())) {
                    JobContextUtil.addDebugMessage("Variable name duplicated!");
                    return false;
                }
                if (variable.getDelete() != null && variable.getDelete() != 1) {
                    variableNameList.add(variable.getName());
                }
            }
        }
        return true;
    }
}
