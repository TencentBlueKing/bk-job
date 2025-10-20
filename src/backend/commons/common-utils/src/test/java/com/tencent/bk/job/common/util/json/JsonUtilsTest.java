package com.tencent.bk.job.common.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Test
    @DisplayName("序列化时，跳过bean中被SkipLogFields标记的字段（包括嵌套类）")
    void testSkipLogFieldsRecursively() {
        InnerInnerClass iic = new InnerInnerClass(1, 2);
        InnerClass b = new InnerClass(10, 20, iic);
        OuterClass c = new OuterClass(100, 200, b);

        String json = JsonUtils.toJsonWithoutSkippedFieldsRecursively(c);
        System.out.println("序列化后的嵌套类:" + json);
        
        OuterClass c2 = JsonUtils.fromJson(json, OuterClass.class);
        assertThat(c2.getO1()).isEqualTo(100);
        assertThat(c2.getO2()).isNull();

        assertThat(c2.getInnerClass().getI1()).isEqualTo(10);
        assertThat(c2.getInnerClass().getI2()).isNull();

        assertThat(c2.getInnerClass().getInnerInnerClass().getIi1()).isEqualTo(1);
        assertThat(c2.getInnerClass().getInnerInnerClass().getIi2()).isNull();

    }

    @Test
    @DisplayName("序列化时，跳过List和Map中泛型对象的SkipLogFields标记字段")
    void testSkipLogFieldsRecursivelyWithCollectionAndMap() {
        // 创建测试数据
        User user1 = new User("user1", "password1", "token1");
        User user2 = new User("user2", "password2", "token2");
        
        List<User> userList = new ArrayList<>();
        userList.add(user1);
        userList.add(user2);
        
        Map<String, User> userMap = new HashMap<>();
        userMap.put("admin", new User("admin", "adminPass", "adminToken"));
        userMap.put("guest", new User("guest", "guestPass", "guestToken"));
        
        UserContainer container = new UserContainer("container1", userList, userMap);

        String json = JsonUtils.toJsonWithoutSkippedFieldsRecursively(container);
        System.out.println("序列化后的集合和Map:" + json);

        assertThat(json).contains("container1");
        assertThat(json).contains("user1", "user2", "admin", "guest");
        assertThat(json).doesNotContain("password1", "password2", "adminPass", "guestPass");
        assertThat(json).doesNotContain("token1", "token2", "adminToken", "guestToken");
        assertThat(json).doesNotContain("secret_key");
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

    private static class InnerInnerClass {
        private Integer ii1;
        @SkipLogFields
        private Integer ii2;

        public InnerInnerClass(Integer ii1, Integer ii2) {
            this.ii1 = ii1;
            this.ii2 = ii2;
        }

        public InnerInnerClass() {
        }

        public Integer getIi1() {
            return ii1;
        }

        public void setIi1(Integer ii1) {
            this.ii1 = ii1;
        }

        public Integer getIi2() {
            return ii2;
        }

        public void setIi2(Integer ii2) {
            this.ii2 = ii2;
        }
    }

    private static class InnerClass {
        private InnerInnerClass innerInnerClass;
        private Integer i1;
        @SkipLogFields
        private Integer i2;

        public InnerClass(Integer i1, Integer i2, InnerInnerClass innerInnerClass) {
            this.i1 = i1;
            this.i2 = i2;
            this.innerInnerClass = innerInnerClass;
        }

        public InnerClass() {
        }

        public Integer getI1() {
            return i1;
        }

        public void setI1(Integer i1) {
            this.i1 = i1;
        }

        public Integer getI2() {
            return i2;
        }

        public void setI2(Integer i2) {
            this.i2 = i2;
        }

        public InnerInnerClass getInnerInnerClass() {
            return innerInnerClass;
        }

        public void setInnerInnerClass(InnerInnerClass innerInnerClass) {
            this.innerInnerClass = innerInnerClass;
        }
    }

    private static class OuterClass {
        private Integer o1;
        @SkipLogFields
        private Integer o2;
        private InnerClass innerClass;

        public OuterClass(Integer o1, Integer o2, InnerClass innerClass) {
            this.o1 = o1;
            this.o2 = o2;
            this.innerClass = innerClass;
        }

        public OuterClass() {
        }

        public Integer getO1() {
            return o1;
        }

        public void setO1(Integer o1) {
            this.o1 = o1;
        }

        public Integer getO2() {
            return o2;
        }

        public void setO2(Integer o2) {
            this.o2 = o2;
        }

        public InnerClass getInnerClass() {
            return innerClass;
        }

        public void setInnerClass(InnerClass innerClass) {
            this.innerClass = innerClass;
        }
    }

    /**
     * 用户类，包含敏感字段
     */
    private static class User {
        private String username;
        @SkipLogFields
        private String password;
        @SkipLogFields
        private String token;

        public User() {
        }

        public User(String username, String password, String token) {
            this.username = username;
            this.password = password;
            this.token = token;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * 用户容器类，包含List和Map类型的成员变量
     */
    private static class UserContainer {
        private String name;
        @SkipLogFields("secret_key")
        @JsonProperty("secret_key")
        private String secretKey;
        private List<User> userList;
        private Map<String, User> userMap;

        public UserContainer() {
        }

        public UserContainer(String name, List<User> userList, Map<String, User> userMap) {
            this.name = name;
            this.secretKey = "secret-" + name;
            this.userList = userList;
            this.userMap = userMap;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public List<User> getUserList() {
            return userList;
        }

        public void setUserList(List<User> userList) {
            this.userList = userList;
        }

        public Map<String, User> getUserMap() {
            return userMap;
        }

        public void setUserMap(Map<String, User> userMap) {
            this.userMap = userMap;
        }
    }
}
