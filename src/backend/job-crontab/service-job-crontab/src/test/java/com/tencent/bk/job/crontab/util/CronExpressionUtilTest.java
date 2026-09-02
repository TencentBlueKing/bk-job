package com.tencent.bk.job.crontab.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CronExpressionUtilTest {

    @Test
    public void testFixExpression() {
        String fixedCron = CronExpressionUtil.fixExpressionForUser("0 30 10 8 * ? *");
        assertThat("30 10 8 * *".equals(fixedCron)).isTrue();
        assertThat(" ".equals(CronExpressionUtil.fixExpressionForUser(" "))).isTrue();
        assertThat(CronExpressionUtil.fixExpressionForUser(null)).isNull();
    }

    @Test
    public void testFixExpressionForUserSafely() {
        assertThat(CronExpressionUtil.fixExpressionForUserSafely("0 30 10 8 * ? *")).isEqualTo("30 10 8 * *");
        // 展示用转换不能因为表达式非法就抛出去，退回原表达式总比让调用方整个失败好
        assertThat(CronExpressionUtil.fixExpressionForUserSafely("not-a-cron")).isEqualTo("not-a-cron");
        assertThat(CronExpressionUtil.fixExpressionForUserSafely(null)).isNull();
    }
}
