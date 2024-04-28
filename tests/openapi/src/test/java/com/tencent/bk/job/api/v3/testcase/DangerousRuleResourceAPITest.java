package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.Constant;
import com.tencent.bk.job.api.constant.EnableStatusEnum;
import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.HighRiskGrammarActionEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbDangerousRuleV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCheckScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreateDangerousRuleV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetDangerousRuleV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbManageDangerousRuleV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbUpdateDangerousRuleV3Req;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 高危语句规则管理 API 测试
 */
@DisplayName("v3.DangerousRuleResourceAPITest")
class DangerousRuleResourceAPITest extends BaseTest {

    private static final List<EsbDangerousRuleV3DTO> createdDangerousRuleList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
        if (CollectionUtils.isNotEmpty(createdDangerousRuleList)) {
            createdDangerousRuleList.forEach(dangerousRule -> {
                // 清理脚本
                EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
                req.setId(dangerousRule.getId());
                Operations.deleteDangerousRule(req);
            });
        }
    }

    @Nested
    class DangerousRuleCreateTest {
        @Test
        @DisplayName("测试高危语句规则正常创建")
        void testCreateDangerousRule() {
            EsbCreateDangerousRuleV3Req req = new EsbCreateDangerousRuleV3Req();
            req.setExpression("rm ");
            req.setDescription(TestValueGenerator.generateUniqueStrValue("dangerous_rule_desc", 50));
            req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SHELL.getValue().byteValue()));
            req.setAction(HighRiskGrammarActionEnum.SCAN.getCode());
            EsbDangerousRuleV3DTO createdDangerousRule =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_DANGEROUS_RULE)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.expression", equalTo(req.getExpression()))
                    .body("data.description", equalTo(req.getDescription()))
                    .body("data.action", equalTo(req.getAction()))
                    .body("data.status", equalTo(EnableStatusEnum.DISABLED.getValue()))
                    .body("data.script_language_list", containsInAnyOrder(req.getScriptTypeList()
                        .stream()
                        .map(Byte::intValue)
                        .collect(Collectors.toList())
                        .toArray()))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0L))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0L))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbDangerousRuleV3DTO>>() {
                    })
                    .getData();
            createdDangerousRuleList.add(createdDangerousRule);
        }

        @Test
        @DisplayName("创建高危语句规则异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidCreateDangerousRuleParamThenFail() {
            EsbCreateDangerousRuleV3Req req = new EsbCreateDangerousRuleV3Req();
            req.setExpression("rm -rf /tmp");
            req.setDescription(TestValueGenerator.generateUniqueStrValue("dangerous_rule_desc", 50));
            req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SHELL.getValue().byteValue()));
            req.setAction(HighRiskGrammarActionEnum.SCAN.getCode());

            // 表达式为空，创建失败
            req.setExpression(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 脚本语言列表为空，创建失败
            req.setExpression("rm -rf /tmp");
            req.setScriptTypeList(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 处理动作只能是1或2枚举，否则创建失败
            req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SHELL.getValue().byteValue()));
            req.setAction(-1);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    class DangerousRuleGetTest {
        @Test
        @DisplayName("测试获取高危语句规则列表")
        void testGetDangerousRuleList() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            createdDangerousRuleList.add(createdDangerousRule);
            // 处理动作查询
            EsbGetDangerousRuleV3Req req = new EsbGetDangerousRuleV3Req();
            req.setAction(HighRiskGrammarActionEnum.SCAN.getCode());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_DANGEROUS_RULE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());

            // 脚本类型查询
            req.setAction(null);
            req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SHELL.getValue().byteValue()));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_DANGEROUS_RULE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());

            // 表达式查询
            req.setAction(null);
            req.setScriptTypeList(null);
            req.setExpression("rm ");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_DANGEROUS_RULE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());
        }
    }

    @Nested
    class DangerousRuleUpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试更新高危语句规则信息")
        void testUpdateDangerousRule() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            createdDangerousRuleList.add(createdDangerousRule);
            EsbUpdateDangerousRuleV3Req req = new EsbUpdateDangerousRuleV3Req();
            req.setId(createdDangerousRule.getId());
            req.setExpression("delete ");
            req.setDescription(TestValueGenerator.generateUniqueStrValue("dangerous_rule_desc", 50));
            req.setScriptTypeList(Arrays.asList(ScriptTypeEnum.SQL.getValue().byteValue()));
            req.setAction(HighRiskGrammarActionEnum.SCAN.getCode());

            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", greaterThan(0))
                .body("data.expression", equalTo(req.getExpression()))
                .body("data.description", equalTo(req.getDescription()))
                .body("data.action", equalTo(req.getAction()))
                .body("data.script_language_list", containsInAnyOrder(req.getScriptTypeList()
                    .stream()
                    .map(Byte::intValue)
                    .collect(Collectors.toList())
                    .toArray()))
                .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L));
        }
    }

    @Nested
    class DangerousRuleOperationTest {
        // 启用、停用等操作
        @Test
        @DisplayName("测试启用高危语句规则")
        void testEnableDangerousRule() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            createdDangerousRuleList.add(createdDangerousRule);
            EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
            req.setId(createdDangerousRule.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.ENABLE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(EnableStatusEnum.ENABLED.getValue()))
                .body("data.id", equalTo(req.getId().intValue()));
        }

        @Test
        @DisplayName("测试停用高危语句规则")
        void testDisableDangerousRule() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            createdDangerousRuleList.add(createdDangerousRule);
            EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
            req.setId(createdDangerousRule.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DISABLE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(EnableStatusEnum.DISABLED.getValue()))
                .body("data.id", equalTo(req.getId().intValue()));
        }

    }

    @Nested
    class DangerousRuleDeleteTest {
        @Test
        @DisplayName("测试高危语句规则删除")
        void testDeleteDangerousRule() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
            req.setId(createdDangerousRule.getId());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }

    @Nested
    class ScriptCheckTest {
        @Test
        @DisplayName("测试高危脚本检测")
        void testCheckScript() {
            EsbDangerousRuleV3DTO createdDangerousRule = Operations.createDangerousRule();
            createdDangerousRuleList.add(createdDangerousRule);
            // 创建的高危语句规则默认未启用，先开启，再检测
            EsbManageDangerousRuleV3Req req = new EsbManageDangerousRuleV3Req();
            req.setId(createdDangerousRule.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.ENABLE_DANGEROUS_RULE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(EnableStatusEnum.ENABLED.getValue()))
                .body("data.id", equalTo(req.getId().intValue()));

            EsbCheckScriptV3Req checkReq = new EsbCheckScriptV3Req();
            checkReq.setContent(Constant.SHELL_DANGEROUS_SCRIPT_CONTENT_BASE64);
            checkReq.setType(ScriptTypeEnum.SHELL.getValue());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(checkReq))
                .post(APIV3Urls.CHECK_SCRIPT)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.size()", greaterThan(0));
        }
    }
}
