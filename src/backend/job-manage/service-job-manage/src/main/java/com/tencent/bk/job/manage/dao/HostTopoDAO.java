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

import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;

public interface HostTopoDAO {
    int insertHostTopo(HostTopoDTO hostTopoDTO);

    int batchInsertHostTopo(List<HostTopoDTO> hostTopoDTOList);

    void deleteHostTopoByHostId(Long appId, Long hostId);

    int deleteHostTopoBeforeOrEqualLastTime(Long hostId, Long bizId, Long setId, Long moduleId, Long lastTime);

    int batchDeleteHostTopo(List<Long> hostIdList);

    int batchDeleteWithLastTime(List<HostTopoDTO> hostTopoList);

    int batchDeleteHostTopo(Long bizId, List<Long> hostIdList);

    int batchUpdateBeforeLastTime(List<HostTopoDTO> hostTopoList);

    int updateBeforeLastTime(HostTopoDTO hostTopo);

    List<HostTopoDTO> listHostTopoByHostId(Long hostId);

    List<HostTopoDTO> listHostTopoByHostIds(Collection<Long> hostId);

    List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds);

    List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds, Long start, Long limit);

    /**
     * 根据要排除的hostId查询其他拓扑
     *
     * @param excludeHostIds 要排除的hostId集合
     * @return 拓扑列表
     */
    List<HostTopoDTO> listHostTopoByExcludeHostIds(Collection<Long> excludeHostIds);

    /**
     * 根据CMDB业务ID与主机ID集合查询下属主机ID列表
     *
     * @param bizIds  业务ID集合
     * @param hostIds 主机ID集合
     * @return 主机ID列表
     */
    List<Long> listHostIdByBizAndHostIds(Collection<Long> bizIds, Collection<Long> hostIds);

    /**
     * 根据hostId查询所属模块Id
     *
     * @param hostId 主机ID
     * @return 模块ID列表
     */
    List<Long> listModuleIdByHostId(Long hostId);

    /**
     * 根据业务ID批量查询主机Id与模块Id信息
     *
     * @param bizId 业务ID
     * @return <主机ID,模块ID>列表
     */
    List<Pair<Long, Long>> listHostIdAndModuleIdByBizId(Long bizId);
}
