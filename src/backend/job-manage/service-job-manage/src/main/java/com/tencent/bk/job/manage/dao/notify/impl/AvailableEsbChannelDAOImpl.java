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

package com.tencent.bk.job.manage.dao.notify.impl;

import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.tables.AvailableEsbChannel;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class AvailableEsbChannelDAOImpl implements AvailableEsbChannelDAO {

    private static final Logger logger = LoggerFactory.getLogger(AvailableEsbChannelDAOImpl.class);
    private static final AvailableEsbChannel T_AVAILABLE_ESB_CHANNEL = AvailableEsbChannel.AVAILABLE_ESB_CHANNEL;
    private static final AvailableEsbChannel defaultTable = T_AVAILABLE_ESB_CHANNEL;
    private final DSLContext dslContext;

    @Autowired
    public AvailableEsbChannelDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int insertAvailableEsbChannel(AvailableEsbChannelDTO availableEsbChannelDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.TYPE,
            defaultTable.ENABLE,
            defaultTable.CREATOR,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            availableEsbChannelDTO.getType(),
            availableEsbChannelDTO.isEnable(),
            availableEsbChannelDTO.getCreator(),
            availableEsbChannelDTO.getLastModifyTime()
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            logger.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteAll() {
        return dslContext.deleteFrom(defaultTable).execute();
    }

    @Override
    public List<AvailableEsbChannelDTO> listAvailableEsbChannel() {
        val records = dslContext.selectFrom(defaultTable).fetch();
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(record -> new AvailableEsbChannelDTO(
                record.getType(),
                record.getEnable(),
                record.getCreator(),
                record.getLastModifyTime()
            ));
        }
    }
}
