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

package com.tencent.bk.job.manage.model.inner;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Job业务属性
 */
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceApplicationAttrsDTO {

    /**
     * cmdb业务集的子业务ID列表
     */
    private List<Long> subBizIds;

    /**
     * cmdb业务集是否包含所有子业务
     */
    private Boolean matchAllBiz;

    /**
     * cmdb租户集是否包含所有租户
     */
    private Boolean matchAllTenant;

    public static ServiceApplicationAttrsDTO fromApplicationAttrsDO(ApplicationAttrsDO attrsDO) {
        if (attrsDO == null) {
            return null;
        }
        ServiceApplicationAttrsDTO dto = new ServiceApplicationAttrsDTO();
        dto.setMatchAllBiz(attrsDO.getMatchAllBiz());
        dto.setMatchAllTenant(attrsDO.getMatchAllTenant());
        dto.setSubBizIds(attrsDO.getSubBizIds());
        return dto;
    }
}
