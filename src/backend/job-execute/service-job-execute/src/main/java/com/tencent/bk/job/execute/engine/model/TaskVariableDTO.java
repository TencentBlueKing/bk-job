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

package com.tencent.bk.job.execute.engine.model;

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.execute.model.ServersDTO;
import lombok.Getter;
import lombok.Setter;

import java.util.StringJoiner;


/**
 * 作业全局变量
 */
@Getter
@Setter
public class TaskVariableDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 变量类型
     *
     * @see TaskVariableTypeEnum
     */
    private Integer type = 1;

    /**
     * 名称
     */
    private String name;

    /**
     * 变量值
     */
    private String value;

    private ServersDTO targetServers;


    /**
     * 是否赋值可变
     */
    private boolean changeable;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 作业实例ID
     */
    private Long taskInstanceId;

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", TaskVariableDTO.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("type=" + type)
            .add("name='" + name + "'")
            .add("targetServers=" + targetServers)
            .add("changeable=" + changeable)
            .add("required=" + required)
            .add("taskInstanceId=" + taskInstanceId);
        if (type != null && type == TaskVariableTypeEnum.CIPHER.getType()) {
            sj.add("value='******'");
        } else {
            sj.add("value='" + value + "'");
        }
        return sj.toString();
    }
}
