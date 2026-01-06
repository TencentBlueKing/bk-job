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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 指定租户ID的主机数据DAO
 */
public interface TenantHostDAO {

    List<ApplicationHostDTO> listHostInfoByHostIds(String tenantId, Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostsByCloudIps(String tenantId, Collection<String> cloudIps);

    List<ApplicationHostDTO> listHostInfoByCloudIpv6(String tenantId, Long cloudAreaId, String ipv6);

    Map<String, Integer> groupHostByOsType(String tenantId);

    /**
     * 从主机表中分页查询主机ID，数据按主机ID升序
     *
     * @param tenantId 租户ID
     * @param start    起始位置
     * @param limit    数据量
     * @return 主机ID列表
     */
    List<Long> listHostIds(String tenantId, Long start, Long limit);

    /**
     * 根据传入的主机ID批量删除主机
     *
     * @param hostIdList 要删除的主机ID列表
     * @return 删除的主机数量
     */
    int batchDeleteHostById(String tenantId, List<Long> hostIdList);
}
