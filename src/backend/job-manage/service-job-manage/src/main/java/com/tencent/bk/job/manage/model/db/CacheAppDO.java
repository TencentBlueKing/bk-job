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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Getter;
import lombok.Setter;

/**
 * Redis 缓存业务DO
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
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
     * 业务属性
     */
    private ApplicationAttrsDO attrs;

    public static CacheAppDO fromApplicationDTO(ApplicationDTO application) {
        if (application == null) {
            return null;
        }
        CacheAppDO cacheAppDO = new CacheAppDO();
        cacheAppDO.setId(application.getId());
        cacheAppDO.setScopeType(application.getScope().getType().getValue());
        cacheAppDO.setScopeId(application.getScope().getId());
        cacheAppDO.setName(application.getName());
        cacheAppDO.setAttrs(application.getAttrs());
        return cacheAppDO;
    }

    public static ApplicationDTO toApplicationDTO(CacheAppDO cacheAppDO) {
        if (cacheAppDO == null) {
            return null;
        }
        ApplicationDTO application = new ApplicationDTO();
        application.setId(cacheAppDO.getId());
        application.setScope(new ResourceScope(cacheAppDO.getScopeType(), cacheAppDO.getScopeId()));
        application.setName(cacheAppDO.getName());
        application.setAttrs(cacheAppDO.getAttrs());
        return application;
    }
}
