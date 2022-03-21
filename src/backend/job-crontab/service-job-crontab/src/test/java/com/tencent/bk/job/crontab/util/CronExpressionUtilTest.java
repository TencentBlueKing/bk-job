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
}
