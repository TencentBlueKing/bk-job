package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.CredentialTypeEnum;
import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbCredentialSimpleInfoV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateOrUpdateCredentialV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetCredentialDetailV3Req;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 凭证 API 测试
 */
@DisplayName("v3.CredentialResourceAPITest")
class CredentialResourceAPITest extends BaseTest {

    private static final List<EsbCredentialSimpleInfoV3DTO> createdCredentialList = new ArrayList<>();

    @Nested
    class CredentialCreateTest {
        @Test
        @DisplayName("测试凭证正常创建")
        void testCreateCredential() {
            EsbCreateOrUpdateCredentialV3Req req = new EsbCreateOrUpdateCredentialV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setName(TestValueGenerator.generateUniqueStrValue("credential_name", 50));
            req.setDescription(TestValueGenerator.generateUniqueStrValue("credential_desc", 50));
            req.setType(CredentialTypeEnum.USERNAME_PASSWORD.name());
            req.setCredentialUsername("admin");
            req.setCredentialPassword("password");
            EsbCredentialSimpleInfoV3DTO createdCredential =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_CREDENTIAL)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbCredentialSimpleInfoV3DTO>>() {
                    })
                    .getData();
            createdCredentialList.add(createdCredential);
        }

        @Test
        @DisplayName("异常创建凭证测试-比如参数校验不通过")
        void givenInvalidCreateCredentialParamThenFail() {
            EsbCreateOrUpdateCredentialV3Req req = new EsbCreateOrUpdateCredentialV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setName(TestValueGenerator.generateUniqueStrValue("credential_name", 50));
            req.setDescription(TestValueGenerator.generateUniqueStrValue("credential_desc", 50));
            req.setType(CredentialTypeEnum.USERNAME_PASSWORD.name());
            req.setCredentialUsername("admin");
            req.setCredentialPassword("password");

            req.setName(null);
            // 名称为空，创建失败
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_CREDENTIAL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON));

            req.setName(TestValueGenerator.generateUniqueStrValue("credential_name", 50));
            req.setType(null);
            // 凭证类型为空，创建失败
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_CREDENTIAL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON));
        }
    }

    @Nested
    class CredentialGetTest {
        @Test
        @DisplayName("测试查询凭证-通过id查询详情")
        void testGetCredentialDetail() {
            EsbCredentialSimpleInfoV3DTO CredentialSimpleInfoV3DTO = Operations.createCredential();
            createdCredentialList.add(CredentialSimpleInfoV3DTO);
            EsbGetCredentialDetailV3Req req = new EsbGetCredentialDetailV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setId(CredentialSimpleInfoV3DTO.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_CREDENTIAL_DETAIL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId()))
                .body("data.name", notNullValue())
                .body("data.type", notNullValue())
                .body("data.description", notNullValue())
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L));
        }
    }

    @Nested
    class CredentialUpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试凭证修改")
        void testUpdateCredential() {
            EsbCredentialSimpleInfoV3DTO CredentialSimpleInfoV3DTO = Operations.createCredential();
            createdCredentialList.add(CredentialSimpleInfoV3DTO);
            EsbCreateOrUpdateCredentialV3Req req = new EsbCreateOrUpdateCredentialV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setId(CredentialSimpleInfoV3DTO.getId());
            req.setName(TestValueGenerator.generateUniqueStrValue("update_credential_name", 50));
            req.setType(CredentialTypeEnum.PASSWORD.name());
            req.setCredentialPassword("password");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_CREDENTIAL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(CredentialSimpleInfoV3DTO.getId()));
        }
    }
}
