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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.model.vo.UserRoleInfoVO;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @since 31/12/2019 16:33
 */
@Data
@ApiModel("定时任务信息")
public class CronJobVO {

    /**
     * 定时任务 ID
     */
    @ApiModelProperty("任务 ID")
    private Long id;

    /**
     * 业务 ID
     */
    @ApiModelProperty("业务 ID")
    private Long appId;

    /**
     * 定时任务名称
     */
    @ApiModelProperty("任务名称")
    private String name;

    /**
     * 定时任务创建人
     */
    @ApiModelProperty("创建人")
    private String creator;

    /**
     * 定时任务创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;

    /**
     * 关联的作业模版 ID
     */
    @ApiModelProperty("关联的作业模版 ID")
    private Long taskTemplateId;

    /**
     * 关联的执行方案 ID
     */
    @ApiModelProperty("关联的执行方案 ID")
    private Long taskPlanId;

    /**
     * 关联的脚本 ID
     */
    @ApiModelProperty("关联的脚本 ID")
    private String scriptId;

    /**
     * 关联的脚本版本 ID
     */
    @ApiModelProperty("关联的脚本版本")
    private Long scriptVersionId;

    /**
     * 循环执行的定时表达式
     */
    @ApiModelProperty("循环执行的定时表达式")
    private String cronExpression;

    /**
     * 单次执行的指定执行时间戳
     */
    @ApiModelProperty("单次执行的指定执行时间戳")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long executeTime;

    /**
     * 变量信息
     */
    @ApiModelProperty("变量信息")
    private List<CronJobVariableVO> variableValue;

    /**
     * 上次执行结果
     */
    @ApiModelProperty("上次执行结果 0 - 未执行 1 - 成功 2 - 失败")
    private Integer lastExecuteStatus;

    /**
     * 上次执行错误码
     */
    @ApiModelProperty("上次执行错误码")
    private Long lastExecuteErrorCode;

    /**
     * 上次执行错误次数
     */
    @ApiModelProperty("上次执行错误次数")
    private Integer lastExecuteErrorCount;

    /**
     * 是否启用
     */
    @ApiModelProperty("是否启用")
    private Boolean enable;

    /**
     * 最后修改人
     */
    @ApiModelProperty("最后修改人")
    private String lastModifyUser;

    /**
     * 最后修改时间戳
     */
    @ApiModelProperty("最后修改时间戳")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long lastModifyTime;

    /**
     * 执行总次数
     */
    @ApiModelProperty("执行总次数")
    private Integer totalCount;

    /**
     * 执行失败次数
     */
    @ApiModelProperty("执行失败次数")
    private Integer failCount;

    /**
     * 最近 5 次执行失败时间戳
     */
    @ApiModelProperty("最近 5 次执行失败时间戳")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private List<Long> lastFailRecord;

    /**
     * 通知提前时间
     */
    @ApiModelProperty("通知提前时间")
    private Long notifyOffset;

    /**
     * 通知接收人
     */
    @ApiModelProperty("通知接收人")
    private UserRoleInfoVO notifyUser;

    /**
     * 通知渠道
     */
    @ApiModelProperty("通知渠道")
    private List<String> notifyChannel;

    /**
     * 周期执行的结束时间
     */
    @ApiModelProperty("周期执行的结束时间")
    private Long endTime;

    /**
     * 是否有管理权限
     */
    @ApiModelProperty("是否有管理权限")
    private Boolean canManage;
}
