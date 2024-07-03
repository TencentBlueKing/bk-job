package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.LogTypeEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbIpLogsV3DTO;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbBatchGetJobInstanceIpLogV3Request;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 根据ip批量查询作业执行日志 API 测试
 */
@DisplayName("v3.EsbGetJobInstanceLogV3ResourceAPITest")
public class EsbBatchGetJobInstanceIpLogV3ResourceAPITest extends BaseTest {

    @Nested
    class GetJobInstanceScriptLogTest {
        @Test
        @DisplayName("测试根据主机列表批量查询脚本执行日志")
        void testGetJobInstanceScriptLog() {
            // 脚本执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbBatchGetJobInstanceIpLogV3Request req = new EsbBatchGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            List<Long> hostIdList = new ArrayList<>();
            hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            req.setHostIdList(hostIdList);

            // 获取脚本执行日志
            EsbIpLogsV3DTO esbIpLogsV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbIpLogsV3DTO>>() {
                })
                .getData();
            assertThat(esbIpLogsV3DTO).isNotNull();
            assertThat(esbIpLogsV3DTO.getTaskInstanceId()).isEqualTo(req.getTaskInstanceId());
            assertThat(esbIpLogsV3DTO.getStepInstanceId()).isEqualTo(req.getStepInstanceId());
            assertThat(esbIpLogsV3DTO.getLogType()).isEqualTo(LogTypeEnum.SCRIPT.getValue());
            // 作业可能还没执行，没有具体日志
            // assertThat(esbIpLogsV3DTO.getScriptTaskLogs()).isNotNull();
        }

        @Test
        @DisplayName("测试根据主机列表批量查询脚本执行日志-作业ID异常")
        void testGetJobInstanceScriptLogWithWrongTaskId() {
            // 脚本执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbBatchGetJobInstanceIpLogV3Request req = new EsbBatchGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(null);//为null
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            List<Long> hostIdList = new ArrayList<>();
            hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            req.setHostIdList(hostIdList);

            // 获取脚本执行日志
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            req.setTaskInstanceId(0L);//ID必须大于0
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试根据主机列表批量查询脚本执行日志-步骤ID异常")
        void testGetJobInstanceScriptLogWithWrongStepId() {
            // 脚本执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbBatchGetJobInstanceIpLogV3Request req = new EsbBatchGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(null);//为null
            List<Long> hostIdList = new ArrayList<>();
            hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            req.setHostIdList(hostIdList);

            // 获取脚本执行日志
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
            req.setStepInstanceId(0L);//ID必须大于0
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("测试根据主机列表批量查询脚本执行日志-主机异常")
        void testGetJobInstanceScriptLogWithWrongHost() {
            // 脚本执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbBatchGetJobInstanceIpLogV3Request req = new EsbBatchGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            req.setHostIdList(null);
            req.setIpList(null);

            // 获取脚本执行日志
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    class GetJobInstanceFileLogTest {
        @Test
        @DisplayName("测试根据主机列表批量查询文件分发执行日志")
        void testGetJobInstanceFileLog() {
            // 文件分发
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastTransferFileTask();
            EsbBatchGetJobInstanceIpLogV3Request req = new EsbBatchGetJobInstanceIpLogV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            List<Long> hostIdList = new ArrayList<>();
            hostIdList.add(TestProps.HOST_1_DEFAULT_BIZ.getHostId());
            req.setHostIdList(hostIdList);

            // 获取文件分发执行日志
            EsbIpLogsV3DTO esbIpLogsV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.BATCH_GET_JOB_INSTANCE_IP_LOG)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbIpLogsV3DTO>>() {
                })
                .getData();
            assertThat(esbIpLogsV3DTO).isNotNull();
            assertThat(esbIpLogsV3DTO.getTaskInstanceId()).isEqualTo(req.getTaskInstanceId());
            assertThat(esbIpLogsV3DTO.getStepInstanceId()).isEqualTo(req.getStepInstanceId());
            assertThat(esbIpLogsV3DTO.getLogType()).isEqualTo(LogTypeEnum.FILE.getValue());
            // 作业可能还没执行，没有具体日志
            // assertThat(esbIpLogsV3DTO.getScriptTaskLogs()).isNotNull();
        }
    }

}
