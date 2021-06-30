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

package com.tencent.bk.job.manage.model.inner.task;

import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 16/10/2019 10:50
 */
@Data
@ApiModel("全局变量信息")
public class ServiceTaskVariableDTO {

    @ApiModelProperty(value = "变量 ID 新增时无需填写，删除时仅需填写 id 和 delete")
    private Long id;

    @ApiModelProperty(value = "变量名")
    private String name;

    @ApiModelProperty(value = "变量类型 1-字符串 2-命名空间 3-主机列表 4-密码")
    private Integer type;

    @ApiModelProperty(value = "默认值")
    private String defaultValue;

    @ApiModelProperty(value = "主机列表默认值")
    private TaskTargetVO defaultTargetValue;

    @ApiModelProperty(value = "变量描述")
    private String description;

    @ApiModelProperty(value = "赋值可变 0-不可变 1-可变")
    private Integer changeable;

    @ApiModelProperty(value = "必填 0-非必填 1-必填")
    private Integer required;

    @ApiModelProperty(value = "删除 0-不删除 1-删除，仅在删除时填写")
    private Integer delete;

    public boolean validate(boolean isCreate) {
        if (isCreate) {
            if (id != null && id > 0) {
                JobContextUtil.addDebugMessage("Create request has variable id!");
                return false;
            }
        }
        if (StringUtils.isBlank(name)) {
            return false;
        }
        if (type <= 0 || type > 4) {
            return false;
        }
        if (required == null || required > 1) {
            return false;
        }
        return true;
    }
}
