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

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.execute.dao.CallbackUrlWhiteInfoDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.CallbackUrlWhiteInfoDTO;
import com.tencent.bk.job.execute.model.tables.CallbackUrlWhiteInfo;
import org.jooq.Batch;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class CallbackUrlWhiteInfoDAOImpl extends BaseDAO implements CallbackUrlWhiteInfoDAO {

    private static final CallbackUrlWhiteInfo T = CallbackUrlWhiteInfo.CALLBACK_URL_WHITE_INFO;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        T.ID,
        T.BASE_URL,
        T.DESCRIPTION,
        T.CREATOR,
        T.LAST_MODIFY_USER,
        T.CREATE_TIME,
        T.LAST_MODIFY_TIME
    };

    private final DSLContext ctx;

    @Autowired
    public CallbackUrlWhiteInfoDAOImpl(DSLContextProviderFactory dslContextProviderFactory) {
        super(dslContextProviderFactory, T.getName());
        this.ctx = dsl();
    }

    @Override
    public List<String> listAllBaseUrls() {
        return ctx.select(T.BASE_URL)
            .from(T)
            .fetch(T.BASE_URL);
    }

    @Override
    public PageData<CallbackUrlWhiteInfoDTO> listByPage(Integer start, Integer length) {
        int offset = start == null || start < 0 ? 0 : start;
        int limit = length == null || length <= 0 ? 10 : length;
        Result<?> records = ctx.select(ALL_FIELDS)
            .from(T)
            .orderBy(T.ID.desc())
            .limit(offset, limit)
            .fetch();
        long total = ctx.selectCount().from(T).fetchOne(0, Long.class);
        List<CallbackUrlWhiteInfoDTO> data = new ArrayList<>(records.size());
        for (Record record : records) {
            data.add(convert(record));
        }
        PageData<CallbackUrlWhiteInfoDTO> pageData = new PageData<>();
        pageData.setStart(offset);
        pageData.setPageSize(limit);
        pageData.setTotal(total);
        pageData.setData(data);
        return pageData;
    }

    @Override
    public List<String> filterExistingBaseUrls(Collection<String> baseUrls) {
        if (baseUrls == null || baseUrls.isEmpty()) {
            return Collections.emptyList();
        }
        return ctx.select(T.BASE_URL)
            .from(T)
            .where(T.BASE_URL.in(baseUrls))
            .fetch(T.BASE_URL);
    }

    @Override
    public int batchInsert(List<CallbackUrlWhiteInfoDTO> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }
        // 用 jOOQ batch API 一次性提交；显式不写 ID/时间字段（让 DB 自增 + DEFAULT CURRENT_TIMESTAMP）
        Query[] queries = records.stream()
            .map(dto -> (Query) ctx.insertInto(T)
                .columns(T.BASE_URL, T.DESCRIPTION, T.CREATOR, T.LAST_MODIFY_USER)
                .values(dto.getBaseUrl(), dto.getDescription(), dto.getCreator(), dto.getLastModifyUser()))
            .toArray(Query[]::new);
        Batch batch = ctx.batch(queries);
        int[] results = batch.execute();
        int affected = 0;
        for (int r : results) {
            if (r > 0) {
                affected += r;
            }
        }
        return affected;
    }

    @Override
    public int deleteByIds(Collection<Long> idList) {
        if (idList == null || idList.isEmpty()) {
            return 0;
        }
        return ctx.deleteFrom(T)
            .where(T.ID.in(idList))
            .execute();
    }

    private CallbackUrlWhiteInfoDTO convert(Record record) {
        CallbackUrlWhiteInfoDTO dto = new CallbackUrlWhiteInfoDTO();
        dto.setId(record.get(T.ID));
        dto.setBaseUrl(record.get(T.BASE_URL));
        dto.setDescription(record.get(T.DESCRIPTION));
        dto.setCreator(record.get(T.CREATOR));
        dto.setLastModifyUser(record.get(T.LAST_MODIFY_USER));
        dto.setCreateTime(record.get(T.CREATE_TIME));
        dto.setLastModifyTime(record.get(T.LAST_MODIFY_TIME));
        return dto;
    }
}
