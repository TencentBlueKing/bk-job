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

package com.tencent.bk.job.common.cc.model.tenantset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * CMDB接口响应实体类，定义租户集字段
 */
@Setter
@Getter
@ToString
public class TenantSetInfo {

    /**
     * ID
     */
    private Long id;

    /**
     * 名称
     */
    private String name;

    /**
     * 运维人员
     */
    private String maintainer;

    /**
     * 描述
     */
    private String description;

    /**
     * 资源（业务/业务集/租户集）类型，1表示内置资源
     */
    @JsonProperty("default")
    private Integer deFault = 0;

    /**
     * 租户范围
     */
    @JsonProperty("bk_scope")
    private TenantSetScope scope;

    /**
     * 创建时间
     */
    @JsonProperty("bk_created_at")
    private String createTime;

    /**
     * 创建人
     */
    @JsonProperty("bk_created_by")
    private String createUser;

    /**
     * 最后修改时间
     */
    @JsonProperty("bk_updated_at")
    private String updateTime;

    /**
     * 更新人
     */
    @JsonProperty("bk_updated_by")
    private String updateUser;

}
