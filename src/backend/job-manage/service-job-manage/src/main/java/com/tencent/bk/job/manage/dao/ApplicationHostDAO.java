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

    List<ApplicationHostDTO> listHostInfoByIps(Long appId, List<String> ips);

    List<ApplicationHostDTO> listHostInfoByAppId(long appId);

    List<ApplicationHostDTO> listAllHostInfo(Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByNormalAppIds(Collection<Long> appIds, Long start, Long limit);

    List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds);

    List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                                          Collection<Long> cloudAreaIds,
                                                          List<String> searchContents, Integer agentStatus,
                                                          Long start, Long limit);

    Long countHostInfoBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                       Collection<Long> cloudAreaIds, List<String> searchContents, Integer agentStatus);

    List<ApplicationHostDTO> listHostInfo(Collection<Long> appIds, Collection<String> ips);

    List<ApplicationHostDTO> listHostInfoBySourceAndIps(long cloudAreaId, Set<String> ips);

    PageData<ApplicationHostDTO> listHostInfoByPage(ApplicationHostDTO applicationHostInfoCondition,
                                                    BaseSearchCondition baseSearchCondition);

    int insertAppHostWithoutTopo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    int insertAppHostInfo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    int batchInsertAppHostInfo(DSLContext dslContext, List<ApplicationHostDTO> applicationHostDTOList);

    boolean existAppHostInfoByHostId(DSLContext dslContext, ApplicationHostDTO applicationHostDTO);

    boolean existAppHostInfoByHostId(DSLContext dslContext, Long hostId);

    int updateAppHostInfoByHostId(DSLContext dslContext, Long appId, ApplicationHostDTO applicationHostDTO);

    int batchUpdateAppHostInfoByHostId(DSLContext dslContext, List<ApplicationHostDTO> applicationHostDTOList);

    int deleteAppHostInfoById(DSLContext dslContext, Long appId, Long appHostId);

    int batchDeleteAppHostInfoById(DSLContext dslContext, Long appId, List<Long> appHostIdList);

    int deleteAppHostInfoByAppId(DSLContext dslContext, long appId);

    int deleteAppHostInfoNotInApps(DSLContext dslContext, Set<Long> notInAppIds);

    boolean existsHost(DSLContext dslContext, long appId, String ip);

    ApplicationHostDTO getLatestHost(DSLContext dslContext, long appId, long cloudAreaId, String ip);

    long countHostsByAppIds(DSLContext dslContext, Collection<Long> appIds);

    long countAllHosts();

    long countHostsByOsType(String osType);

    long syncHostTopo(DSLContext dslContext, Long hostId);

    List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps);
}
