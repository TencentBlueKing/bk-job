package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 获取步骤详情 API 测试
 */
@DisplayName("v3.EsbGetStepInstanceDetailV3ResourceAPITest")
public class EsbGetStepInstanceDetailV3ResourceAPITest extends BaseTest {

    @AfterAll
    static void tearDown() {

    }

    @Nested
    class GetStepInstanceDetailTest {
        @Test
        @DisplayName("测试正常获取步骤详情")
        void testGetInstanceDetail() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            // 2.获取步骤详情
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .param("step_instance_id", esbJobExecuteV3DTO.getStepInstanceId())
                .get(APIV3Urls.GET_STEP_INSTANCE_DETAIL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(esbJobExecuteV3DTO.getStepInstanceId()))
                .body("data.type", equalTo(1))
                .body("data.script_info", notNullValue())
                .body("data.file_info", nullValue());
        }

        @Test
        @DisplayName("测试任务实例ID异常")
        void testGetInstanceDetailWithWrongJobInstanceId() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            // 2.获取步骤详情
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", -1)
                .param("step_instance_id", esbJobExecuteV3DTO.getStepInstanceId())
                .get(APIV3Urls.GET_STEP_INSTANCE_DETAIL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试步骤实例ID异常")
        void testGetInstanceDetailWithWrongStepInstanceId() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            // 2.获取步骤详情
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .param("step_instance_id", -1)
                .get(APIV3Urls.GET_STEP_INSTANCE_DETAIL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
