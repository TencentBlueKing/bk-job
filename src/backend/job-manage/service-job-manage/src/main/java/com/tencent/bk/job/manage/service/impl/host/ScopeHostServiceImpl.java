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

package com.tencent.bk.job.manage.service.impl.host;

import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.BizHostService;
import com.tencent.bk.job.manage.service.ScopeHostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 资源范围下的主机服务
 */
@Slf4j
@Service
public class ScopeHostServiceImpl implements ScopeHostService {

    private final ApplicationService applicationService;
    private final BizHostService bizHostService;

    @Autowired
    public ScopeHostServiceImpl(ApplicationService applicationService,
                                BizHostService bizHostService) {
        this.applicationService = applicationService;
        this.bizHostService = bizHostService;
    }

    @Override
    public List<ApplicationHostDTO> getScopeHostsByIds(String username,
                                                       AppResourceScope appResourceScope,
                                                       Collection<Long> hostIds) {
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO.isAllBizSet()) {
            // 全业务
            return bizHostService.getHostsByHostIds(hostIds);
        } else if (applicationDTO.isBizSet()) {
            // 业务集
            List<Long> bizIds = applicationDTO.getSubBizIds();
            return bizHostService.getHostsByBizAndHostIds(bizIds, hostIds);
        } else {
            // 普通业务
            Long bizId = Long.parseLong(applicationDTO.getScope().getId());
            return bizHostService.getHostsByBizAndHostIds(Collections.singletonList(bizId), hostIds);
        }
    }
}
