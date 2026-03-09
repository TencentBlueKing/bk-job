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

package com.tencent.bk.job.crontab.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.bk.job.common.model.dto.notify.StatusNotifyChannel;
import com.tencent.bk.job.common.model.vo.UserRoleInfoVO;
import com.tencent.bk.job.common.util.json.ToSecondDeserializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collections;
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
     */
    @ApiModelProperty(value = "定时任务 ID", hidden = true)
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
     * 单次执行的指定执行时间，单位毫秒
     * <p>
     * 不可与 cronExpression 同时为空
     */
    @ApiModelProperty("单次执行的指定执行时间, 不可与 cronExpression 同时为空，单位毫秒")
    @JsonDeserialize(using = ToSecondDeserializer.class)
    private Long executeTime;

    /**
     * 定时任务触发时间、结束时间是哪个时区下的时间
     */
    @ApiModelProperty("定时任务触发时间、结束时间是哪个时区下的时间")
    private String executeTimeZone;

    /**
     * 变量信息
     */
    @ApiModelProperty("变量信息")
    private List<CronJobVariableVO> variableValue;

    /**
     * 定时任务启用状态
     */
    @ApiModelProperty(value = "是否启用", required = true)
    private Boolean enable = false;

    /**
     * 通知提前时间
     * <p>
     * 单位分钟
     */
    @ApiModelProperty("通知提前时间，单位分钟")
    private Long notifyOffset = 0L;

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
    private List<String> notifyChannel = Collections.emptyList();

    /**
     * 通知方式（1-继承业务, 2-自定义）
     * @see com.tencent.bk.job.common.constant.CronJobNotifyType
     */
    @ApiModelProperty("通知方式（1:继承业务/2:自定义），默认继承业务")
    private Integer notifyType = 1;

    /**
     * 自定义通知，当notifyType为CUSTOM时生效
     * 通知对象
     */
    @ApiModelProperty("自定义通知，当notifyType为CUSTOM时生效，通知对象")
    private UserRoleInfoVO customNotifyUser;

    /**
     * 自定义通知，当notifyType为CUSTOM时生效
     * 执行状态与对应通知渠道列表
     */
    @ApiModelProperty("自定义通知，当notifyType为CUSTOM时生效，执行状态与对应通知渠道列表")
    private List<StatusNotifyChannel> customNotifyChannel = Collections.emptyList();

    /**
     * 周期执行的结束时间
     */
    @ApiModelProperty("周期执行的结束时间，单位毫秒")
    @JsonDeserialize(using = ToSecondDeserializer.class)
    private Long endTime = 0L;

}
