package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class RandomUtilTest {

    @Test
    void testNextInt() {
        for (int i = 0; i < 100; i++) {
            int bound = RandomUtil.nextInt(1000) + 1;
            assertThat(RandomUtil.nextInt(bound)).isGreaterThanOrEqualTo(0);
            assertThat(RandomUtil.nextInt(bound)).isLessThan(bound);
        }
    }

}
