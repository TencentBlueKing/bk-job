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

import com.tencent.bk.job.common.gse.constants.AgentStatusEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 4/11/2019 15:01
 */
public interface ApplicationHostDAO {

    ApplicationHostDTO getHostById(Long hostId);

    List<ApplicationHostDTO> listHostInfoByIps(Long bizId, List<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizId(long bizId);

    List<ApplicationHostDTO> listAllHostInfo(Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByBizIds(Collection<Long> bizIds, Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds, Collection<Long> moduleIds,
                                                          Collection<Long> cloudAreaIds,
                                                          List<String> searchContents, Integer agentStatus,
                                                          Long start, Long limit);

    Long countHostInfoBySearchContents(Collection<Long> bizIds, Collection<Long> moduleIds,
                                       Collection<Long> cloudAreaIds, List<String> searchContents, Integer agentStatus);

    /**
     * 根据ID与Agent状态查询主机数量
     *
     * @param hostIds     主机Id集合
     * @param agentStatus Agent状态
     * @return 主机数量
     */
    Long countHostByIdAndStatus(Collection<Long> hostIds, AgentStatusEnum agentStatus);

    List<ApplicationHostDTO> listHostInfo(Collection<Long> bizIds, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoByBizAndCloudIPs(Collection<Long> bizIds, Collection<String> cloudIPs);

    List<ApplicationHostDTO> listHostInfoBySourceAndIps(long cloudAreaId, Set<String> ips);

    PageData<ApplicationHostDTO> listHostInfoByPage(ApplicationHostDTO applicationHostInfoCondition,
                                                    BaseSearchCondition baseSearchCondition);

    int insertHostWithoutTopo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    int insertOrUpdateHost(DSLContext dslContext, ApplicationHostDTO hostDTO);

    int batchInsertAppHostInfo(DSLContext dslContext, List<ApplicationHostDTO> applicationHostDTOList);

    boolean existAppHostInfoByHostId(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    boolean existAppHostInfoByHostId(DSLContext dslContext, Long hostId);

    int updateHostAttrsById(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    int updateBizHostInfoByHostId(DSLContext dslContext, Long bizId, ApplicationHostDTO applicationHostDTO);

    int updateBizHostInfoByHostId(DSLContext dslContext,
                                  Long bizId,
                                  ApplicationHostDTO applicationHostDTO,
                                  boolean updateTopo);

    int batchUpdateBizHostInfoByHostId(DSLContext dslContext, List<ApplicationHostDTO> applicationHostDTOList);


    int deleteBizHostInfoById(DSLContext dslContext, Long bizId, Long hostId);

    /**
     * 根据传入的业务ID与主机ID批量删除主机
     *
     * @param dslContext DB操作上下文
     * @param bizId      业务ID
     * @param hostIdList 要删除的主机ID列表
     * @return 删除的主机数量
     */
    int batchDeleteBizHostInfoById(DSLContext dslContext, Long bizId, List<Long> hostIdList);

    /**
     * 删除某个业务下的全部主机，用于业务被删除后清理主机
     *
     * @param dslContext DB操作上下文
     * @param bizId      业务ID
     * @return 删除的主机数量
     */
    int deleteBizHostInfoByBizId(DSLContext dslContext, long bizId);

    boolean existsHost(DSLContext dslContext, long bizId, String ip);

    ApplicationHostDTO getLatestHost(DSLContext dslContext, long bizId, long cloudAreaId, String ip);

    long countHostsByBizIds(DSLContext dslContext, Collection<Long> bizIds);

    long countAllHosts();

    long countHostsByOsType(String osType);

    int syncHostTopo(DSLContext dslContext, Long hostId);

    List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps);

    /**
     * 查询全部主机，主机对象只有主要属性
     *
     * @return 主机列表
     */
    List<HostSimpleDTO> listAllHostSimpleInfo();

    /**
     * 批量更新主机状态
     * @param status 主机状态
     * @param hostIdList 主机id列表
     * @return 成功更新的条数
     */
    int batchUpdateHostStatusByHostIds(int status, List<Long> hostIdList);

    /**
     * 根据业务id统计主机状态数量
     * @param bizIds 业务id
     * @return 状态数量
     */
    List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds);
}
