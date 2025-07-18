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

package com.tencent.bk.job.common.mysql.listener;

import com.tencent.bk.job.common.mysql.MySQLProperties;
import com.tencent.bk.job.common.mysql.metrics.MetricsConstants;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.ExecuteContext;
import org.jooq.impl.DefaultExecuteListener;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * jOOQ执行监听器，用于监控慢SQL
 */
@Slf4j
public class JooqExecuteListener extends DefaultExecuteListener {

    private final MySQLProperties mySQLProperties;
    private final MeterRegistry meterRegistry;

    private final String KEY_START_TIME = "sqlStartTimeMillis";

    public JooqExecuteListener(MySQLProperties mySQLProperties, MeterRegistry meterRegistry) {
        this.mySQLProperties = mySQLProperties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void executeStart(ExecuteContext ctx) {
        super.executeStart(ctx);
        ctx.data(KEY_START_TIME, System.currentTimeMillis());
    }

    @Override
    public void executeEnd(ExecuteContext ctx) {
        super.executeEnd(ctx);
        tryToAnalysisExecuteTime(ctx);
    }

    /**
     * 尝试分析SQL执行时间，如果异常则打印WARN日志，不影响主流程
     *
     * @param ctx 执行上下文
     */
    @SuppressWarnings("DataFlowIssue")
    private void tryToAnalysisExecuteTime(ExecuteContext ctx) {
        try {
            Long startTimeMillis = (Long) ctx.data(KEY_START_TIME);
            long costTimeMillis = System.currentTimeMillis() - startTimeMillis;
            String sql = parseSql(ctx);
            recordSqlExecuteTimeConsuming(sql, costTimeMillis);
            if (shouldLogSlowSql(costTimeMillis)) {
                // 打印慢SQL日志
                log.warn(
                    "SlowSQL cost {}ms. SQL=[{}]",
                    costTimeMillis,
                    sql
                );
            }
        } catch (Exception e) {
            log.warn("tryToAnalysisExecuteTime failed", e);
        }
    }

    /**
     * 判断是否需要打印慢SQL
     *
     * @param costTimeMillis SQL执行耗时
     * @return 是否需要打印慢SQL
     */
    private boolean shouldLogSlowSql(long costTimeMillis) {
        return mySQLProperties.getSlowSql().isLogEnabled()
            && costTimeMillis > mySQLProperties.getSlowSql().getThresholdMillis();
    }

    /**
     * 从执行上下文中解析真实执行的SQL
     *
     * @param ctx 执行上下文
     * @return 真实执行的SQL
     */
    private String parseSql(ExecuteContext ctx) {
        return ctx.sql();
    }

    /**
     * 记录SQL执行耗时到指标数据
     *
     * @param sql            执行的SQL
     * @param costTimeMillis 耗时
     */
    private void recordSqlExecuteTimeConsuming(String sql, long costTimeMillis) {
        // 1.分析出SQL类型
        String sqlType = parseSqlType(sql);
        // 2.记录到MeterRegistry
        Timer.builder(MetricsConstants.NAME_SQL_EXECUTE_TIME)
            .description("SQL Execute Time")
            .tags(Tags.of(MetricsConstants.TAG_KEY_TYPE, sqlType))
            .publishPercentileHistogram(false)
            .publishPercentiles(0.5, 0.8, 0.95, 0.99)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofMinutes(1L))
            .register(meterRegistry)
            .record(costTimeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 解析SQL类型：SELECT、INSERT、UPDATE、DELETE、UNKNOWN
     *
     * @param sql 待解析的SQL
     * @return SQL类型
     */
    private String parseSqlType(String sql) {
        if (StringUtils.isBlank(sql)) {
            return "UNKNOWN";
        }
        sql = sql.trim().replaceAll("[\r\n]+", " ");
        int indexOfSpace = sql.indexOf(" ");
        if (indexOfSpace < 0) {
            return sql;
        }
        return sql.substring(0, indexOfSpace).toUpperCase();
    }
}
