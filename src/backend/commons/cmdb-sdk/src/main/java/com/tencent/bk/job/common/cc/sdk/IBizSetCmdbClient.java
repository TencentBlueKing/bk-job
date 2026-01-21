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

package com.tencent.bk.job.common.cc.sdk;

import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.result.BizSetEventDetail;
import com.tencent.bk.job.common.cc.model.result.BizSetRelationEventDetail;
import com.tencent.bk.job.common.cc.model.result.ResourceWatchResult;

import java.util.List;
import java.util.Set;

/**
 * CMDB业务集相关接口
 */
public interface IBizSetCmdbClient {

    /**
     * 查询业务集
     *
     * @param tenantId 租户ID
     * @param bizSetId 业务集ID
     * @return 业务集
     */
    BizSetInfo queryBizSet(String tenantId, Long bizSetId);

    /**
     * 从CC获取所有业务集信息
     *
     * @param tenantId 租户ID
     * @return 业务集列表
     */
    List<BizSetInfo> listAllBizSets(String tenantId);

    /**
     * 从CC获取业务集信息
     *
     * @param tenantId  租户ID
     * @param bizSetIds 业务集ID
     * @return 业务集列表
     */
    List<BizSetInfo> listBizSetByIds(String tenantId, List<Long> bizSetIds);

    /**
     * 根据游标获取业务集事件
     *
     * @param tenantId  租户ID
     * @param startTime 监听事件的起始时间
     * @param cursor    监听事件的游标
     * @return 事件
     */
    ResourceWatchResult<BizSetEventDetail> getBizSetEvents(String tenantId, Long startTime, String cursor);

    /**
     * 根据游标获取业务集与业务关系事件
     *
     * @param startTime 监听事件的起始时间
     * @param cursor    监听事件的游标
     * @return 事件
     */
    ResourceWatchResult<BizSetRelationEventDetail> getBizSetRelationEvents(String tenantId,
                                                                           Long startTime,
                                                                           String cursor);

    /**
     * 根据cmdb业务角色获取人员
     *
     * @param tenantId 租户ID
     * @param bizId    cmdb业务ID
     * @param role     业务角色
     */
    Set<String> listUsersByRole(String tenantId, Long bizId, String role);
}
