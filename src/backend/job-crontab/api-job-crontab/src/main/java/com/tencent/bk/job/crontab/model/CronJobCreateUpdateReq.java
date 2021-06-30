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

package com.tencent.bk.job.crontab.model;

import com.tencent.bk.job.common.model.vo.UserRoleInfoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 定时任务创建、更新请求
 *
 * @since 31/12/2019 16:33
 */
@Data
@ApiModel("定时任务创建、更新请求")
public class CronJobCreateUpdateReq {

    /**
     * 定时任务 ID
     * <p>
     * 创建时填 0
     */
    @ApiModelProperty(value = "任务 ID，创建填 0", required = true)
    private Long id;

    /**
     * 定时任务名称
     */
    @ApiModelProperty(value = "任务名称", required = true)
    private String name;

    /**
     * 关联的作业模版 ID
     * <p>
     * 更新时传空 或 0 表示不更新
     */
    @ApiModelProperty(value = "关联的作业模版 ID，更新时传空 或 0 表示不更新", required = true)
    private Long taskTemplateId;

    /**
     * 关联的执行方案 ID
     * <p>
     * 更新时传空 或 0 表示不更新
     */
    @ApiModelProperty(value = "关联的执行方案 ID，更新时传空 或 0 表示不更新", required = true)
    private Long taskPlanId;

    /**
     * 关联的脚本 ID
     * <p>
     * 保留字段，暂未使用
     */
    @ApiModelProperty(value = "关联的脚本 ID, 保留字段，暂未使用")
    private String scriptId;

    /**
     * 关联的脚本版本
     * <p>
     * 保留字段，暂未使用
     */
    @ApiModelProperty(value = "关联的脚本版本, 保留字段，暂未使用")
    private Long scriptVersionId;
    /**
     * 循环执行的定时表达式
     * <p>
     * 不可与 executeTime 同时为空
     */
    @ApiModelProperty(value = "循环执行的定时表达式, 不可与 executeTime 同时为空")
    private String cronExpression;

    /**
     * 单次执行的指定执行时间
     * <p>
     * 不可与 cronExpression 同时为空
     */
    @ApiModelProperty("单次执行的指定执行时间, 不可与 cronExpression 同时为空")
    private Long executeTime;

    /**
     * 变量信息
     */
    @ApiModelProperty("变量信息")
    private List<CronJobVariableVO> variableValue;

    /**
     * 定时任务启用状态
     */
    @ApiModelProperty(value = "是否启用", required = true)
    private Boolean enable;

    /**
     * 通知提前时间
     * <p>
     * 单位分钟
     */
    @ApiModelProperty("通知提前时间，单位分钟")
    private Long notifyOffset;

    /**
     * 通知接收人列表
     * <p>
     * 若 notifyOffset > 0，不可为空
     */
    @ApiModelProperty("通知接收人，若 notifyOffset > 0，不可为空")
    private UserRoleInfoVO notifyUser;

    /**
     * 通知渠道列表
     * <p>
     * 若 notifyOffset > 0，不可为空
     */
    @ApiModelProperty("通知渠道，若 notifyOffset > 0，不可为空")
    private List<String> notifyChannel;

    /**
     * 周期执行的结束时间
     */
    @ApiModelProperty("周期执行的结束时间")
    private Long endTime;

}
