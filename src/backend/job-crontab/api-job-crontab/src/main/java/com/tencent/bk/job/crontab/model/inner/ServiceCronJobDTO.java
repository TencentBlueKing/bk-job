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

package com.tencent.bk.job.crontab.model.inner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ApiModel("定时任务信息")
public class ServiceCronJobDTO {

    @ApiModelProperty("任务 ID")
    private Long id;

    @ApiModelProperty("业务 ID")
    private Long appId;

    @ApiModelProperty("任务名称")
    private String name;

    @ApiModelProperty("创建人")
    private String creator;

    @ApiModelProperty("关联的执行方案 ID")
    private Long taskPlanId;

    @ApiModelProperty("关联的脚本 ID")
    private String scriptId;

    @ApiModelProperty("关联的脚本版本")
    private Long scriptVersionId;

    @ApiModelProperty("循环执行的定时表达式")
    private String cronExpression;

    @ApiModelProperty("单次执行的指定执行时间戳")
    private Long executeTime;

    @ApiModelProperty("变量信息")
    private List<ServiceCronJobVariableDTO> variableValue;

    @ApiModelProperty("上次执行结果 0 - 未执行 1 - 成功 2 - 失败")
    private Integer lastExecuteStatus;

    @ApiModelProperty("是否启用")
    private Boolean enable;

    @ApiModelProperty("最后修改人")
    private String lastModifyUser;

    @ApiModelProperty("最后修改时间戳")
    private Long lastModifyTime;
}
