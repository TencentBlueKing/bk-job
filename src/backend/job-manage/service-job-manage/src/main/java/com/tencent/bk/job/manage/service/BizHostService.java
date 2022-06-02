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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;

import java.util.Collection;
import java.util.List;

/**
 * 业务主机相关服务
 */
public interface BizHostService {

    /**
     * 根据 hostId 集合查询主机信息
     *
     * @param hostIds 主机 ID集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getHostsByHostIds(Collection<Long> hostIds);

    /**
     * 根据 bizId、hostId集合查询主机信息
     *
     * @param bizIds  业务 ID集合
     * @param hostIds 主机 ID集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getHostsByBizAndHostIds(Collection<Long> bizIds,
                                                     Collection<Long> hostIds);

    /**
     * 根据条件查询主机ID
     *
     * @param bizIds         业务ID集合
     * @param moduleIds      模块ID集合
     * @param cloudAreaIds   云区域ID集合
     * @param searchContents 搜索关键字列表
     * @param agentStatus    agent状态
     * @param start          数据起始位置
     * @param limit          查询数据条数
     * @return 主机列表
     */
    PageData<Long> pageListHostId(Collection<Long> bizIds,
                                  Collection<Long> moduleIds,
                                  Collection<Long> cloudAreaIds,
                                  List<String> searchContents,
                                  Integer agentStatus,
                                  Long start,
                                  Long limit);
}
