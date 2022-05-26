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

package com.tencent.bk.job.analysis.model.inner;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.analysis.model.web.PerAppStatisticVO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.json.PercentageFormatJsonSerializer;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@ApiModel("单个业务的统计信息")
@Data
public class PerAppStatisticDTO {

    /**
     * 业务ID
     */
    private Long appId;

    /**
     * 资源范围名称
     */
    private String scopeName;

    /**
     * 统计量数值
     */
    private Long value;

    /**
     * 占比
     */
    @JsonSerialize(using = PercentageFormatJsonSerializer.class)
    private Float ratio;

    public PerAppStatisticVO toPerAppStatisticVO() {
        PerAppStatisticVO perAppStatisticVO = new PerAppStatisticVO();
        // TODO:发布后去除
        perAppStatisticVO.setAppId(appId);
        perAppStatisticVO.setAppName(scopeName);
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
        perAppStatisticVO.setScopeType(resourceScope.getType().getValue());
        perAppStatisticVO.setScopeId(resourceScope.getId());
        perAppStatisticVO.setScopeName(scopeName);
        perAppStatisticVO.setValue(value);
        perAppStatisticVO.setRatio(ratio);
        return perAppStatisticVO;
    }
}
