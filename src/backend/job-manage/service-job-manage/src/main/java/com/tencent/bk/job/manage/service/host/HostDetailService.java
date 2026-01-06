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

package com.tencent.bk.job.manage.service.host;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 主机详情服务，查出的主机信息包含Agent状态，云区域名称，云厂商等详情信息
 */
public interface HostDetailService {

    /**
     * 查询主机详情
     *
     * @param tenantId         租户ID
     * @param appResourceScope 业务资源范围
     * @param hostIds          主机ID列表
     * @return 主机详情列表
     */
    List<ApplicationHostDTO> listHostDetails(String tenantId,
                                             AppResourceScope appResourceScope,
                                             Collection<Long> hostIds);

    /**
     * 为主机填充云区域、云厂商、系统类型名称等信息
     *
     * @param tenantId 租户ID
     * @param hostList 主机列表
     */
    void fillDetailForApplicationHosts(String tenantId, List<ApplicationHostDTO> hostList);

    /**
     * 为主机填充云区域、云厂商、系统类型名称等信息
     *
     * @param tenantHostMap Map<租户ID,主机列表>
     */
    void fillDetailForTenantHosts(Map<String, List<ApplicationHostDTO>> tenantHostMap);

    /**
     * 为主机填充云区域、云厂商、系统类型名称等信息
     *
     * @param tenantId 租户ID
     * @param hostList 主机列表
     */
    void fillDetailForHosts(String tenantId, List<HostDTO> hostList);

}
