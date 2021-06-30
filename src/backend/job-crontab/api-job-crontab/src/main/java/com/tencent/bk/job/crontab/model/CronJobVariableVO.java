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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("全局变量VO")
public class CronJobVariableVO {
    /**
     * 全局变量 id
     */
    @ApiModelProperty(value = "ID", required = true)
    private Long id;

    /**
     * 全局变量类型
     * <p>
     * 1-字符串 2-命名空间 3-主机列表 4-密码
     *
     * @see TaskVariableTypeEnum
     */
    @ApiModelProperty(value = "变量类型 1-字符串 2-命名空间 3-主机列表 4-密码", required = true)
    private Integer type;

    /**
     * 全局变量名称
     */
    @ApiModelProperty(value = "变量名称", required = true)
    private String name;

    /**
     * 字符型变量的值
     * <p>
     * 命名空间，字符串，密码从该字段获取值，type 为 1 2 4 时必填
     */
    @ApiModelProperty("字符变量值，命名空间，字符串，密码从该字段获取值，type 为 1 2 4 时必填")
    private String value;

    /**
     * 主机型变量的值
     * <p>
     * 当变量类型为主机列表时，从该字段取值，type 为 3 时必填
     */
    @ApiModelProperty("主机列表值，当变量类型为主机列表时，从该字段取值，type 为 3 时必填")
    private TaskTargetVO targetValue;
}
