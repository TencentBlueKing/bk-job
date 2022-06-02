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
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;

import java.util.Collection;
import java.util.List;

/**
 * 资源范围主机相关服务
 */
public interface ScopeHostService {

    /**
     * 根据 HostId 列表查询主机信息
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param hostIds          主机ID集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsByIds(String username,
                                                AppResourceScope appResourceScope,
                                                Collection<Long> hostIds);

    /**
     * 根据拓扑节点、模糊搜索关键字、agent状态分页查询查询资源范围下的主机
     *
     * @param username         用户名
     * @param appResourceScope 资源范围
     * @param appTopoNodeList  拓扑节点列表
     * @param searchContent    模糊搜索关键字（同时对主机IP/主机名/操作系统/云区域名称进行模糊搜索）
     * @param agentStatus      筛选条件：agentStatus：0为异常，1为正常
     * @param start            数据起始位置
     * @param pageSize         拉取数量
     * @return hostId列表
     */
    PageData<Long> listHostIdByBizTopologyNodes(String username,
                                                AppResourceScope appResourceScope,
                                                List<AppTopologyTreeNode> appTopoNodeList,
                                                String searchContent,
                                                Integer agentStatus,
                                                Long start,
                                                Long pageSize);
}
