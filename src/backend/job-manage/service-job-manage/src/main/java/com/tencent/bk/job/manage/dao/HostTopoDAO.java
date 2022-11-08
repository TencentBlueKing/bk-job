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

import com.tencent.bk.job.manage.model.dto.HostTopoDTO;

import java.util.Collection;
import java.util.List;

public interface HostTopoDAO {
    void insertHostTopo(HostTopoDTO hostTopoDTO);

    int batchInsertHostTopo(List<HostTopoDTO> hostTopoDTOList);

    void deleteHostTopoByHostId(Long appId, Long hostId);

    void deleteHostTopo(Long hostId, Long appId, Long setId, Long moduleId);

    int batchDeleteHostTopo(List<Long> hostIdList);

    int batchDeleteHostTopo(Long bizId, List<Long> hostIdList);

    int countHostTopo(Long bizId, Long hostId);

    List<HostTopoDTO> listHostTopoByHostId(Long hostId);

    List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds);

    List<HostTopoDTO> listHostTopoByModuleIds(Collection<Long> moduleIds, Long start, Long limit);

    /**
     * 根据CMDB业务IDs查询下属主机ID列表
     *
     * @param bizIds 业务ID集合
     * @return 主机ID列表
     */
    List<Long> listHostIdByBizIds(Collection<Long> bizIds);

    /**
     * 根据CMDB业务ID与主机ID集合查询下属主机ID列表
     *
     * @param bizIds  业务ID集合
     * @param hostIds 主机ID集合
     * @return 主机ID列表
     */
    List<Long> listHostIdByBizAndHostIds(Collection<Long> bizIds, Collection<Long> hostIds);
}
