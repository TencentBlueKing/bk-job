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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.cc.model.DynamicGroupHostPropDTO;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.PageUtil;
import com.tencent.bk.job.manage.service.host.HostDetailService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 查询业务动态分组下的主机信息
 */
@Slf4j
@Service
public class BizDynamicGroupHostService {

    private final BizCmdbClient bizCmdbClient;
    private final HostDetailService hostDetailService;

    @Autowired
    public BizDynamicGroupHostService(BizCmdbClient bizCmdbClient,
                                      HostDetailService hostDetailService) {
        this.bizCmdbClient = bizCmdbClient;
        this.hostDetailService = hostDetailService;
    }

    public PageData<ApplicationHostDTO> pageHostByDynamicGroups(AppResourceScope appResourceScope,
                                                                String idStr,
                                                                int start,
                                                                int pageSize) {
        if (StringUtils.isBlank(idStr)) {
            return new PageData<>(start, pageSize, 0L, Collections.emptyList());
        }
        idStr = idStr.trim();
        List<Long> hostIds = listHostIdsByDynamicGroups(appResourceScope, Arrays.asList(idStr.split(",")));
        // 排序
        hostIds.sort(Long::compareTo);
        List<ApplicationHostDTO> hostList = hostDetailService.listHostDetails(
            appResourceScope,
            hostIds
        );
        // CMDB动态分组接口不支持分页，因此对host在内存中进行分页
        return PageUtil.pageInMem(hostList, start, pageSize);
    }

    public List<ApplicationHostDTO> listHostByDynamicGroups(AppResourceScope appResourceScope, Collection<String> ids) {
        List<Long> hostIds = listHostIdsByDynamicGroups(appResourceScope, ids);
        // 展示信息需要包含主机详情完整字段
        return hostDetailService.listHostDetails(appResourceScope, hostIds);
    }

    public List<Long> listHostIdsByDynamicGroups(AppResourceScope appResourceScope, Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        // 并发拉取
        int threadNum = Math.min(ids.size(), 5);
        List<Long> hostIdList = ConcurrencyUtil.getResultWithThreads(ids, threadNum, dynamicGroupId ->
            listHostIdsByDynamicGroup(appResourceScope, dynamicGroupId)
        );
        // 去重
        hostIdList = new ArrayList<>(new HashSet<>(hostIdList));
        return hostIdList;
    }

    public List<ApplicationHostDTO> listHostByDynamicGroup(AppResourceScope appResourceScope, String id) {
        List<Long> hostIds = listHostIdsByDynamicGroup(appResourceScope, id);
        // 展示信息需要包含主机详情完整字段
        return hostDetailService.listHostDetails(appResourceScope, hostIds);
    }

    private List<Long> listHostIdsByDynamicGroup(AppResourceScope appResourceScope, String id) {
        // 动态分组当前只支持业务，不支持业务集
        if (appResourceScope.getType() != ResourceScopeTypeEnum.BIZ) {
            throw new NotImplementedException(ErrorCode.NOT_SUPPORT_FEATURE_FOR_BIZ_SET);
        }
        long bizId = Long.parseLong(appResourceScope.getId());
        List<DynamicGroupHostPropDTO> dynamicGroupHostList = bizCmdbClient.getDynamicGroupIp(bizId, id);
        if (CollectionUtils.isEmpty(dynamicGroupHostList)) {
            return Collections.emptyList();
        }
        return dynamicGroupHostList.stream()
            .filter(Objects::nonNull)
            .map(DynamicGroupHostPropDTO::getId)
            .collect(Collectors.toList());
    }
}
