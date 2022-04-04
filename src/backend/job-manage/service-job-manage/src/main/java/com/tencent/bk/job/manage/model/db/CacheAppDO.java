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

package com.tencent.bk.job.manage.model.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.DeprecatedAppLogic;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Redis 缓存业务DO
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CacheAppDO {
    /**
     * 业务ID
     */
    private Long id;

    /**
     * 资源范围类型
     */
    private String scopeType;

    /**
     * 资源范围ID
     */
    private String scopeId;

    /**
     * 业务名称
     */
    private String name;

    /**
     * 业务类型
     */
    @DeprecatedAppLogic
    private Integer appType;

    /**
     * 子业务
     */
    @DeprecatedAppLogic
    private List<Long> subBizIds;

    /**
     * 业务运维
     */
    @DeprecatedAppLogic
    private String maintainers;

    /**
     * 临时字段-业务初始部门ID
     */
    @DeprecatedAppLogic
    private Long operateDeptId;

    public static CacheAppDO fromApplicationDTO(ApplicationDTO application) {
        CacheAppDO cacheAppDO = new CacheAppDO();
        cacheAppDO.setId(application.getId());
        cacheAppDO.setScopeType(application.getScope().getType().getValue());
        cacheAppDO.setScopeId(application.getScope().getId());
        cacheAppDO.setName(application.getName());
        cacheAppDO.setAppType(application.getAppType().getValue());
        cacheAppDO.setMaintainers(application.getMaintainers());
        cacheAppDO.setSubBizIds(application.getSubBizIds());
        cacheAppDO.setOperateDeptId(application.getOperateDeptId());
        return cacheAppDO;
    }

    public static ApplicationDTO toApplicationDTO(CacheAppDO cacheAppDO) {
        ApplicationDTO application = new ApplicationDTO();
        application.setId(cacheAppDO.getId());
        application.setScope(new ResourceScope(cacheAppDO.getScopeType(), cacheAppDO.getScopeId()));
        application.setName(cacheAppDO.getName());
        application.setAppType(AppTypeEnum.valueOf(cacheAppDO.getAppType()));
        application.setMaintainers(cacheAppDO.getMaintainers());
        application.setSubBizIds(cacheAppDO.getSubBizIds());
        application.setOperateDeptId(cacheAppDO.getOperateDeptId());
        return application;
    }
}
