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

package com.tencent.bk.job.manage.service.host;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;

import java.util.Collection;
import java.util.List;

/**
 * 考虑到IP白名单数据的资源范围主机相关服务
 */
public interface WhiteIpAwareScopeHostService {

    /**
     * 根据 HostId 列表查询主机信息
     *
     * @param appResourceScope 资源范围
     * @param actionScope      生效场景
     * @param hostIds          主机ID集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByHostId(AppResourceScope appResourceScope,
                                                                   ActionScopeEnum actionScope,
                                                                   Collection<Long> hostIds);

    /**
     * 根据 IP 列表查询主机信息
     *
     * @param appResourceScope 资源范围
     * @param actionScope      生效场景
     * @param ips              IP集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByIp(AppResourceScope appResourceScope,
                                                               ActionScopeEnum actionScope,
                                                               Collection<String> ips);

    /**
     * 根据 CloudIP 列表查询主机信息
     *
     * @param appResourceScope 资源范围
     * @param actionScope      生效场景
     * @param cloudIps         CloudIP集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByCloudIp(AppResourceScope appResourceScope,
                                                                    ActionScopeEnum actionScope,
                                                                    Collection<String> cloudIps);

    /**
     * 根据 ipv6 集合查询主机信息
     *
     * @param appResourceScope 资源范围
     * @param actionScope      生效场景
     * @param ipv6s            主机Ipv6集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByIpv6(AppResourceScope appResourceScope,
                                                                 ActionScopeEnum actionScope,
                                                                 Collection<String> ipv6s);

    /**
     * 根据 关键字 集合查询主机信息
     *
     * @param appResourceScope 资源范围
     * @param actionScope      生效场景
     * @param keys             关键字集合
     * @return 主机信息列表
     */
    List<ApplicationHostDTO> getScopeHostsIncludingWhiteIPByKey(AppResourceScope appResourceScope,
                                                                ActionScopeEnum actionScope,
                                                                Collection<String> keys);

}
