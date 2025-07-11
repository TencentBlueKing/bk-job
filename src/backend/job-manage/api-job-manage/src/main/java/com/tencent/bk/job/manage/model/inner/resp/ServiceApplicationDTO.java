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

package com.tencent.bk.job.manage.model.inner.resp;

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationAttrsDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 业务
 */
@Data
@ApiModel("业务")
public class ServiceApplicationDTO {


    @ApiModelProperty("业务ID")
    private Long id;

    /**
     * 资源范围类型
     */
    private String scopeType;
    /**
     * 资源范围ID,比如cmdb业务ID、cmdb业务集ID
     */
    private String scopeId;

    /**
     * 业务名称
     */
    @ApiModelProperty("业务名称")
    private String name;

    /**
     * 运维
     */
    private String maintainers;

    @ApiModelProperty("开发商")
    private String owner;

    /**
     * 时区
     */
    private String timeZone;

    /**
     * 语言
     */
    private String language;

    /**
     * 业务属性
     */
    private ServiceApplicationAttrsDTO attrs;

    public boolean isBiz() {
        return ResourceScopeTypeEnum.BIZ.getValue().equals(scopeType);
    }
}
