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

package com.tencent.bk.job.common.cc.model.bizset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * CMDB接口响应实体类，定义业务集字段
 */
@Setter
@Getter
@ToString
public class BizSetInfo {

    /**
     * ID
     */
    @JsonProperty("bk_biz_set_id")
    private Long id;

    /**
     * 名称
     */
    @JsonProperty("bk_biz_set_name")
    private String name;

    /**
     * 描述
     */
    @JsonProperty("bk_biz_set_desc")
    private String desc;

    /**
     * 运维人员
     */
    @JsonProperty("bk_biz_maintainer")
    private String maintainer;

    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private String createTime;

    /**
     * 最后修改时间
     */
    @JsonProperty("last_time")
    private String lastTime;

    /**
     * 运维部门ID
     */
    @JsonProperty("bk_supplier_account")
    private String supplierAccount;

    /**
     * 运维部门ID
     */
    @JsonProperty("bk_operate_dept_id")
    private Long operateDeptId;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 语言
     */
    private String language;

    /**
     * 业务范围
     */
    @JsonProperty("bk_scope")
    private BizSetScope scope;
}
