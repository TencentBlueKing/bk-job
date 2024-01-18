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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.constant.ProfileEnum;
import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.discovery.model.ServiceInstanceInfoDTO;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.manage.api.esb.v3.EsbServiceInfoV3Resource;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbServiceVersionV3DTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
@Profile("!" + ProfileEnum.Constants.TEST)
public class EsbServiceInfoV3ResourceImpl implements EsbServiceInfoV3Resource {

    private final ServiceInfoProvider serviceInfoProvider;

    @Autowired
    public EsbServiceInfoV3ResourceImpl(ServiceInfoProvider serviceInfoProvider) {
        this.serviceInfoProvider = serviceInfoProvider;;
    }


    @Override
    public EsbResp<EsbServiceVersionV3DTO> getLatestServiceVersion(String username,
                                                                   String appCode) {
        return getLatestServiceVersionUsingPost(username, appCode);
    }

    @Override
    public EsbResp<EsbServiceVersionV3DTO> getLatestServiceVersionUsingPost(String username,
                                                                            String appCode) {
        List<ServiceInstanceInfoDTO> instanceInfoDTOList = serviceInfoProvider.listServiceInfo();
        ServiceInstanceInfoDTO latestVersionInstance = findLatestVersionInstance(instanceInfoDTOList);
        EsbServiceVersionV3DTO esbServiceVersionV3DTO = new EsbServiceVersionV3DTO();
        if (latestVersionInstance != null) {
            esbServiceVersionV3DTO.setVersion(latestVersionInstance.getVersion());
        }
        return EsbResp.buildSuccessResp(esbServiceVersionV3DTO);
    }

    private static ServiceInstanceInfoDTO findLatestVersionInstance(List<ServiceInstanceInfoDTO> instanceInfoDTOList) {
        if (instanceInfoDTOList == null || instanceInfoDTOList.isEmpty()) {
            return null;
        }

        ServiceInstanceInfoDTO latestInstance = instanceInfoDTOList.get(0);
        String latestVersion = latestInstance.getVersion();

        for (int i = 1; i < instanceInfoDTOList.size(); i++) {
            ServiceInstanceInfoDTO currentInstance = instanceInfoDTOList.get(i);
            String currentVersion = currentInstance.getVersion();
            if (CompareUtil.compareVersion(currentVersion, latestVersion) > 0) {
                latestInstance = currentInstance;
                latestVersion = currentVersion;
            }
        }
        return latestInstance;
    }
}
