package com.tencent.bk.job.common.util.json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilsTest {

    @Test
    @DisplayName("测试null对象转换为字符串")
    public void testNullToJson() {
        String jsonStr = JsonUtils.toJson(null);
        assertThat(jsonStr).isEqualTo("null");
    }

}
