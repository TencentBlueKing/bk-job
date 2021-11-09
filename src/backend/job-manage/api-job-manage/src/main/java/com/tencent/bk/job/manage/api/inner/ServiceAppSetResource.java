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

package com.tencent.bk.job.manage.api.inner;

import com.tencent.bk.job.common.annotation.InternalAPI;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.request.ServiceAddAppSetRequest;
import com.tencent.bk.job.manage.model.inner.request.ServiceUpdateAppSetRequest;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/service/app-set")
@Api(tags = {"job-manage:service:App_Set_Management"})
@RestController
@InternalAPI
public interface ServiceAppSetResource {
    @GetMapping("/{appId}")
    InternalResponse<ServiceApplicationDTO> queryAppSetById(@PathVariable("appId") Long appId);

    @GetMapping("/list")
    InternalResponse<List<ServiceApplicationDTO>> listAppSet();


    @DeleteMapping("/{appId}")
    InternalResponse<Boolean> deleteAppSet(@PathVariable("appId") Long appId);

    @PutMapping
    InternalResponse<ServiceApplicationDTO> addAppSet(@RequestBody ServiceAddAppSetRequest request);

    @PutMapping("/{appId}/maintainers")
    InternalResponse<ServiceApplicationDTO> addMaintainers(@RequestBody ServiceUpdateAppSetRequest request);

    @DeleteMapping("/{appId}/maintainers")
    InternalResponse<ServiceApplicationDTO> deleteMaintainers(@RequestBody ServiceUpdateAppSetRequest request);

    @PutMapping("/{appId}/sub-app")
    InternalResponse<ServiceApplicationDTO> addSubAppToAppSet(@RequestBody ServiceUpdateAppSetRequest request);

    @DeleteMapping("/{appId}/sub-app")
    InternalResponse<ServiceApplicationDTO> deleteSubApp(@RequestBody ServiceUpdateAppSetRequest request);
}
