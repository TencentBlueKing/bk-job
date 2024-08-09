package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbGlobalVarV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbExecuteJobV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 执行作业方案 API 测试
 */
@DisplayName("v3.ExecuteJobPlanResourceAPITest")
class EsbExecuteJobPlanV3ResourceAPITest extends BaseTest {

    @Nested
    class ExecuteJobPlanCreateTest {
        @Test
        @DisplayName("执行作业方案")
        void testExecuteJobPlan() {
            EsbExecuteJobV3Request req = new EsbExecuteJobV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskId(TestProps.DEFAULT_TADK_PLAN_ID);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.EXECUTE_JOB_PLAN)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.job_instance_name", notNullValue())
                .body("data.job_instance_id", greaterThan(0L));
        }

        @Test
        @DisplayName("执行作业方案-方案ID异常")
        void testExecuteJobPlanWithWrongPlanId() {
            EsbExecuteJobV3Request req = new EsbExecuteJobV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskId(null);//不能为null
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.EXECUTE_JOB_PLAN)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            req.setTaskId(0L);//需大于0
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.EXECUTE_JOB_PLAN)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("执行作业方案-全局变量异常")
        void testExecuteJobPlanWithWrongGlobalVar() {
            EsbExecuteJobV3Request req = new EsbExecuteJobV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskId(TestProps.DEFAULT_TADK_PLAN_ID);
            // 全局变量ID、名称至少一个有效
            List<EsbGlobalVarV3DTO> globalVars = new ArrayList<>();
            globalVars.add(new EsbGlobalVarV3DTO());
            req.setGlobalVars(globalVars);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.EXECUTE_JOB_PLAN)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
