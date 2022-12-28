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

package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.esb.validate.EsbAppScopeReqGroupSequenceProvider;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.validation.CheckEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.group.GroupSequenceProvider;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MAX;
import static com.tencent.bk.job.common.constant.JobConstants.JOB_BUILD_IN_BIZ_SET_ID_MIN;

@Setter
@Getter
@GroupSequenceProvider(EsbAppScopeReqGroupSequenceProvider.class)
public class EsbAppScopeReq extends EsbJobReq {
    /**
     * 兼容字段,表示cmdb 业务/业务集ID
     */
    @CompatibleImplementation(explain = "兼容字段,表示业务ID或者业务集ID", version = "3.6.x")
    @JsonProperty("bk_biz_id")
    @Min(value = 1L, message = "{validation.constraints.InvalidBkBizId.message}", groups = UseBkBizIdParam.class)
    private Long bizId;

    /**
     * 资源范围类型
     */
    @JsonProperty("bk_scope_type")
    @CheckEnum(enumClass = ResourceScopeTypeEnum.class, enumMethod = "isValid",
        message = "{validation.constraints.InvalidBkScopeType.message}", groups = UseScopeParam.class)
    private String scopeType;

    /**
     * 资源范围ID
     */
    @JsonProperty("bk_scope_id")
    @NotBlank(message = "{validation.constraints.InvalidBkScopeId.message}", groups = UseScopeParam.class)
    private String scopeId;

    /**
     * Job 业务ID
     */
    @JsonIgnore
    private Long appId;

    public interface UseBkBizIdParam {
    }

    public interface UseScopeParam {
    }

    /**
     * 补全ESB 请求中的appId/scopeType/scopeId
     *
     * @param appScopeMappingService 业务与资源范围映射公共接口
     */
    public void fillAppResourceScope(AppScopeMappingService appScopeMappingService) {
        boolean isExistScopeParam = StringUtils.isNotEmpty(this.scopeType) ||
            StringUtils.isNotEmpty(this.scopeId);
        boolean isExistBkBizIdParam = this.bizId != null;
        if (isExistScopeParam) {
            this.appId = appScopeMappingService.getAppIdByScope(this.scopeType, this.scopeId);
        } else if (isExistBkBizIdParam) {
            // [8000000,9999999]是迁移业务集之前约定的业务集ID范围。为了兼容老的API调用方，在这个范围内的bizId解析为业务集
            this.scopeId = String.valueOf(this.bizId);
            if (this.bizId >= JOB_BUILD_IN_BIZ_SET_ID_MIN && this.bizId <= JOB_BUILD_IN_BIZ_SET_ID_MAX) {
                this.appId = appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(),
                    String.valueOf(this.bizId));
                this.scopeType = ResourceScopeTypeEnum.BIZ_SET.getValue();
            } else {
                this.appId = appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ.getValue(),
                    String.valueOf(this.bizId));
                this.scopeType = ResourceScopeTypeEnum.BIZ.getValue();
            }
        }
    }

    @JsonIgnore
    public AppResourceScope getAppResourceScope() {
        return new AppResourceScope(this.scopeType, this.scopeId, this.appId);
    }
}
