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
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;

import java.util.Collection;
import java.util.List;

/**
 * 使用从JobContext中获取的当前租户ID，自动添加到各类查询条件或新增字段中
 * 用于用户触发操作的逻辑（Web/ESB接口等）
 */
public interface CurrentTenantHostDAO {

    // 查询类操作
    List<ApplicationHostDTO> listHostInfoByIps(Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByCloudIps(Collection<String> cloudIps);

    List<ApplicationHostDTO> listHostInfoByBizId(long bizId);

    List<Long> listHostIdsByHostIds(Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s);

    List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames);

    List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                          Collection<Long> moduleIds,
                                                          Collection<Long> cloudAreaIds,
                                                          List<String> searchContents,
                                                          Integer agentStatus,
                                                          Long start,
                                                          Long limit);

    List<Long> getHostIdListBySearchContents(Collection<Long> appIds,
                                             Collection<Long> moduleIds,
                                             Collection<Long> cloudAreaIds,
                                             List<String> searchContents,
                                             Integer agentAlive,
                                             Long start,
                                             Long limit);

    Long countHostInfoBySearchContents(Collection<Long> bizIds,
                                       Collection<Long> moduleIds,
                                       Collection<Long> cloudAreaIds,
                                       List<String> searchContents,
                                       Integer agentStatus);

    List<ApplicationHostDTO> listHostInfoByMultiKeys(Collection<Long> bizIds,
                                                     Collection<Long> moduleIds,
                                                     Collection<Long> cloudAreaIds,
                                                     Collection<String> ipKeys,
                                                     Collection<String> ipv6Keys,
                                                     Collection<String> hostNameKeys,
                                                     Collection<String> osNameKeys,
                                                     Integer agentAlive,
                                                     Long start,
                                                     Long limit);

    List<Long> getHostIdListByMultiKeys(Collection<Long> bizIds,
                                        Collection<Long> moduleIds,
                                        Collection<Long> cloudAreaIds,
                                        Collection<String> ipKeys,
                                        Collection<String> ipv6Keys,
                                        Collection<String> hostNameKeys,
                                        Collection<String> osNameKeys,
                                        Integer agentAlive,
                                        Long start,
                                        Long limit);

    Long countHostInfoByMultiKeys(Collection<Long> bizIds,
                                  Collection<Long> moduleIds,
                                  Collection<Long> cloudAreaIds,
                                  Collection<String> ipKeys,
                                  Collection<String> ipv6Keys,
                                  Collection<String> hostNameKeys,
                                  Collection<String> osNameKeys,
                                  Integer agentAlive);

    long countHostsByBizIds(Collection<Long> bizIds);

    long countAllHosts();

    List<ApplicationHostDTO> listHostInfoByBizAndIps(Collection<Long> bizIds, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizAndCloudIps(Collection<Long> bizIds, Collection<String> cloudIps);

    List<ApplicationHostDTO> listHostInfoByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s);

    List<ApplicationHostDTO> listHostInfoByBizAndHostNames(Collection<Long> bizIds, Collection<String> hostNames);

    /**
     * 根据业务id统计主机状态数量
     *
     * @param bizIds 业务id
     * @return 状态数量
     */
    List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds);
}
