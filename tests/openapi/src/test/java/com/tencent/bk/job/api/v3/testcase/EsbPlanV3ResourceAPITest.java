package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.request.EsbGetPlanDetailV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbGetPlanListV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 本地文件 API 测试
 */
@DisplayName("v3.LocalFileResourceAPITest")
class EsbPlanV3ResourceAPITest extends BaseTest {

    @Nested
    class PlanGetTest {
        @Test
        @DisplayName("测试获取执行方案列表")
        void testGetPlanList() {
            EsbGetPlanListV3Request req = new EsbGetPlanListV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setLength(10);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_PLAN_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0))
                .body("data.data", notNullValue());
        }

        @Test
        @DisplayName("测试获取执行方案详情")
        void testGetPlanDetail() {
            Long planId = Operations.getTaskPlanId();
            EsbGetPlanDetailV3Request req = new EsbGetPlanDetailV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setPlanId(planId);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_PLAN_DETAIL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(planId.intValue()))
                .body("data.name", notNullValue())
                .body("data.creator", notNullValue())
                .body("data.create_time", notNullValue())
                .body("data.last_modify_user", notNullValue())
                .body("data.last_modify_time", notNullValue())
                .body("data.step_list", notNullValue())
                .body("data.step_list.size()", greaterThan(0));
        }
        
        @Test
        @DisplayName("测试获取执行方案详情-执行方案ID异常")
        void testGetPlanDetailWithWrongPlanId() {
            Long planId = Operations.getTaskPlanId();
            EsbGetPlanDetailV3Request req = new EsbGetPlanDetailV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setPlanId(null);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_PLAN_DETAIL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            req.setPlanId(0L);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_PLAN_DETAIL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
