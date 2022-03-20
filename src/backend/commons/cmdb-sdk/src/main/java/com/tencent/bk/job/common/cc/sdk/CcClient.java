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
import com.tencent.bk.job.common.cc.model.CcGroupDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.CcObjAttributeDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.model.req.input.GetHostByIpInput;
import com.tencent.bk.job.common.cc.model.result.BizEventDetail;
import com.tencent.bk.job.common.cc.model.result.FindHostBizRelationsResult;
import com.tencent.bk.job.common.cc.model.result.HostEventDetail;
import com.tencent.bk.job.common.cc.model.result.HostRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ListBizHostsTopoResult;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 11/11/2019 12:06
 */
public interface CcClient {
    /**
     * 获取topo
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     * @throws ServiceException
     */
    InstanceTopologyDTO getBizInstTopology(long appId, String owner, String uin) throws ServiceException;

    /**
     * 获取业务完整拓扑树（含空闲机/故障机模块）
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     * @throws ServiceException
     */
    InstanceTopologyDTO getBizInstCompleteTopology(long appId, String owner, String uin) throws ServiceException;

    /**
     * 获取业务内部模块
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     * @throws ServiceException
     */
    InstanceTopologyDTO getBizInternalModule(long appId, String owner, String uin) throws ServiceException;

    /**
     * 根据topo实例获取hosts
     *
     * @param ccInstList topo节点列表
     * @return 主机
     */
    List<ApplicationHostDTO> getHosts(long appId, List<CcInstanceDTO> ccInstList);

    /**
     * 根据topo实例获取hosts
     *
     * @param ccInstList topo节点列表
     * @return 主机列表
     */
    List<ApplicationHostDTO> getHostsByTopology(long appId, List<CcInstanceDTO> ccInstList);

    /**
     * 根据module获取hosts
     *
     * @param uin
     * @param owner
     * @return
     */
    List<ApplicationHostDTO> findHostByModule(long appId, List<Long> moduleIdList, String uin, String owner);

    /**
     * 获取业务下的主机与拓扑
     */
    List<ListBizHostsTopoResult.HostInfo> listBizHostsTopo(long appId, String uin, String owner);

    /**
     * 从CC获取所有业务信息
     *
     * @return
     */
    List<ApplicationDTO> getAllBizApps();

    /**
     * 从CC获取所有业务集信息
     *
     * @return
     */
    List<ApplicationDTO> getAllBizSetApps();

    /**
     * 获取用户的业务
     *
     * @param uin
     * @param owner
     * @return
     * @throws ServiceException
     */
    List<ApplicationDTO> getAppByUser(String uin, String owner) throws ServiceException;

    /**
     * 获取业务详细信息
     *
     * @param appId
     * @param uin
     * @param owner
     * @return
     */
    ApplicationDTO getAppById(long appId, String owner, String uin);

    /**
     * 查询自定义分组
     *
     * @param appId
     * @param owner
     * @param uin
     * @return
     */
    List<CcGroupDTO> getCustomGroupList(long appId, String owner, String uin);

    /**
     * 获取自定义分组下面的IP
     *
     * @param appId
     * @param owner
     * @param uin
     * @param groupId
     * @return
     */
    List<CcGroupHostPropDTO> getCustomGroupIp(long appId, String owner, String uin, String groupId);

    List<CcCloudAreaInfoDTO> getCloudAreaList();

    List<ApplicationHostDTO> getHostByIp(GetHostByIpInput input);

    /**
     * 根据云区域ID+IP获取主机信息
     *
     * @param cloudAreaId 云区域ID
     * @param ip          IP
     * @return 主机信息
     */
    ApplicationHostDTO getHostByIp(Long cloudAreaId, String ip);

    List<ApplicationHostDTO> listAppHosts(long appId, Collection<IpDTO> ipList);

    List<FindHostBizRelationsResult> findHostBizRelations(String uin, List<Long> hostIdList);

    /**
     * 根据IP获取主机信息
     * 注意：拿不到app_id属性，统一设为-1L，需调其他接口获取
     *
     * @param uin
     * @param ipList
     * @return
     */
    List<ApplicationHostDTO> getHostByIpWithoutAppId(String uin, List<String> ipList);

    /**
     * 获取CMDB对象属性信息列表
     *
     * @param objId cmdb对象ID
     * @return
     * @throws ServiceException
     */
    List<CcObjAttributeDTO> getObjAttributeList(String objId) throws ServiceException;

    /**
     * 根据cmdb业务角色获取人员
     *
     * @param appId 业务ID
     * @param role  业务角色
     * @return
     * @throws ServiceException
     */
    Set<String> getAppUsersByRole(Long appId, String role) throws ServiceException;

    /**
     * 获取CMDB业务角色列表
     *
     * @return
     * @throws ServiceException
     */
    List<AppRoleDTO> getAppRoleList() throws ServiceException;

    /**
     * 批量获取topo节点层级
     *
     * @param getTopoNodePathReq 请求
     * @return topo节点层级
     */
    List<InstanceTopologyDTO> getTopoInstancePath(GetTopoNodePathReq getTopoNodePathReq);


    /**
     * 监听CMDB主机事件
     *
     * @return
     */
    ResourceWatchResult<HostEventDetail> getHostEvents(Long startTime, String cursor);

    /**
     * 监听CMDB主机拓扑关系事件
     *
     * @return
     */
    ResourceWatchResult<HostRelationEventDetail> getHostRelationEvents(Long startTime, String cursor);

    /**
     * 监听CMDB业务事件
     *
     * @return
     */
    ResourceWatchResult<BizEventDetail> getAppEvents(Long startTime, String cursor);
}
