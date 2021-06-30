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

package com.tencent.bk.job.common.util;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.bean.BeanMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

/**
 * 分页工具
 *
 * @param <E>
 */
public class PageHelper<E> {
    public PageData<E> fetchPage(final JdbcTemplate jdbcTemplate,
                                 final String sqlCountRows, final Object countArgs[],
                                 final String sqlFetchRows, final Object fetchRowsArgs[],
                                 final int start, final int length,
                                 final RowMapper<E> rowMapper) {
        final long rowCount = jdbcTemplate.queryForObject(sqlCountRows, countArgs, Long.class);
        final PageData<E> page = new PageData<>();
        page.setStart(start);
        page.setPageSize(length);
        page.setTotal(rowCount);
        page.setData(Lists.newArrayListWithCapacity(length));
        jdbcTemplate.query(sqlFetchRows, fetchRowsArgs, (ResultSetExtractor<Void>) rs -> {
            final List<E> pageItems = page.getData();
            int currentRow = 0;
            while (rs.next()) {
                pageItems.add(rowMapper.mapRow(rs, currentRow));
                currentRow++;
            }

            return null;
        });
        return page;
    }

    public PageData<E> fetchPage(final JdbcTemplate jdbcTemplate,
                                 final String sqlCountRows, final Object countArgs[],
                                 final String sqlFetchRows, final Object fetchRowsArgs[],
                                 final int start, final int length, Class<E> beanClass) {
        long rowCount = jdbcTemplate.queryForObject(sqlCountRows, countArgs, Long.class);
        PageData<E> page = new PageData<>();
        page.setStart(start);
        page.setPageSize(length);
        page.setTotal(rowCount);
        page.setData(Lists.newArrayListWithCapacity(length));

        Object[] pageArgs = new Object[fetchRowsArgs.length + 2];
        System.arraycopy(fetchRowsArgs, 0, pageArgs, 0, fetchRowsArgs.length);
        pageArgs[fetchRowsArgs.length] = start;
        pageArgs[fetchRowsArgs.length + 1] = length;
        List<E> es = BeanMapper.mapList(jdbcTemplate.queryForList(sqlFetchRows, pageArgs), beanClass);
        page.setData(es);
        return page;
    }
}
