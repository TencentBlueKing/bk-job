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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;

import java.util.List;

/**
 * 校验作业中引用的第三方文件源对当前业务是否可用
 */
public interface FileSourceReferenceService {

    /**
     * 校验文件步骤引用的第三方文件源（fileType = 3）对指定业务均可用，不可用则抛异常。
     * <p>
     * 返回批量查询结果供调用方复用（后续的 view_file_source 鉴权需要其中的 ownerAppId 与 alias），
     * 整个请求只发起一次跨服务查询。未引用第三方文件源时返回空列表且不发起查询。
     *
     * @param tenantId       租户ID
     * @param appId          当前业务ID
     * @param fileSourceList 文件步骤的源文件配置
     * @return 被引用的文件源的可用性查询结果
     */
    List<ServiceFileSourceAvailabilityDTO> validateReferencedFileSources(String tenantId,
                                                                         long appId,
                                                                         List<FileSourceDTO> fileSourceList);
}
