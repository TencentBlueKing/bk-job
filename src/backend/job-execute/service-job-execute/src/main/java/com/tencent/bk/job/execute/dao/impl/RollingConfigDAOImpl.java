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

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.dao.RollingConfigDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import com.tencent.bk.job.execute.model.tables.RollingConfig;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RollingConfigDAOImpl extends BaseDAO implements RollingConfigDAO {

    private static final RollingConfig TABLE = RollingConfig.ROLLING_CONFIG;

    @Autowired
    public RollingConfigDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    @Override
    @MySQLOperation(table = "rolling_config", op = DbOperationEnum.WRITE)
    public long saveRollingConfig(RollingConfigDTO rollingConfig) {
        Record record = dsl().insertInto(
                TABLE,
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.CONFIG_NAME,
                TABLE.CONFIG)
            .values(
                rollingConfig.getId(),
                rollingConfig.getTaskInstanceId(),
                rollingConfig.getConfigName(),
                JsonUtils.toJson(rollingConfig.getConfigDetail()))
            .returning(TABLE.ID)
            .fetchOne();

        return rollingConfig.getId() != null ? rollingConfig.getId() : record.getValue(TABLE.ID);

    }

    @Override
    @MySQLOperation(table = "rolling_config", op = DbOperationEnum.READ)
    public RollingConfigDTO queryRollingConfigById(Long taskInstanceId, Long rollingConfigId) {
        Record record = dsl().select(
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.CONFIG_NAME,
                TABLE.CONFIG)
            .from(TABLE)
            .where(TaskInstanceIdDynamicCondition.build(taskInstanceId, TABLE.TASK_INSTANCE_ID::eq))
            .and(TABLE.ID.eq(rollingConfigId))
            .fetchOne();
        return extract(record);
    }

    @Override
    @MySQLOperation(table = "rolling_config", op = DbOperationEnum.READ)
    public boolean existsRollingConfig(long taskInstanceId) {
        Result<Record1<Integer>> records = dsl().selectOne()
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .limit(1)
            .fetch();
        return CollectionUtils.isNotEmpty(records);
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
