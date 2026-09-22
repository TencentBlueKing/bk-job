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

package com.tencent.bk.job.service;

import com.tencent.bk.job.model.DeleteVersionBranchResp;
import com.tencent.bk.job.model.VersionBranchDTO;
import com.tencent.bk.job.model.VersionBranchReq;

import java.util.List;

/**
 * 版本分支信息管理服务
 */
public interface VersionBranchService {

    /**
     * 新增版本分支。重复新增拒绝，不执行 upsert。
     */
    VersionBranchDTO create(VersionBranchReq req);

    /**
     * 按版本分支查询
     */
    VersionBranchDTO get(String versionBranch);

    /**
     * 全量列表，按 versionBranch 升序
     */
    List<VersionBranchDTO> list();

    /**
     * 按版本分支修改内容字段（全量覆盖，不可改名）
     */
    VersionBranchDTO update(VersionBranchReq req);

    /**
     * 按版本分支物理删除
     */
    DeleteVersionBranchResp delete(String versionBranch);
}
