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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.dao.RollingConfigDAO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.RollingConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class RollingConfigDAOImpl implements RollingConfigDAO {

    private static final RollingConfig TABLE = RollingConfig.ROLLING_CONFIG;
    private final DSLContext CTX;

    @Autowired
    public RollingConfigDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.CTX = ctx;
    }

    @Override
    public long saveRollingConfig(RollingConfigDTO rollingConfig) {
        Record record = CTX.insertInto(
            TABLE,
            TABLE.TASK_INSTANCE_ID,
            TABLE.CONFIG_NAME,
            TABLE.CONFIG)
            .values(
                rollingConfig.getTaskInstanceId(),
                rollingConfig.getConfigName(),
                JsonUtils.toJson(rollingConfig.getConfigDetail()))
            .returning(TABLE.ID)
            .fetchOne();
        assert record != null;
        return record.get(TABLE.ID);
    }

    @Override
    public RollingConfigDTO queryRollingConfigById(Long rollingConfigId) {
        Record record = CTX.select(
            TABLE.ID,
            TABLE.TASK_INSTANCE_ID,
            TABLE.CONFIG_NAME,
            TABLE.CONFIG)
            .from(TABLE)
            .where(TABLE.ID.eq(rollingConfigId))
            .fetchOne();
        return extract(record);
    }

    private RollingConfigDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        rollingConfig.setId(record.get(TABLE.ID));
        rollingConfig.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        rollingConfig.setConfigName(record.get(TABLE.CONFIG_NAME));
        rollingConfig.setConfigDetail(JsonUtils.fromJson(record.get(TABLE.CONFIG), RollingConfigDetailDO.class));
        return rollingConfig;
    }
}
