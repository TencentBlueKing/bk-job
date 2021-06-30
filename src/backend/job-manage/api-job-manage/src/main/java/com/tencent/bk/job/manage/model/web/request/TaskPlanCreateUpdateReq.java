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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.manage.model.web.vo.task.TaskVariableVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @since 15/10/2019 20:27
 */
@Data
@ApiModel("作业执行方案新增、更新请求报文")
public class TaskPlanCreateUpdateReq {
    /**
     * 执行方案 ID
     */
    @ApiModelProperty(value = "执行方案 ID，更新和删除时需要传入", required = true)
    private Long id;

    /**
     * 模版 ID
     */
    @ApiModelProperty(value = "模版 ID", required = true)
    private Long templateId;

    /**
     * 执行方案名称
     */
    @ApiModelProperty(value = "执行方案名称", required = true)
    private String name;

    /**
     * 启用的步骤列表
     */
    @ApiModelProperty(value = "启用的步骤列表，新增、修改时需要传入", required = true)
    private List<Long> enableSteps;

    /**
     * 执行方案变量
     */
    @ApiModelProperty(value = "执行方案变量，新增、修改时需要传入", required = true)
    private List<TaskVariableVO> variables;

}
