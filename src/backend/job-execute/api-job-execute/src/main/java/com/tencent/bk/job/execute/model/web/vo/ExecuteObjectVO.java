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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.common.model.vo.HostInfoVO;
import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 作业执行对象 VO
 */
@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("作业执行对象")
public class ExecuteObjectVO {

    /**
     * 执行对象类型
     *
     * @see ExecuteObjectTypeEnum
     */
    private ExecuteObjectTypeEnum type;

    /**
     * 执行对象资源实例 ID（比如 主机/容器在 cmdb 对应的资源ID)
     */
    private Long executeObjectResourceId;

    /**
     * 容器
     */
    private ContainerVO container;

    /**
     * 主机
     */
    private HostInfoVO host;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExecuteObjectTypeEnum fromExecuteObjectTypeValue(int type) {
        return ExecuteObjectTypeEnum.valOf(type);
    }

    public static String buildExecuteObjectId(Integer executeObjectType, Long executeObjectResoruceId) {
        return executeObjectType + ":" + executeObjectResoruceId;
    }
}
