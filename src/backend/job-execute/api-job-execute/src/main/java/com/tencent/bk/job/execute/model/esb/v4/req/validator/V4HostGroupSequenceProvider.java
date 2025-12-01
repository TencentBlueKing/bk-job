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

package com.tencent.bk.job.execute.model.esb.v4.req.validator;

import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import java.util.ArrayList;
import java.util.List;

public class V4HostGroupSequenceProvider implements DefaultGroupSequenceProvider<OpenApiV4HostDTO> {

    @Override
    public List<Class<?>> getValidationGroups(OpenApiV4HostDTO hostDTO) {
        List<Class<?>> groups = new ArrayList<>();
        groups.add(OpenApiV4HostDTO.class);
        if (hostDTO != null) {
            // 优先级：bk_host_id > (bk_cloud_id + ip)
            if (hostDTO.getBkHostId() != null) {
                groups.add(ValidationGroups.HostType.HostId.class);
            } else if (hostDTO.getIp() != null || hostDTO.getBkCloudId() != null) {
                groups.add(ValidationGroups.HostType.CloudIdIp.class);
            } else {
                groups.add(ValidationGroups.HostType.HostId.class);
            }
        }
        return groups;
    }
}

