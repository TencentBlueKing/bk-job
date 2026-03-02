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

import com.tencent.bk.job.model.MigrateReq;
import com.tencent.bk.job.model.MigrateResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 按业务分批迁移环境时使用的相关接口
 */
@RequestMapping("/api/migrate")
public interface MigrateBizResource {

    /**
     * 批量标记业务已完成环境迁移
     */
    @PostMapping("/add_migrate_biz")
    MigrateResp addMigrateBiz(@RequestBody MigrateReq req);

    /**
     * 批量取消业务的环境迁移标记
     */
    @PostMapping("/delete_migrate_biz")
    MigrateResp deleteMigrateBiz(@RequestBody MigrateReq req);

    /**
     * 查询某个业务是否已完成环境迁移
     *
     * @param bizId 业务ID
     * @return 是否已完成环境迁移
     */
    @GetMapping("/is_migrated")
    MigrateResp isMigrated(@RequestParam("bizId") String bizId);
}
