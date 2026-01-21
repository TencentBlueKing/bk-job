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

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 当前租户下的主机服务
 */
public interface CurrentTenantHostService {

    /**
     * 批量获取主机。如果在Job缓存的主机中不存在，那么从cmdb查询
     *
     * @param hosts 主机
     * @return 主机
     */
    List<ApplicationHostDTO> listHosts(Collection<HostDTO> hosts);

    /**
     * 根据主机批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param hostIds 主机ID列表
     * @return 主机 Map<hostId, host>
     */
    Map<Long, ApplicationHostDTO> listHostsByHostIds(Collection<Long> hostIds);

    /**
     * 根据主机批量获取主机。如果在同步的主机中不存在，那么从cmdb查询
     *
     * @param cloudIps 主机云区域+ip列表
     * @return 主机 Map<hostId, host>
     */
    Map<String, ApplicationHostDTO> listHostsByIps(Collection<String> cloudIps);
}
