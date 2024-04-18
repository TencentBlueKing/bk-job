package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.FileSourceTypeEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbFileSourceSimpleInfoV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.api.v3.model.request.EsbGetFileSourceDetailV3Req;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 文件源 API 测试
 */
@DisplayName("v3.FileSourceResourceAPITest")
class FileSourceResourceAPITest extends BaseTest {

    private static final List<EsbFileSourceSimpleInfoV3DTO> createdFileSourceList = new ArrayList<>();

    @Nested
    class FileSourceCreateTest {
        @Test
        @DisplayName("测试文件源正常创建")
        void testCreateFileSource() {
            EsbCreateOrUpdateFileSourceV3Req req = new EsbCreateOrUpdateFileSourceV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCode(TestValueGenerator.generateUniqueStrValue("file_source_code", 50));
            req.setAlias(TestValueGenerator.generateUniqueStrValue("file_source_alias", 50));
            req.setType(FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name());
            Map<String, Object> accessParams = new HashMap<>();
            accessParams.put("base_url", "https://bkrepo.com");
            req.setAccessParams(accessParams);
            req.setCredentialId(TestValueGenerator.generateUniqueStrValue("credential_id", 50));
            EsbFileSourceSimpleInfoV3DTO createdFileSource =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.CREATE_FILE_SOURCE)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbFileSourceSimpleInfoV3DTO>>() {
                    })
                    .getData();
            createdFileSource.setCode(req.getCode());
            createdFileSourceList.add(createdFileSource);
        }

        @Test
        @DisplayName("异常创建文件源测试-比如参数校验不通过")
        void givenInvalidCreateFileSourceParamThenFail() {
            EsbCreateOrUpdateFileSourceV3Req req = new EsbCreateOrUpdateFileSourceV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCode(TestValueGenerator.generateUniqueStrValue("file_source_code", 50));
            req.setAlias(TestValueGenerator.generateUniqueStrValue("file_source_alias", 50));
            req.setType(FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name());
            Map<String, Object> accessParams = new HashMap<>();
            accessParams.put("base_url", "https://bkrepo.com");
            req.setAccessParams(accessParams);
            req.setCredentialId(TestValueGenerator.generateUniqueStrValue("credential_id", 50));

            req.setCode(null);
            // code为空，创建失败
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME));

            req.setCode("file_source_code_?<");
            // code有非法字符，创建失败
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            req.setCode(TestValueGenerator.generateUniqueStrValue("file_source_code", 50));
            // 别名为空，创建失败
            req.setAlias(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME));

            // 别名有非法字符，创建失败
            req.setAlias("alias|*");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 类型为空，创建失败
            req.setAlias(TestValueGenerator.generateUniqueStrValue("file_source_alias", 50));
            req.setType(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.CREATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME));
        }
    }

    @Nested
    class FileSourceGetTest {
        @Test
        @DisplayName("测试查询文件源-通过code查询详情")
        void testGetFileSourceDetail() {
            EsbFileSourceSimpleInfoV3DTO fileSourceSimpleInfoV3DTO = Operations.createFileSource();
            createdFileSourceList.add(fileSourceSimpleInfoV3DTO);
            EsbGetFileSourceDetailV3Req req = new EsbGetFileSourceDetailV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCode(fileSourceSimpleInfoV3DTO.getCode());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_FILE_SOURCE_DETAIL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", greaterThan(0))
                .body("data.code", equalTo(req.getCode()))
                .body("data.alias", notNullValue())
                .body("data.file_source_type_code", notNullValue())
                .body("data.is_public", notNullValue())
                .body("data.credential_id", notNullValue())
                .body("data.enable", notNullValue())
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()))
                .body("data.creator", notNullValue())
                .body("data.create_time", greaterThan(0L))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0L));
        }
    }

    @Nested
    class FileSourceUpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试文件源修改")
        void testUpdateFileSource() {
            EsbFileSourceSimpleInfoV3DTO fileSourceSimpleInfoV3DTO = Operations.createFileSource();
            createdFileSourceList.add(fileSourceSimpleInfoV3DTO);
            EsbCreateOrUpdateFileSourceV3Req req = new EsbCreateOrUpdateFileSourceV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCode(fileSourceSimpleInfoV3DTO.getCode());
            req.setType(FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name());
            req.setAlias(TestValueGenerator.generateUniqueStrValue("update_file_source_alias", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_FILE_SOURCE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(fileSourceSimpleInfoV3DTO.getId()));
        }
    }
}
