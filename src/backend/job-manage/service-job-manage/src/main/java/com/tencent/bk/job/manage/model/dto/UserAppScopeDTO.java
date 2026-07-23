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

package com.tencent.bk.job.manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户视角下的 Job 业务/资源范围（内部模型，非 Web/OpenAPI 对外协议）
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAppScopeDTO {
    /**
     * Job 业务 ID
     */
    private Long appId;
    /**
     * 资源范围类型
     */
    private String scopeType;
    /**
     * 资源范围 ID
     */
    private String scopeId;
    /**
     * 是否为系统内置资源
     */
    private boolean builtIn;
    /**
     * 是否为全业务
     */
    private boolean allBizSet;
    /**
     * 名称
     */
    private String name;
    /**
     * 是否有权限
     */
    private Boolean hasPermission;
    /**
     * 时区
     */
    private String timeZone;
    /**
     * 是否收藏
     */
    private Boolean favor;
    /**
     * 收藏时间，Unix 时间戳，单位毫秒
     */
    private Long favorTime;
}
