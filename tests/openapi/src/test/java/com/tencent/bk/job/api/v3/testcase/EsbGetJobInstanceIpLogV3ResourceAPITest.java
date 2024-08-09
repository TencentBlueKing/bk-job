package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.LogTypeEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbGetJobInstanceIpLogV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 根据主机查询作业执行日志 API 测试
 */
@DisplayName("v3.EsbGetJobInstanceIpLogV3ResourceAPITest")
public class EsbGetJobInstanceIpLogV3ResourceAPITest extends BaseTest {
    @Nested
    class getJobInstanceIpLogTest {
        @Test
        @DisplayName("测试根据主机查询脚本执行日志")
        void testGetJobInstanceIpLogByScript() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbGetJobInstanceIpLogV3Request req = new EsbGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            req.setHostId(TestProps.HOST_1_DEFAULT_BIZ.getHostId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.log_type", equalTo(LogTypeEnum.SCRIPT.getValue()))
                //.body("data.log_content", notNullValue()) // 作业可能还没执行，没有具体日志
                .body("data.bk_host_id", equalTo(req.getHostId().intValue()));
        }

        @Test
        @DisplayName("测试根据主机查询文件分发执行日志")
        void testGetJobInstanceIpLogByFile() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastTransferFileTask();
            EsbGetJobInstanceIpLogV3Request req = new EsbGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            req.setHostId(TestProps.HOST_2_DEFAULT_BIZ.getHostId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.log_type", equalTo(LogTypeEnum.FILE.getValue()))
                //.body("data.file_logs", notNullValue()) // 作业可能还没执行，没有具体日志
                .body("data.bk_host_id", equalTo(req.getHostId().intValue()));
        }

        @Test
        @DisplayName("测试根据主机查询作业执行日志-作业实例ID异常")
        void testGetInstanceDetailWithWrongJobInstanceId() {
            EsbGetJobInstanceIpLogV3Request req = new EsbGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setStepInstanceId(1L);
            req.setHostId(TestProps.HOST_2_DEFAULT_BIZ.getHostId());
            // 不能为null
            req.setTaskInstanceId(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_IP_LOG, req, ErrorCode.BAD_REQUEST);
            // 必须大于0
            req.setTaskInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_IP_LOG, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试根据主机查询作业执行日志-作业步骤ID异常")
        void testGetInstanceDetailWithWrongStepId() {
            EsbGetJobInstanceIpLogV3Request req = new EsbGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(1L);
            req.setHostId(TestProps.HOST_2_DEFAULT_BIZ.getHostId());
            // 不能为null
            req.setStepInstanceId(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_IP_LOG, req, ErrorCode.BAD_REQUEST);
            // 必须大于0
            req.setStepInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_IP_LOG, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试根据主机查询作业执行日志-作业实例ID异常(get)")
        void testGetInstanceDetailWithWrongInstanceIdUsingGet() {
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", ResourceScopeTypeEnum.BIZ.getValue())
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("bk_host_id", TestProps.HOST_2_DEFAULT_BIZ.getHostId())
                .param("step_instance_id", 1L)
                .get(APIV3Urls.GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试根据主机查询作业执行日志-作业步骤ID异常(get)")
        void testGetInstanceDetailWithWrongStepIdUsingGet() {
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", ResourceScopeTypeEnum.BIZ.getValue())
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("bk_host_id", TestProps.HOST_2_DEFAULT_BIZ.getHostId())
                .param("job_instance_id", 1L)
                .get(APIV3Urls.GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
