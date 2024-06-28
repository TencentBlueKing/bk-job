package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.request.EsbGenLocalFileUploadUrlV3Req;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 本地文件 API 测试
 */
@DisplayName("v3.LocalFileResourceAPITest")
class EsbLocalFileV3ResourceAPITest extends BaseTest {

    @Nested
    class LocalFileCreateTest {
        @Test
        @DisplayName("测试生成本地文件上传URL")
        void testCreateLocalFile() {
            EsbGenLocalFileUploadUrlV3Req req = new EsbGenLocalFileUploadUrlV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            List<String> list = new ArrayList<>();
            list.add("file1.txt");
            list.add("file2.txt");
            req.setFileNameList(list);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GENERATE_LOCAL_FILE_UPLOAD_URL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());
        }
        
        @Test
        @DisplayName("测试生成本地文件上传URL-文件名异常")
        void testCreateLocalFileWithWrongLocalFile() {
            EsbGenLocalFileUploadUrlV3Req req = new EsbGenLocalFileUploadUrlV3Req();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setFileNameList(new ArrayList<>());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GENERATE_LOCAL_FILE_UPLOAD_URL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
