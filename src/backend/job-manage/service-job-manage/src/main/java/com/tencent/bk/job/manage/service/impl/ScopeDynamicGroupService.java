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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.model.dto.DynamicGroupDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ScopeDynamicGroupService {

    private final ApplicationService applicationService;
    private final BizDynamicGroupService bizDynamicGroupService;

    @Autowired
    public ScopeDynamicGroupService(ApplicationService applicationService,
                                    BizDynamicGroupService bizDynamicGroupService) {
        this.applicationService = applicationService;
        this.bizDynamicGroupService = bizDynamicGroupService;
    }

    public List<DynamicGroupDTO> listOrderedDynamicGroup(AppResourceScope appResourceScope, Collection<String> ids) {
        List<DynamicGroupDTO> dynamicGroupDTOList = listDynamicGroup(appResourceScope, ids);
        dynamicGroupDTOList = defaultSort(dynamicGroupDTOList);
        return dynamicGroupDTOList;
    }

    public List<DynamicGroupDTO> listDynamicGroup(AppResourceScope appResourceScope, Collection<String> ids) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return Collections.emptyList();
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            return Collections.emptyList();
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            if (ids == null) {
                return bizDynamicGroupService.listDynamicGroup(bizId);
            }
            return bizDynamicGroupService.listDynamicGroup(bizId, ids);
        }
    }

    /**
     * 动态分组默认排序：按首字母字典序排序，24小时内有更新的置顶（按更新时间倒序排列）
     *
     * @param dynamicGroupDTOList 动态分组列表
     */
    private List<DynamicGroupDTO> defaultSort(List<DynamicGroupDTO> dynamicGroupDTOList) {
        List<DynamicGroupDTO> latestUpdatedDynamicGroupList = new ArrayList<>();
        List<DynamicGroupDTO> notUpdatedDynamicGroupList = new ArrayList<>();
        List<DynamicGroupDTO> orderedDynamicGroupList = new ArrayList<>();
        for (DynamicGroupDTO dynamicGroupDTO : dynamicGroupDTOList) {
            ZonedDateTime parsedLastTime = dynamicGroupDTO.getParsedLastTime();
            if (parsedLastTime != null && parsedLastTime.plusHours(24).isAfter(ZonedDateTime.now())) {
                latestUpdatedDynamicGroupList.add(dynamicGroupDTO);
            } else {
                notUpdatedDynamicGroupList.add(dynamicGroupDTO);
            }
        }
        // 最近24小时内更新过的动态分组按更新时间降序
        latestUpdatedDynamicGroupList.sort((dynamicGroup1, dynamicGroup2) ->
            dynamicGroup2.getParsedLastTime().compareTo(dynamicGroup1.getParsedLastTime())
        );
        // 最近24小时内未更新过的动态分组按首字母升序
        notUpdatedDynamicGroupList.sort(Comparator.comparing(DynamicGroupDTO::getName));
        // 最近24小时内更新过的动态分组置顶
        orderedDynamicGroupList.addAll(latestUpdatedDynamicGroupList);
        orderedDynamicGroupList.addAll(notUpdatedDynamicGroupList);
        return orderedDynamicGroupList;
    }
}
