package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.JobResourceStatusEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Base64Util;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreatePublicScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbCreatePublicScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeletePublicScriptV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbDeletePublicScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetPublicScriptVersionListV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbManagePublicScriptVersionV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbUpdatePublicScriptBasicV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbUpdatePublicScriptVersionV3Req;
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
 * 公共脚本管理 API 测试
 */
@DisplayName("v3.PublicScriptResourceAPITest")
class PublicScriptResourceAPITest extends BaseTest {

    private static final List<EsbScriptVersionDetailV3DTO> createdPublicScriptList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
        if (CollectionUtils.isNotEmpty(createdPublicScriptList)) {
            createdPublicScriptList.forEach(script -> {
                // 清理脚本
                EsbDeletePublicScriptV3Req req = new EsbDeletePublicScriptV3Req();
                req.setScriptId(script.getScriptId());
                Operations.deletePublicScript(req);
            });
        }
    }

    @Nested
    class PublicScriptCreateTest {
        @Test
        @DisplayName("测试公共脚本脚本正常创建")
        void testCreatePublicScript() {
            EsbCreatePublicScriptV3Req req = new EsbCreatePublicScriptV3Req();
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setDescription(TestValueGenerator.generateUniqueStrValue("shell_script_desc", 50));
            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            req.setType(ScriptTypeEnum.SHELL.getValue());
            req.setVersion("v1");
            req.setVersionDesc("v1_desc");

            EsbScriptVersionDetailV3DTO createdScript =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.script_id", notNullValue())
                    .body("data.name", equalTo(req.getName()))
                    .body("data.script_language", equalTo(req.getType()))
                    .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
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

            createdPublicScriptList.add(createdScript);
        }

        @Test
        @DisplayName("创建公共脚本异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidCreatePublicScriptParamThenFail() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbCreatePublicScriptV3Req req = new EsbCreatePublicScriptV3Req();
            req.setContent(Base64Util.base64EncodeContentToStr(createdPublicScript.getContent()));
            req.setDescription(createdPublicScript.getDescription());
            req.setType(createdPublicScript.getType());
            req.setVersion("v2");
            req.setVersionDesc("v2_desc");
            // 脚本名称为空，创建失败
            req.setName(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 脚本名称有特殊字符，创建失败
            req.setName(TestValueGenerator.generateUniqueStrValue("*<>", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            // 脚本类型非法，创建失败
            req.setType(-1);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            req.setType(ScriptTypeEnum.SHELL.getValue());
            // 脚本内容为空，创建失败
            req.setContent(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            // 版本号为空, 创建失败
            req.setVersion(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 版本号有特殊字符, 创建失败
            req.setVersion("|");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试公共脚本版本创建")
        void testCreatePublicScriptVersion() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbCreatePublicScriptVersionV3Req req = new EsbCreatePublicScriptVersionV3Req();
            req.setContent(Base64Util.base64EncodeContentToStr(createdPublicScript.getContent()));
            req.setVersion("v2");
            req.setVersionDesc("v2_desc");
            req.setScriptId(createdPublicScript.getScriptId());
            EsbScriptVersionDetailV3DTO createdScript =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.script_id", equalTo(req.getScriptId()))
                    .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0L))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0L))
                    .body("data.version", equalTo(req.getVersion()))
                    .body("data.version_desc", equalTo(req.getVersionDesc()))
                    .body("data.status", equalTo(JobResourceStatusEnum.DRAFT.getValue()))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbScriptVersionDetailV3DTO>>() {
                    })
                    .getData();
        }

        @Test
        @DisplayName("创建公共脚本版本异常场景测试-比如参数校验，业务逻辑等")
        void givenInvalidCreatePublicScriptVersionParamThenFail() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbCreatePublicScriptVersionV3Req req = new EsbCreatePublicScriptVersionV3Req();
            req.setContent(Base64Util.base64EncodeContentToStr(createdPublicScript.getContent()));
            req.setVersion("v2");
            req.setVersionDesc("v2_desc");

            // 脚本ID为空，创建失败
            req.setScriptId(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            // 脚本内容为空，创建失败
            req.setScriptId(createdPublicScript.getScriptId());
            req.setContent(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            // 版本号为空，创建失败
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            req.setVersion(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            // 版本号有特殊字符，创建失败
            req.setVersion("v2?");
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            // 同一脚本下，脚本版本号重复，创建失败
            req.setVersion(createdPublicScript.getVersion());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.SCRIPT_VERSION_NAME_EXIST));
        }
    }

    @Nested
    class PublicScriptGetTest {
        @Test
        @DisplayName("测试获取公共脚本版本列表")
        void testGetPublicScriptVersionList() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbGetPublicScriptVersionListV3Request req = new EsbGetPublicScriptVersionListV3Request();
            req.setScriptId(createdPublicScript.getScriptId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_PUBLIC_SCRIPT_VERSION_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0));
        }
    }

    @Nested
    class PublicScriptUpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试更新公共脚本基础信息")
        void testUpdatePublicScriptBasic() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbUpdatePublicScriptBasicV3Req req = new EsbUpdatePublicScriptBasicV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setName(TestValueGenerator.generateUniqueStrValue("shell_script", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_PUBLIC_SCRIPT_BASIC)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getScriptId()))
                .body("data.name", equalTo(req.getName()))
                .body("data.script_language", greaterThan(0))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L));
        }

        @Test
        @DisplayName("测试更新公共脚本版本信息")
        void testUpdatePublicScriptVersion() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbUpdatePublicScriptVersionV3Req req = new EsbUpdatePublicScriptVersionV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setScriptVersionId(createdPublicScript.getId());
            req.setContent(SHELL_SCRIPT_CONTENT_BASE64);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()))
                .body("data.name", notNullValue())
                .body("data.script_language", notNullValue())
                .body("data.content", equalTo(Base64Util.base64DecodeContentToStr(req.getContent())))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L))
                .body("data.version", notNullValue())
                .body("data.status", notNullValue());
        }
    }

    @Nested
    class PublicScriptOperationTest {
        // 上线、下线等操作
        @Test
        @DisplayName("测试上线公共脚本版本")
        void testPublishPublicScriptVersion() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbManagePublicScriptVersionV3Req req = new EsbManagePublicScriptVersionV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setScriptVersionId(createdPublicScript.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.PUBLISH_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(JobResourceStatusEnum.ONLINE.getValue()))
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()));
        }

        @Test
        @DisplayName("测试禁用公共脚本版本")
        void testDisablePublicScriptVersion() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbManagePublicScriptVersionV3Req req = new EsbManagePublicScriptVersionV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setScriptVersionId(createdPublicScript.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.PUBLISH_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(JobResourceStatusEnum.ONLINE.getValue()))
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()));

            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DISABLE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data.status", equalTo(JobResourceStatusEnum.DISABLED.getValue()))
                .body("data.id", equalTo(req.getScriptVersionId().intValue()))
                .body("data.script_id", equalTo(req.getScriptId()));
        }

        @Test
        @DisplayName("测试禁用公共脚本版本-不支持操作")
        void testDisablePublicScriptVersionNotSupport() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbManagePublicScriptVersionV3Req req = new EsbManagePublicScriptVersionV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setScriptVersionId(createdPublicScript.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DISABLE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.UNSUPPORTED_OPERATION));
        }
    }

    @Nested
    class PublicScriptDeleteTest {
        @Test
        @DisplayName("测试公共脚本删除")
        void testDeletePublicScript() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            EsbDeletePublicScriptV3Req req = new EsbDeletePublicScriptV3Req();
            req.setScriptId(createdPublicScript.getScriptId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_PUBLIC_SCRIPT)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }

        @Test
        @DisplayName("测试公共脚本版本删除")
        void testDeletePublicScriptVersion() {
            EsbScriptVersionDetailV3DTO createdPublicScript = Operations.createPublicScript();
            createdPublicScriptList.add(createdPublicScript);
            EsbDeletePublicScriptVersionV3Req req = new EsbDeletePublicScriptVersionV3Req();
            req.setScriptId(createdPublicScript.getScriptId());
            req.setScriptVersionId(createdPublicScript.getId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_PUBLIC_SCRIPT_VERSION)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }
}
