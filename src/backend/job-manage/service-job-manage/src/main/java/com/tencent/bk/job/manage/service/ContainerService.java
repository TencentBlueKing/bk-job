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

package com.tencent.bk.job.manage.service;

import com.tencent.bk.job.common.cc.model.container.ContainerDetailDTO;
import com.tencent.bk.job.common.cc.model.container.KubeTopologyDTO;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.query.ContainerQuery;

import java.util.List;

/**
 * 容器服务
 */
public interface ContainerService {


    /**
     * 根据业务 ID 查询容器拓扑（缓存)
     *
     * @param bizId 业务 ID
     * @return 容器拓扑
     */
    KubeTopologyDTO getBizKubeCacheTopo(long bizId);

    /**
     * 根据容器拓扑获取container信息(分页）
     *
     * @param query 查询条件
     * @return 容器列表（分页）
     */
    PageData<ContainerDetailDTO> listPageKubeContainerByTopo(ContainerQuery query);

    /**
     * 根据容器拓扑获取container信息
     *
     * @param query 查询条件
     * @return 容器列表
     */
    List<ContainerDetailDTO> listKubeContainerByTopo(ContainerQuery query);

    /**
     * 根据容器 ID 批量获取容器详情
     *
     * @param bizId        业务 ID
     * @param containerIds 容器 ID 列表
     * @return 容器列表
     */
    List<ContainerDetailDTO> listKubeContainerByIds(Long bizId, List<Long> containerIds);

    /**
     * 根据容器 UId 批量获取容器详情
     *
     * @param bizId         业务 ID
     * @param containerUIds 容器 UId 列表
     * @return 容器列表
     */
    List<ContainerDetailDTO> listKubeContainerByUIds(Long bizId, List<String> containerUIds);

}
