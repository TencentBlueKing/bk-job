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

package com.tencent.bk.job.controller;

import com.tencent.bk.job.model.DeleteVersionBranchResp;
import com.tencent.bk.job.model.VersionBranchDTO;
import com.tencent.bk.job.model.VersionBranchReq;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 版本分支信息管理接口，鉴权沿用 /api/** 的 X-Job-Op-Api-Key
 */
@RequestMapping("/api/versionBranch")
public interface VersionBranchResource {

    /**
     * 新增版本分支。同一 versionBranch 重复新增返回 409，不执行 upsert。
     */
    @PostMapping
    VersionBranchDTO create(@RequestBody VersionBranchReq req);

    /**
     * 按版本分支查询
     */
    @GetMapping
    VersionBranchDTO get(@RequestParam(value = "versionBranch", required = false) String versionBranch);

    /**
     * 全量列表，不分页，按 versionBranch 升序
     */
    @GetMapping("/list")
    List<VersionBranchDTO> list();

    /**
     * 修改版本分支内容字段。versionBranch 仅作定位键，不可改名。
     * 对 description / devBranch / 两条流水线字段全量覆盖：未传或 null 会写成 NULL。
     * 请先 GET 再提交完整 5 个业务字段。
     */
    @PostMapping("/update")
    VersionBranchDTO update(@RequestBody VersionBranchReq req);

    /**
     * 按版本分支物理删除
     */
    @PostMapping("/delete")
    DeleteVersionBranchResp delete(@RequestParam(value = "versionBranch", required = false) String versionBranch);
}
