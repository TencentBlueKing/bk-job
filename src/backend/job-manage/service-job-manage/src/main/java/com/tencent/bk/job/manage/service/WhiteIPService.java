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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.vo.CloudAreaInfoVO;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.dto.whiteip.CloudIPDTO;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import com.tencent.bk.job.manage.model.web.request.whiteip.WhiteIPRecordCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.whiteip.ActionScopeVO;
import com.tencent.bk.job.manage.model.web.vo.whiteip.WhiteIPRecordVO;

import java.util.Collection;
import java.util.List;

/**
 * IP白名单服务
 */
public interface WhiteIPService {

    PageData<WhiteIPRecordVO> listWhiteIPRecord(String username, String ipStr, String appIdStr, String appNameStr,
                                                String actionScopeIdStr, String creator, String lastModifyUser,
                                                Integer start, Integer pageSize, String orderField, Integer order);

    /**
     * 查找对业务生效的IP信息
     *
     * @param appId 业务Id
     * @return 对业务生效的IP信息
     */
    List<CloudIPDTO> listWhiteIP(Long appId, ActionScopeEnum actionScope);

    /**
     * 查找对业务生效的IP对应的主机信息
     *
     * @param appId Job业务Id
     * @return 对业务生效的白名单IP对应的主机
     */
    List<HostDTO> listAvailableWhiteIPHost(Long appId, ActionScopeEnum actionScope, Collection<Long> hostIds);

    Long saveWhiteIP(String username, WhiteIPRecordCreateUpdateReq createUpdateReq);

    WhiteIPRecordVO getWhiteIPDetailById(String username, Long id);

    List<CloudAreaInfoVO> listCloudAreas(String username);

    List<ActionScopeVO> listActionScope(String username);

    Long deleteWhiteIPById(String username, Long id);

    List<String> getWhiteIPActionScopes(Long appId, String ip, Long cloudAreaId);

    List<ServiceWhiteIPInfo> listWhiteIPInfos();
}
