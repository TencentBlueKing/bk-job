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

package com.tencent.bk.job.manage.service.topo.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.vo.TargetNodeVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.topo.BizTopoService;
import com.tencent.bk.job.manage.service.topo.ScopeTopoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScopeTopoServiceImpl implements ScopeTopoService {

    private final ApplicationService applicationService;
    private final BizTopoService bizTopoService;

    @Autowired
    public ScopeTopoServiceImpl(ApplicationService applicationService,
                                BizTopoService bizTopoService) {
        this.applicationService = applicationService;
        this.bizTopoService = bizTopoService;
    }


    @Override
    public List<List<InstanceTopologyDTO>> queryNodePaths(AppResourceScope appResourceScope,
                                                          List<TargetNodeVO> targetNodeVOList) {
        ApplicationDTO appDTO = applicationService.getAppByScope(appResourceScope);
        if (appDTO.isBizSet() || appDTO.isTenantSet()) {
            // 业务集/租户集
            return Collections.emptyList();
        }
        return bizTopoService.queryBizNodePaths(
            appDTO.getBizIdIfBizApp(),
            targetNodeVOList.stream().map(it -> {
                InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
                instanceTopologyDTO.setObjectId(it.getObjectId());
                instanceTopologyDTO.setInstanceId(it.getInstanceId());
                return instanceTopologyDTO;
            }).collect(Collectors.toList()));
    }
}
