package com.tencent.bk.job.crontab.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CronExpressionUtilTest {

    @Test
    public void testFixExpression() {
        String fixedCron = CronExpressionUtil.fixExpressionForUser("0 30 10 8 * ? *");
        assertThat(fixedCron.equals("30 10 8 * *")).isTrue();
        assertThat(CronExpressionUtil.fixExpressionForUser(" ").equals(" ")).isTrue();
        assertThat(CronExpressionUtil.fixExpressionForUser(null)).isNull();
    }
}
