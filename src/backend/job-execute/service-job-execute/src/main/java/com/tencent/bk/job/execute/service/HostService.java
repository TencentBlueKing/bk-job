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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务器服务
 */
public interface HostService {
    /**
     * 获取从cmdb同步的主机列表。注意，使用该方法时，需要先判断ServiceResponse的状态！只有成功响应，里边的数据才是有效的；避免同步时候误删除
     *
     * @return 服务端响应
     */
    InternalResponse<List<ServiceHostInfoDTO>> listSyncHosts(long appId);

    /**
     * 获取主机的业务ID
     *
     * @param cloudAreaId 云区域ID
     * @param ip          ip
     * @return 业务ID
     */
    Long getCacheHostAppId(long cloudAreaId, String ip);

    /**
     * 获取主机信息
     *
     * @param cloudAreaId 云区域ID
     * @param ip          ip
     * @return 主机信息
     */
    ApplicationHostInfoDTO getHostPreferCache(long cloudAreaId, String ip);

    /**
     * 批量获取主机信息
     *
     * @param hosts 主机列表
     * @return 主机信息
     */
    Map<IpDTO, ApplicationHostInfoDTO> batchGetHostsPreferCache(List<IpDTO> hosts);

    /**
     * 获取不在业务列表下的主机
     *
     * @param appIdList 业务ID列表
     * @param hosts     主机列表
     * @return Pair<List < IpDTO>, List<IpDTO>>, pair中第一个是在其他业务下的host，第二个是不在cache中的host
     */
    Pair<List<IpDTO>, List<IpDTO>> checkHostsNotInAppByCache(List<Long> appIdList, List<IpDTO> hosts);

    /**
     * 从CMDB检查主机是否存在业务下,同时刷新主机缓存
     *
     * @param appId 业务ID
     * @param hosts 主机列表
     * @return 未被当前业务缓存的主机列表
     */
    List<IpDTO> checkHostsNotInAppByCmdb(long appId, Collection<IpDTO> hosts);

    /**
     * 查询主机在白名单中允许的操作
     *
     * @param appId 业务ID
     * @param host  主机
     * @return 允许的操作
     */
    List<String> getHostAllowedAction(long appId, IpDTO host);

    /**
     * 判断IP白名单是否配置该主机的对应操作
     *
     * @param appId  业务ID
     * @param host   主机
     * @param action 操作
     * @return 是否配置规则
     */
    boolean isMatchWhiteIpRule(long appId, IpDTO host, String action);

}
