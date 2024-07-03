package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.EsbJobInstanceStatusV3DTO;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 获取作业执行状态 API 测试
 */
@DisplayName("v3.EsbGetJobInstanceStatusV3ResourceAPITest")
public class EsbGetJobInstanceStatusV3ResourceAPITest extends BaseTest {

    @Nested
    class GetJobInstanceStatusTest {
        @Test
        @DisplayName("测试获取脚本执行状态")
        void testGetScriptJobInstanceStatus() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            // 2.获取步骤详情
            EsbJobInstanceStatusV3DTO JobInstanceStatusV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .get(APIV3Urls.GET_JOB_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbJobInstanceStatusV3DTO>>() {
                })
                .getData();
            assertThat(JobInstanceStatusV3DTO).isNotNull();
            assertThat(JobInstanceStatusV3DTO.getFinished()).isNotNull();
            assertThat(JobInstanceStatusV3DTO.getJobInstance().getId())
                .isEqualTo(esbJobExecuteV3DTO.getTaskInstanceId());
            assertThat(JobInstanceStatusV3DTO.getStepInstances()).isNotNull();
        }

        @Test
        @DisplayName("测试获取文件分发状态")
        void testGetFileJobInstanceStatus() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastTransferFileTask();
            // 2.获取步骤详情
            EsbJobInstanceStatusV3DTO JobInstanceStatusV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .get(APIV3Urls.GET_JOB_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbJobInstanceStatusV3DTO>>() {
                })
                .getData();
            assertThat(JobInstanceStatusV3DTO).isNotNull();
            assertThat(JobInstanceStatusV3DTO.getFinished()).isNotNull();
            assertThat(JobInstanceStatusV3DTO.getJobInstance().getId())
                .isEqualTo(esbJobExecuteV3DTO.getTaskInstanceId());
            assertThat(JobInstanceStatusV3DTO.getStepInstances()).isNotNull();
        }

        @Test
        @DisplayName("测试获取执行状态-作业ID异常")
        void testGetInstanceDetailWithWrongJobInstanceId() {
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .get(APIV3Urls.GET_JOB_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
