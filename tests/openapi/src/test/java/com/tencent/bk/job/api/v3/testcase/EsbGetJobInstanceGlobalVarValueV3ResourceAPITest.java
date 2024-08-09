package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbGetJobInstanceGlobalVarValueV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 获取作业实例全局变量的值 API 测试
 */
@DisplayName("v3.EsbGetJobInstanceGlobalVarValueV3ResourceAPITest")
public class EsbGetJobInstanceGlobalVarValueV3ResourceAPITest extends BaseTest {
    @Nested
    class getJobInstanceGlobalVarValueTest {
        @Test
        @DisplayName("测试获取作业实例全局变量的值")
        void testGetInstanceDetail() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbGetJobInstanceGlobalVarValueV3Request req = new EsbGetJobInstanceGlobalVarValueV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_INSTANCE_GLOBAL_VAR_VALUE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.job_instance_id", greaterThan(0L))
                .body("data.step_instance_var_list", notNullValue());
        }

        @Test
        @DisplayName("测试获取作业实例全局变量的值-作业实例ID异常")
        void testGetInstanceDetailWithWrongJobInstanceId() {
            EsbGetJobInstanceGlobalVarValueV3Request req = new EsbGetJobInstanceGlobalVarValueV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 不能为null
            req.setTaskInstanceId(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_GLOBAL_VAR_VALUE, req, ErrorCode.BAD_REQUEST);
            // 必须大于0
            req.setTaskInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_GLOBAL_VAR_VALUE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试获取作业实例全局变量的值-作业实例ID异常(get)")
        void testGetInstanceDetailWithWrongStepInstanceIdUsingGet() {
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", ResourceScopeTypeEnum.BIZ.getValue())
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", 0L)
                .get(APIV3Urls.GET_JOB_INSTANCE_GLOBAL_VAR_VALUE)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
