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

package com.tencent.bk.job.analysis.model.web;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 后台任务分析结果VO
 */
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("后台任务分析结果")
@Data
public class AnalysisResultVO {
    @ApiModelProperty("Id")
    private Long id;
    @ApiModelProperty("分析任务代码【DefaultTipsProvider：默认文本提示；ForbiddenScriptFinder" +
        "：寻找在作业模板/执行方案中使用的禁用脚本；TaskPlanTargetChecker：寻找执行方案中是否有无效IP/Agent异常/Agent未安装的情况；TimerTaskFailRateWatcher" +
        "：寻找周期内定时任务失败率超过60%的定时任务；TimerTaskFailWatcher：寻找周期内执行失败的定时任务】")
    private String analysisTaskCode;
    @ApiModelProperty("优先级")
    private int priority;
    @ApiModelProperty("任务结果总体描述")
    private String description;
    @ApiModelProperty("具体子条目内容")
    private List<AnalysisResultItemVO> contents;
}
