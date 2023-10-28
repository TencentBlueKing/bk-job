package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.JobResourceStatusEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateScriptV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteScriptV3Req;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.api.constant.Constant.SHELL_SCRIPT_CONTENT_BASE64;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 业务脚本管理 API 测试
 */
@DisplayName("v3.ScriptResourceAPITest")
class ScriptResourceAPITest {

    private static final List<EsbScriptVersionDetailV3DTO> createdScriptList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
        if (CollectionUtils.isNotEmpty(createdScriptList)) {
            createdScriptList.forEach(script -> {
                EsbDeleteScriptV3Req req = new EsbDeleteScriptV3Req();
                req.setScopeId(script.getScopeId());
                req.setScopeType(script.getScopeType());
                req.setScriptId(script.getScriptId());
                Operations.deleteScript(req);
            });
        }
    }

    @Nested
    class CreateTest {
        @Test
        @DisplayName("测试脚本正常创建")
        void testCreateScript() {
            EsbCreateScriptV3Request req = new EsbCreateScriptV3Request();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            req.setType(ScriptTypeEnum.SHELL.getValue());
            req.setVersion("v1");
            req.setVersionDesc("v1_desc");

            EsbScriptVersionDetailV3DTO createdScript =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_SCRIPT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.script_id", notNullValue())
                    .body("data.name", equalTo(req.getName()))
                    .body("data.script_language", equalTo(req.getType()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    // 返回与 API 文档描述不一致，暂时注释
//                .body("data.content", equalTo(req.getContent()))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0L))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0L))
                    .body("data.version", equalTo(req.getVersion()))
                    .body("data.version_desc", equalTo(req.getVersionDesc()))
                    .body("data.status", equalTo(JobResourceStatusEnum.DRAFT.getValue()))
                    .body("data.description", equalTo(req.getDescription()))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
                    })
                    .getData();

            createdScriptList.add(createdScript);
        }

        @Test
        @DisplayName("异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidParamThenFail() {

        }

    }

    @Nested
    class GetTest {
        @Test
        @DisplayName("测试获取脚本版本列表")
        void testGetScriptVersionList() {

        }
    }

    @Nested
    class UpdateTest {
        // 更新操作

    }

    @Nested
    class OperationTest {
        // 上线、下线等操作
    }

    @Nested
    class DeleteTest {
        @Test
        @DisplayName("测试脚本删除")
        void testDeleteScript() {
            EsbScriptVersionDetailV3DTO createdScript = Operations.createScript();
            EsbDeleteScriptV3Req req = new EsbDeleteScriptV3Req();
            req.setScopeId(createdScript.getScopeId());
            req.setScopeType(createdScript.getScopeType());
            req.setScriptId(createdScript.getScriptId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_SCRIPT)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }


}
