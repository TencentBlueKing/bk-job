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

package com.tencent.bk.job.common.cc.model.result;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.cc.util.VersionCompatUtil;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 业务集事件详情
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class BizSetEventDetail {
    /**
     * 业务集ID
     */
    @JsonProperty("bk_biz_set_id")
    private Long bizSetId;
    /**
     * 业务集名称
     */
    @JsonProperty("bk_biz_set_name")
    private String bizSetName;
    /**
     * 业务运维
     */
    @JsonProperty("bk_biz_set_maintainer")
    private String maintainers;
    /**
     * 时区
     */
    @JsonProperty("time_zone")
    private String timezone;
    /**
     * 语言
     */
    @JsonProperty("language")
    private String language;

    public ApplicationDTO toApplicationDTO() {
        ApplicationDTO applicationDTO = new ApplicationDTO();
        ResourceScope resourceScope = new ResourceScope(ResourceScopeTypeEnum.BIZ_SET, String.valueOf(bizSetId));
        applicationDTO.setScope(resourceScope);
        applicationDTO.setAppType(AppTypeEnum.APP_SET);
        applicationDTO.setName(bizSetName);
        applicationDTO.setMaintainers(VersionCompatUtil.convertMaintainers(maintainers));
        applicationDTO.setTimeZone(timezone);
        applicationDTO.setLanguage(language);
        return applicationDTO;
    }
}
