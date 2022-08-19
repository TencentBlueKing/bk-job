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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * Job业务
 */
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Slf4j
public class ApplicationDTO {

    /**
     * 业务ID
     */
    private Long id;

    /**
     * 资源范围
     */
    private ResourceScope scope;

    /**
     * 业务名称
     */
    private String name;


    /**
     * 开发商账号
     */
    private String bkSupplierAccount;

    /**
     * 业务时区
     */
    private String timeZone;

    /**
     * 语言：1-中文，2-英文
     */
    private String language;

    /**
     * 业务是否已经被删除
     */
    private boolean isDeleted;

    /**
     * 业务属性
     */
    private ApplicationAttrsDO attrs;

    @JsonIgnore
    public boolean isBiz() {
        return scope != null && scope.getType() == ResourceScopeTypeEnum.BIZ;
    }

    /**
     * 是否是业务集
     */
    @JsonIgnore
    public boolean isBizSet() {
        return scope != null && scope.getType() == ResourceScopeTypeEnum.BIZ_SET;
    }

    /**
     * 是否是全业务
     */
    @JsonIgnore
    public boolean isAllBizSet() {
        return isBizSet()
            && attrs != null
            && attrs.getMatchAllBiz() != null
            && attrs.getMatchAllBiz();
    }

    /**
     * 返回对应的cmdb业务ID
     *
     * @return cmdb业务ID
     */
    @JsonIgnore
    public Long getBizIdIfBizApp() {
        return Long.valueOf(this.scope.getId());
    }

    public List<Long> getSubBizIds() {
        if (attrs != null) {
            return attrs.getSubBizIds();
        } else {
            return Collections.emptyList();
        }
    }

}
