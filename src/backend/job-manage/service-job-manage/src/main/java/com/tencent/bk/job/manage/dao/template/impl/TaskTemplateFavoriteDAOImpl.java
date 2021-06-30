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

package com.tencent.bk.job.manage.dao.template.impl;

import com.tencent.bk.job.manage.dao.TaskFavoriteDAO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.generated.tables.TaskFavoriteTemplate;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3/10/2019 21:52
 */
@Slf4j
@Repository("TaskTemplateFavoriteDAOImpl")
public class TaskTemplateFavoriteDAOImpl implements TaskFavoriteDAO {

    private static final TaskFavoriteTemplate TABLE = TaskFavoriteTemplate.TASK_FAVORITE_TEMPLATE;

    private DSLContext context;

    @Autowired
    public TaskTemplateFavoriteDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public List<Long> listFavoriteParentIdByUser(long appId, String username) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.USERNAME.eq(username));
        conditions.add(TABLE.IS_DELETED.eq(UByte.valueOf(0)));
        Result<Record1<ULong>> results = context.select(TABLE.TEMPLATE_ID).from(TABLE).where(conditions).fetch();
        List<Long> templateIdList = new ArrayList<>();
        if (results != null && results.size() >= 1) {
            results.map(record -> templateIdList.add(record.get(TABLE.TEMPLATE_ID).longValue()));
        }
        return templateIdList;
    }

    @Override
    public boolean insertFavorite(long appId, String username, long parentId) {
        int affected = context.insertInto(TABLE).columns(TABLE.APP_ID, TABLE.USERNAME, TABLE.TEMPLATE_ID)
            .values(ULong.valueOf(appId), username, ULong.valueOf(parentId)).onDuplicateKeyUpdate()
            .set(TABLE.IS_DELETED, UByte.valueOf(0)).execute();
        return 1 == affected || 2 == affected;
    }

    @Override
    public boolean deleteFavorite(long appId, String username, long parentId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.USERNAME.eq(username));
        conditions.add(TABLE.TEMPLATE_ID.eq(ULong.valueOf(parentId)));
        return 1 == context.update(TABLE).set(TABLE.IS_DELETED, UByte.valueOf(1)).where(conditions).limit(1).execute();
    }
}
