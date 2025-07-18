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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.dao.OperationLogDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.tables.OperationLog;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class OperationLogDAOImpl extends BaseDAO implements OperationLogDAO {
    private static final OperationLog TABLE = OperationLog.OPERATION_LOG;

    @Autowired
    public OperationLogDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, TABLE.getName());
    }

    @Override
    @MySQLOperation(table = "operation_log", op = DbOperationEnum.WRITE)
    public long saveOperationLog(OperationLogDTO operationLog) {
        Record record = dsl().insertInto(
                TABLE,
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.OP_CODE,
                TABLE.OPERATOR,
                TABLE.CREATE_TIME,
                TABLE.DETAIL)
            .values(
                operationLog.getId(),
                operationLog.getTaskInstanceId(),
                JooqDataTypeUtil.toByte(operationLog.getOperationEnum().getValue()),
                operationLog.getOperator(),
                operationLog.getCreateTime(),
                JsonUtils.toJson(operationLog.getDetail()))
            .returning(TABLE.ID)
            .fetchOne();

        return operationLog.getId() != null ? operationLog.getId() : record.getValue(TABLE.ID);
    }

    @Override
    @MySQLOperation(table = "operation_log", op = DbOperationEnum.READ)
    public List<OperationLogDTO> listOperationLog(long taskInstanceId) {
        Result result = dsl()
            .select(
                TABLE.ID,
                TABLE.TASK_INSTANCE_ID,
                TABLE.OP_CODE,
                TABLE.OPERATOR,
                TABLE.CREATE_TIME,
                TABLE.DETAIL)
            .from(TABLE)
            .where(TABLE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .orderBy(TABLE.CREATE_TIME.desc())
            .fetch();
        List<OperationLogDTO> opLogs = new ArrayList<>();
        if (result.size() > 0) {
            result.into(record -> {
                opLogs.add(extractInfo(record));
            });
        }
        return opLogs;
    }

    private OperationLogDTO extractInfo(Record record) {
        OperationLogDTO opLog = new OperationLogDTO();
        opLog.setId(record.get(TABLE.ID));
        opLog.setTaskInstanceId(record.get(TABLE.TASK_INSTANCE_ID));
        opLog.setOperationEnum(UserOperationEnum.valueOf(JooqDataTypeUtil.toInteger(record.get(TABLE.OP_CODE))));
        opLog.setCreateTime(record.get(TABLE.CREATE_TIME));
        opLog.setOperator(record.get(TABLE.OPERATOR));
        String detail = record.get(TABLE.DETAIL);
        if (StringUtils.isNotEmpty(detail)) {
            opLog.setDetail(JsonUtils.fromJson(detail, OperationLogDTO.OperationDetail.class));
        }

        return opLog;
    }
}
