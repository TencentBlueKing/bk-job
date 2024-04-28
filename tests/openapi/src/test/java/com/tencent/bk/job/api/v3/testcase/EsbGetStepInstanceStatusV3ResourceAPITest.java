package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.EsbStepInstanceStatusV3DTO;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 获取步骤状态 API 测试
 */
@DisplayName("v3.EsbGetStepInstanceStatusV3ResourceAPITest")
public class EsbGetStepInstanceStatusV3ResourceAPITest extends BaseTest {

    @AfterAll
    static void tearDown() {

    }

    @Nested
    class GetStepInstanceStatusTest {
        @Test
        @DisplayName("测试正常获取脚本步骤状态")
        void testGetScriptStepInstanceStatus() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            // 2.获取步骤详情
            EsbStepInstanceStatusV3DTO stepInstanceStatusV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .param("step_instance_id", esbJobExecuteV3DTO.getStepInstanceId())
                .param("max_host_num_per_group", 1)
                .get(APIV3Urls.GET_STEP_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbStepInstanceStatusV3DTO>>() {
                })
                .getData();
            assertThat(stepInstanceStatusV3DTO).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getId()).isEqualTo(esbJobExecuteV3DTO.getStepInstanceId());
            assertThat(stepInstanceStatusV3DTO.getExecuteCount()).isEqualTo(0);
            assertThat(stepInstanceStatusV3DTO.getType()).isEqualTo(1);
            assertThat(stepInstanceStatusV3DTO.getStatus()).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getStartTime()).isGreaterThan(0L);
            assertThat(stepInstanceStatusV3DTO.getTotalTime()).isGreaterThan(0L);
            assertThat(stepInstanceStatusV3DTO.getStepResultGroupList()).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getStepResultGroupList().size()).isEqualTo(1);
        }

        @Test
        @DisplayName("测试正常获取文件步骤状态")
        void testGetFileStepInstanceStatus() {
            // 1.触发一次执行
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastTransferFileTask();
            // 2.获取步骤详情
            EsbStepInstanceStatusV3DTO stepInstanceStatusV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", "biz")
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("job_instance_id", esbJobExecuteV3DTO.getTaskInstanceId())
                .param("step_instance_id", esbJobExecuteV3DTO.getStepInstanceId())
                .get(APIV3Urls.GET_STEP_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbStepInstanceStatusV3DTO>>() {
                })
                .getData();
            assertThat(stepInstanceStatusV3DTO).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getId()).isEqualTo(esbJobExecuteV3DTO.getStepInstanceId());
            assertThat(stepInstanceStatusV3DTO.getExecuteCount()).isEqualTo(0);
            assertThat(stepInstanceStatusV3DTO.getType()).isEqualTo(2);
            assertThat(stepInstanceStatusV3DTO.getStatus()).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getStartTime()).isGreaterThan(0L);
            assertThat(stepInstanceStatusV3DTO.getTotalTime()).isGreaterThan(0L);
            assertThat(stepInstanceStatusV3DTO.getStepResultGroupList()).isNotNull();
            assertThat(stepInstanceStatusV3DTO.getStepResultGroupList().size()).isEqualTo(1);
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
                .get(APIV3Urls.GET_STEP_INSTANCE_STATUS)
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
                .get(APIV3Urls.GET_STEP_INSTANCE_STATUS)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

}
