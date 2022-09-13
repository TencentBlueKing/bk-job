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

package com.tencent.bk.job.manage.api.migration.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.migration.AddHostIdMigrationTask;
import com.tencent.bk.job.manage.migration.EncryptDbAccountPasswordMigrationTask;
import com.tencent.bk.job.manage.migration.ResourceTagsMigrationTask;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.migration.AddHostIdMigrationReq;
import com.tencent.bk.job.manage.model.migration.SetBizSetMigrationStatusReq;
import com.tencent.bk.job.manage.service.impl.BizSetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 微服务升级
 */
@RequestMapping("/migration")
@Slf4j
@RestController
public class MigrationResource {
    private final EncryptDbAccountPasswordMigrationTask encryptDbAccountPasswordMigrationTask;
    private final ResourceTagsMigrationTask resourceTagsMigrationTask;
    private final BizSetService bizSetService;
    private final AddHostIdMigrationTask addHostIdMigrationTask;

    @Autowired
    public MigrationResource(
        EncryptDbAccountPasswordMigrationTask encryptDbAccountPasswordMigrationTask,
        ResourceTagsMigrationTask resourceTagsMigrationTask,
        BizSetService bizSetService,
        AddHostIdMigrationTask addHostIdMigrationTask) {
        this.encryptDbAccountPasswordMigrationTask = encryptDbAccountPasswordMigrationTask;
        this.resourceTagsMigrationTask = resourceTagsMigrationTask;
        this.bizSetService = bizSetService;
        this.addHostIdMigrationTask = addHostIdMigrationTask;
    }

    /**
     * 对DB账号的密码进行加密
     */
    @PostMapping("/action/encryptDbAccountPassword")
    public Response<List<Long>> encryptDbAccountPassword() {
        return encryptDbAccountPasswordMigrationTask.encryptDbAccountPassword();
    }

    /**
     * 迁移模板、脚本引用的标签数据到resource_tag表
     */
    @PostMapping("/action/migrationResourceTags")
    public Response<List<ResourceTagDTO>> upgradeResourceTags() {
        return resourceTagsMigrationTask.execute();
    }

    /**
     * 设置迁移业务集的状态
     */
    @PostMapping("/action/setBizSetMigrationStatus")
    public Response<Boolean> setBizSetMigrationStatus(@RequestBody SetBizSetMigrationStatusReq req) {
        return Response.buildSuccessResp(bizSetService.setBizSetMigratedToCMDB(req.getMigrated()));
    }

    /**
     * 作业模板、执行方案等包含的主机数据，在原来的云区域+ip的基础上，填充hostID属性
     */
    @PostMapping("/action/addHostIdMigrationTask")
    public Response<String> addHostIdMigrationTask(AddHostIdMigrationReq req) {
        List<AddHostIdMigrationTask.AddHostIdResult> results = addHostIdMigrationTask.execute(req.isDryRun());
        boolean success = results.stream().allMatch(AddHostIdMigrationTask.AddHostIdResult::isSuccess);
        return success ? Response.buildSuccessResp(JsonUtils.toJson(results)) :
            Response.buildCommonFailResp(ErrorCode.MIGRATION_FAIL, new String[]{"AddHostIdMigrationTask",
                JsonUtils.toJson(results)});
    }
}
