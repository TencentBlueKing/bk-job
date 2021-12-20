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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceTagResource;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTagDTO;
import com.tencent.bk.job.manage.service.TagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class ServiceTagResourceImpl implements ServiceTagResource {
    private final TagService tagService;

    @Autowired
    public ServiceTagResourceImpl(TagService tagService) {
        this.tagService = tagService;
    }

    private ServiceTagDTO convert(TagDTO tagDTO) {
        ServiceTagDTO serviceTagDTO = new ServiceTagDTO();
        serviceTagDTO.setId(tagDTO.getId());
        serviceTagDTO.setName(tagDTO.getName());
        return serviceTagDTO;
    }

    @Override
    public InternalResponse<List<ServiceTagDTO>> listTags(Long appId) {
        List<TagDTO> tags = tagService.listTags(appId, null);
        return InternalResponse.buildSuccessResp(tags.parallelStream().map(this::convert).collect(Collectors.toList()));
    }

    @Override
    public InternalResponse<List<ServiceTagDTO>> listPublicTags() {
        List<TagDTO> tags = tagService.listTags(JobConstants.PUBLIC_APP_ID, null);
        return InternalResponse.buildSuccessResp(tags.parallelStream().map(this::convert).collect(Collectors.toList()));
    }
}
