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

package com.tencent.bk.job.common.cc.sdk;

import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcDynamicGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.model.req.input.GetHostByIpInput;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostBizRelationDTO;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * cmdb API Client - 业务相关
 */
public interface IBizCmdbClient {
    /**
     * 获取业务topo
     *
     * @param bizId cmdb业务ID
     */
    InstanceTopologyDTO getBizInstTopology(long bizId);

    /**
     * 获取业务完整拓扑树（含空闲机/故障机模块）
     *
     * @param bizId cmdb业务ID
     */
    InstanceTopologyDTO getBizInstCompleteTopology(long bizId);

    /**
     * 查询业务内置模块（空闲机/故障机等）
     *
     * @param bizId cmdb业务ID
     * @return 只有一个Set，根节点为Set的Topo树
     */
    InstanceTopologyDTO getBizInternalModule(long bizId);

    /**
     * 根据topo实例获取hosts
     *
     * @param bizId      cmdb业务ID
     * @param ccInstList topo节点列表
     * @return 主机
     */
    List<ApplicationHostDTO> getHosts(long bizId, List<CcInstanceDTO> ccInstList);

    /**
     * 根据topo实例获取hosts
     *
     * @param bizId      cmdb业务ID
     * @param ccInstList topo节点列表
     * @return 主机列表
     */
    List<ApplicationHostDTO> getHostsByTopology(long bizId, List<CcInstanceDTO> ccInstList);

    /**
     * 根据module获取hosts
     *
     * @param bizId        cmdb业务ID
     * @param moduleIdList 模块ID列表
     * @return 主机
     */
    List<ApplicationHostDTO> findHostByModule(long bizId, List<Long> moduleIdList);

    /**
     * 从CC获取所有业务信息
     *
     * @return 业务
     */
    List<ApplicationDTO> getAllBizApps();

    /**
     * 获取业务详情
     *
     * @param bizId cmdb业务ID
     * @return 业务
     */
    ApplicationDTO getBizAppById(long bizId);

    /**
     * 查询业务下的动态分组
     *
     * @param bizId cmdb业务ID
     * @return 动态分组
     */
    List<CcDynamicGroupDTO> getDynamicGroupList(long bizId);

    /**
     * 获取动态分组下面的主机
     *
     * @param bizId   cmdb业务ID
     * @param groupId 动态分组ID
     * @return 动态分组下的主机
     */
    List<CcGroupHostPropDTO> getDynamicGroupIp(long bizId, String groupId);

    /**
     * 获取云区域
     *
     * @return 云区域列表
     */
    List<CcCloudAreaInfoDTO> getCloudAreaList();

    /**
     * 根据IP查询主机
     *
     * @param input 查询条件
     * @return 主机
     */
    List<ApplicationHostDTO> getHostByIp(GetHostByIpInput input);

    /**
     * 根据云区域ID+IP获取主机信息
     *
     * @param cloudAreaId 云区域ID
     * @param ip          IP
     * @return 主机信息
     */
    ApplicationHostDTO getHostByIp(Long cloudAreaId, String ip);

    /**
     * 根据IP批量获取主机
     *
     * @param hostIps 云区域+IP列表
     * @return 主机列表
     */
    List<ApplicationHostDTO> listHostsByIps(List<IpDTO> hostIps);

    /**
     * 批量获取业务下的主机列表
     *
     * @param bizId  cmdb业务ID
     * @param ipList 主机IP列表
     * @return 主机
     */
    List<ApplicationHostDTO> listBizHosts(long bizId, Collection<IpDTO> ipList);

    List<HostBizRelationDTO> findHostBizRelations(String uin, List<Long> hostIdList);

    /**
     * 获取CMDB对象属性信息列表
     *
     * @param objId cmdb对象ID
     */
    List<CcObjAttributeDTO> getObjAttributeList(String objId);

    /**
     * 根据cmdb业务角色获取人员
     *
     * @param appId 业务ID
     * @param role  业务角色
     */
    Set<String> getAppUsersByRole(Long appId, String role);

    /**
     * 获取CMDB业务角色列表
     */
    List<AppRoleDTO> getAppRoleList();

    /**
     * 批量获取topo节点层级
     *
     * @param getTopoNodePathReq 请求
     * @return topo节点层级
     */
    List<InstanceTopologyDTO> getTopoInstancePath(GetTopoNodePathReq getTopoNodePathReq);


    /**
     * 监听CMDB主机事件
     */
    ResourceWatchResult<HostEventDetail> getHostEvents(Long startTime, String cursor);

    /**
     * 监听CMDB主机拓扑关系事件
     */
    ResourceWatchResult<HostRelationEventDetail> getHostRelationEvents(Long startTime, String cursor);

    /**
     * 监听CMDB业务事件
     */
    ResourceWatchResult<BizEventDetail> getAppEvents(Long startTime, String cursor);
}
