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

package com.tencent.bk.job.crontab.util;

import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static com.cronutils.model.CronType.QUARTZ;
import static com.cronutils.model.CronType.UNIX;

/**
 * @since 17/1/2020 22:06
 */
@Slf4j
public class CronExpressionUtil {

    private static final CronParser QUARTZ_PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(QUARTZ));
    private static final CronParser UNIX_PARSER = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(UNIX));

    public static String fixExpressionForQuartz(String expression) {
        if (StringUtils.isBlank(expression)) {
            return expression;
        }
        Cron unixCron = UNIX_PARSER.parse(expression);
        unixCron.validate();
        return CronMapper.fromUnixToQuartz().map(unixCron).validate().asString();
    }

    public static String fixExpressionForUser(String expression) {
        if (StringUtils.isBlank(expression)) {
            return expression;
        }
        return CronMapper.fromQuartzToUnix().map(QUARTZ_PARSER.parse(expression)).asString();
    }

    /**
     * 仅供展示用的 Quartz -> UNIX 转换：转换失败时退回原表达式。
     * <p>
     * 落库的表达式都已通过校验，理论上转不失败；但调用方（如带审批接口的预检）只是要给人看一眼定时规则，
     * <b>不能因为这一个展示细节把整次调用拖挂</b>，退回 Quartz 形态总比直接报错好。
     */
    public static String fixExpressionForUserSafely(String expression) {
        try {
            return fixExpressionForUser(expression);
        } catch (Exception e) {
            log.warn("Convert cron expression for user display failed, expression: {}", expression, e);
            return expression;
        }
    }
}
