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
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 4/11/2019 15:01
 */
public interface ApplicationHostDAO {

    ApplicationHostInfoDTO getHostById(Long hostId);

    Set<Long> listCloudAreasByAppId(long appId);

    List<ApplicationHostInfoDTO> listHostInfoByIps(Long appId, List<String> ips);

    List<ApplicationHostInfoDTO> listHostInfoByAppId(long appId);

    List<ApplicationHostInfoDTO> listAllHostInfo(Long start, Long limit);

    List<ApplicationHostInfoDTO> listHostInfoByNormalAppIds(Collection<Long> appIds, Long start, Long limit);

    List<ApplicationHostInfoDTO> listHostInfoByHostIds(Collection<Long> hostIds);

    List<ApplicationHostInfoDTO> listHostInfoBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                                              Collection<Long> cloudAreaIds,
                                                              List<String> searchContents, Integer agentStatus,
                                                              Long start, Long limit);

    Long countHostInfoBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                       Collection<Long> cloudAreaIds, List<String> searchContents, Integer agentStatus);

    List<ApplicationHostInfoDTO> listHostInfo(Collection<Long> appIds, Collection<String> ips);

    List<ApplicationHostInfoDTO> listHostInfoByDisplayIp(long appId, String ip);

    List<ApplicationHostInfoDTO> listHostInfoBySourceAndIps(long cloudAreaId, Set<String> ips);

    List<ApplicationHostInfoDTO> listHostInfoByAppIdsAndSourceAndIps(long appId, long cloudAreaId, Set<String> ips);

    PageData<ApplicationHostInfoDTO> listHostInfoByPage(ApplicationHostInfoDTO applicationHostInfoCondition,
                                                        BaseSearchCondition baseSearchCondition);

    int insertAppHostWithoutTopo(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO);

    int insertAppHostInfo(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO);

    int batchInsertAppHostInfo(DSLContext dslContext, List<ApplicationHostInfoDTO> applicationHostInfoDTOList);

    boolean existAppHostInfoByHostId(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO);

    boolean existAppHostInfoByHostId(DSLContext dslContext, Long hostId);

    int updateAppHostInfoByHostId(DSLContext dslContext, Long appId, ApplicationHostInfoDTO applicationHostInfoDTO);

    int batchUpdateAppHostInfoByHostId(DSLContext dslContext, List<ApplicationHostInfoDTO> applicationHostInfoDTOList);

    int deleteAppHostInfoById(DSLContext dslContext, Long appId, Long appHostId);

    int batchDeleteAppHostInfoById(DSLContext dslContext, Long appId, List<Long> appHostIdList);

    int batchDeleteAppHostTopoById(DSLContext dslContext, List<Long> appHostIdList);

    int deleteAppHostInfoByAppId(DSLContext dslContext, long appId);

    int deleteAppHostInfoNotInApps(DSLContext dslContext, Set<Long> notInAppIds);

    boolean existsHost(DSLContext dslContext, long appId, String ip);

    ApplicationHostInfoDTO getLatestHost(DSLContext dslContext, long appId, long cloudAreaId, String ip);

    long countHostsByAppIds(DSLContext dslContext, Collection<Long> appIds);

    long countAllHosts();

    long countHostsByOsType(String osType);

    long syncHostTopo(DSLContext dslContext, Long hostId);
}
