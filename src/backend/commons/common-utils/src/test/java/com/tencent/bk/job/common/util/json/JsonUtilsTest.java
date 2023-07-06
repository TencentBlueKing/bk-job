package com.tencent.bk.job.common.util.json;

import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class JsonUtilsTest {

    @Test
    @DisplayName("测试null对象转换为字符串")
    public void testNullToJson() {
        String jsonStr = JsonUtils.toJson(null);
        assertThat(jsonStr).isEqualTo("null");
    }

    @Test
    @DisplayName("测试 Joda Time json field 反序列化")
    void jodaTimeToJson() {
        class TestObj {
            private DateTime dateTime = DateTime.now();

            public DateTime getDateTime() {
                return dateTime;
            }

            public void setDateTime(DateTime dateTime) {
                this.dateTime = dateTime;
            }
        }

        TestObj testObj = new TestObj();
        assertThatCode(() -> JsonUtils.toJson(testObj)).doesNotThrowAnyException();
    }
}
