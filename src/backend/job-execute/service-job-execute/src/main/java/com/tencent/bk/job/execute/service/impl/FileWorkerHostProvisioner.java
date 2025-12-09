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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.DistributeFileSourceHostException;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.prepare.third.FileWorkerHostService;
import com.tencent.bk.job.execute.service.ThirdFileDistributeSourceHostProvisioner;
import lombok.extern.slf4j.Slf4j;

/**
 * 使用job-file-worker所在Node作为文件源文件的分发源
 * 用于获取job-file-worker信息
 */
@Slf4j
public class FileWorkerHostProvisioner implements ThirdFileDistributeSourceHostProvisioner {

    private final FileWorkerHostService fileWorkerHostService;

    public FileWorkerHostProvisioner(FileWorkerHostService fileWorkerHostService) {
        this.fileWorkerHostService = fileWorkerHostService;
    }

    @Override
    public HostDTO getThirdFileDistributeSourceHost(Long cloudId, String protocol, String ip) {
        log.debug("distribute third file from file worker host");
        HostDTO hostDTO = fileWorkerHostService.parseFileWorkerHostWithCache(cloudId, protocol, ip);
        if (hostDTO == null) {
            throw new DistributeFileSourceHostException("File worker source host not found.",
                ErrorCode.INTERNAL_ERROR);
        }
        return hostDTO;
    }
}
