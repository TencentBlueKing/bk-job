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

package com.tencent.bk.job.manage.dao;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @since 4/11/2019 15:01
 */
public interface ApplicationHostDAO {

    // 查询类操作
    boolean existAppHostInfoByHostId(Long hostId);

    ApplicationHostDTO getHostById(Long hostId);

    List<ApplicationHostDTO> listHostInfoByIps(Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByCloudIps(Collection<String> cloudIps);

    /**
     * 查询近期未更新的主机ID
     *
     * @param bizId              主机所属业务ID
     * @param minUpdateTimeMills 最小更新时间，查出最近更新时间在此时间之前的主机
     * @param maxUpdateTimeMills 最大更新时间，查出最近更新时间在此时间之前的主机
     * @return 主机ID列表
     */
    List<Long> listHostId(long bizId, long minUpdateTimeMills, long maxUpdateTimeMills);

    List<ApplicationHostDTO> listHostInfoByIps(Long bizId, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizId(long bizId);

    List<BasicHostDTO> listBasicHostInfo(Collection<Long> hostIds);

    List<ApplicationHostDTO> listAllHostInfo(Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByBizIds(Collection<Long> bizIds, Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s);

    List<ApplicationHostDTO> listHostInfoByCloudIpv6(Long cloudAreaId, String ipv6);

    List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames);

    List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                          Collection<Long> moduleIds,
                                                          Collection<Long> cloudAreaIds,
                                                          List<String> searchContents,
                                                          Integer agentStatus,
                                                          Long start,
                                                          Long limit);

    List<ApplicationHostDTO> listHostInfo(Collection<Long> bizIds, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizAndCloudIPs(Collection<Long> bizIds, Collection<String> cloudIPs);

    PageData<ApplicationHostDTO> listHostInfoByPage(ApplicationHostDTO applicationHostInfoCondition,
                                                    BaseSearchCondition baseSearchCondition);

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

    long countHostsByOsType(String osType);

    Map<String, Integer> groupHostByOsType();

    List<ApplicationHostDTO> listHostInfoByBizAndIps(Collection<Long> bizIds, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizAndCloudIps(Collection<Long> bizIds, Collection<String> cloudIps);

    List<ApplicationHostDTO> listHostInfoByBizAndIpv6s(Collection<Long> bizIds, Collection<String> ipv6s);

    List<ApplicationHostDTO> listHostInfoByBizAndHostNames(Collection<Long> bizIds, Collection<String> hostNames);

    /**
     * 查询全部主机，主机对象只有主要属性
     *
     * @return 主机列表
     */
    List<HostSimpleDTO> listAllHostSimpleInfo();

    /**
     * 查询全部主机，主机对象只有基础属性
     *
     * @return 主机列表
     */
    List<BasicHostDTO> listAllBasicHost();

    /**
     * 批量更新主机状态
     *
     * @param status     主机状态
     * @param hostIdList 主机id列表
     * @return 成功更新的条数
     */
    int batchUpdateHostStatusByHostIds(int status, List<Long> hostIdList);

    // 新增、更新类操作
    int insertHostWithoutTopo(ApplicationHostDTO applicationHostDTO);

    int batchInsertHost(List<ApplicationHostDTO> applicationHostDTOList);

    int updateHostAttrsByHostId(ApplicationHostDTO applicationHostDTO);

    int updateHostAttrsBeforeLastTime(ApplicationHostDTO applicationHostDTO);

    int batchUpdateHostsBeforeLastTime(List<ApplicationHostDTO> applicationHostDTOList);

    int syncHostTopo(Long hostId);


    // 删除类操作
    int deleteHostBeforeOrEqualLastTime(Long bizId, Long hostId, Long lastTime);

    /**
     * 根据传入的主机ID批量删除主机
     *
     * @param hostIdList 要删除的主机ID列表
     * @return 删除的主机数量
     */
    int batchDeleteHostById(List<Long> hostIdList);

    /**
     * 根据传入的业务ID与主机ID批量删除主机
     *
     * @param bizId      业务ID
     * @param hostIdList 要删除的主机ID列表
     * @return 删除的主机数量
     */
    int batchDeleteBizHostInfoById(Long bizId, List<Long> hostIdList);

    /**
     * 根据ip查询主机
     * 删除某个业务下的全部主机，用于业务被删除后清理主机
     * 根据cloudIp查询主机
     *
     * @param cloudIps 主机ip(云区域+ip)集合
     */
    List<ApplicationHostDTO> listHostsByCloudIps(Collection<String> cloudIps);

    /**
     * 删除某个业务下的全部主机，用于业务被删除后清理主机
     *
     * @param bizId 业务ID
     * @return 删除的主机数量
     */
    int deleteBizHostInfoByBizId(long bizId);

    /**
     * 根据主机基础信息进行批量删除
     *
     * @param basicHostList 主机基础信息列表
     * @return 成功删除的主机数量
     */
    int deleteByBasicHost(List<BasicHostDTO> basicHostList);

    /**
     * 根据业务id统计主机状态数量
     *
     * @param bizIds 业务id
     * @return 状态数量
     */
    List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds);
}
