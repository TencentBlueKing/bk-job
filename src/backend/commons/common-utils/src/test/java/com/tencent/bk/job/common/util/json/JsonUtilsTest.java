package com.tencent.bk.job.common.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @Test
    @DisplayName("测试序列化非空的属性")
    void testToNonEmptyJson() {
        Content content = new Content();
        content.setId("abc");
        assertThat(JsonUtils.toNonEmptyJson(content)).doesNotContain("name");
    }

    @Test
    @DisplayName("测试序列化所有属性，包括 null/empty 的")
    void testAllOutputJson() {
        Content content = new Content();
        content.setId("abc");
        assertThat(JsonUtils.toJson(content)).contains("id", "name");
    }

    @Test
    @DisplayName("测试 JsonInclude 注解覆盖 JsonUtils 中的默认的Include 配置")
    void testJsonIncludeAnnotation() {
        Content2 content = new Content2();
        content.setId("abc");
        assertThat(JsonUtils.toJson(content)).doesNotContain("name");
    }


    private static class Content {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class Content2 {
        private String id;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
