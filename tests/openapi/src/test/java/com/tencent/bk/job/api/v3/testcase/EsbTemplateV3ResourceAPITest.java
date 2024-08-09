package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.request.EsbGetTemplateListV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 本地文件 API 测试
 */
@DisplayName("v3.TemplateResourceAPITest")
class EsbTemplateV3ResourceAPITest extends BaseTest {

    @Nested
    class PlanGetTest {
        @Test
        @DisplayName("测试获取作业列表")
        void testGetTemplateList() {
            EsbGetTemplateListV3Request req = new EsbGetTemplateListV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setLength(10);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_TEMPLATE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0))
                .body("data.data", notNullValue());
        }
    }
}
