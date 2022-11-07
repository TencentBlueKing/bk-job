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

package com.tencent.bk.job.crontab.api.migration.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.migration.AddHostIdForCronVariableMigrationTask;
import com.tencent.bk.job.manage.model.migration.AddHostIdMigrationReq;
import com.tencent.bk.job.manage.model.migration.AddHostIdResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 微服务升级
 */
@RequestMapping("/migration")
@Slf4j
@RestController
public class MigrationResource {
    private final AddHostIdForCronVariableMigrationTask addHostIdForCronVariableMigrationTask;

    @Autowired
    public MigrationResource(AddHostIdForCronVariableMigrationTask addHostIdForCronVariableMigrationTask) {
        this.addHostIdForCronVariableMigrationTask = addHostIdForCronVariableMigrationTask;
    }

    /**
     * 定时任务变量包含的主机数据，在原来的云区域+ip的基础上，填充hostID属性
     */
    @PostMapping("/action/addHostIdMigrationTask")
    public Response<String> addHostIdMigrationTask(@RequestBody AddHostIdMigrationReq req) {
        List<AddHostIdResult> results = new ArrayList<>();
        results.add(addHostIdForCronVariableMigrationTask.execute(req.isDryRun()));
        boolean success = results.stream().allMatch(AddHostIdResult::isSuccess);
        return success ? Response.buildSuccessResp(JsonUtils.toJson(results)) :
            Response.buildCommonFailResp(ErrorCode.MIGRATION_FAIL, new String[]{"AddHostIdMigrationTask",
                JsonUtils.toJson(results)});
    }
}
