package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.StepOperationEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbOperateStepInstanceV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 作业步骤操作 API 测试
 */
@DisplayName("v3.EsbOperateStepInstanceV3ResourceAPITest")
public class EsbOperateStepInstanceV3ResourceAPITest extends BaseTest {

    @Nested
    class OperateStepInstanceTest {
        @Test
        @DisplayName("测试作业步骤操作")
        void testOperateStepInstance() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbOperateStepInstanceV3Request req = new EsbOperateStepInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            req.setOperationCode(StepOperationEnum.RETRY_ALL_IP.getValue());

//            EsbJobExecuteV3DTO jobExecuteV3DTO = given()
//                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
//                .body(JsonUtil.toJson(req))
//                .post(APIV3Urls.OPERATE_STEP_INSTANCE)
//                .then()
//                .spec(ApiUtil.successResponseSpec())
//                .extract()
//                .body()
//                .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
//                })
//                .getData();
//            assertThat(jobExecuteV3DTO).isNotNull();
//            assertThat(jobExecuteV3DTO.getTaskInstanceId()).isEqualTo(esbJobExecuteV3DTO.getTaskInstanceId());
//            assertThat(jobExecuteV3DTO.getStepInstanceId()).isEqualTo(esbJobExecuteV3DTO.getTaskInstanceId());
            // 执行作业，执行步骤会立马进入执行中状态，不支持操作
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.UNSUPPORTED_OPERATION);
        }

        @Test
        @DisplayName("测试作业步骤操作-作业实例ID异常")
        void testOperateStepInstanceWithWrongInstanceId() {
            EsbOperateStepInstanceV3Request req = new EsbOperateStepInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setStepInstanceId(1L);
            // 作业实例ID不能为null
            req.setTaskInstanceId(null);
            req.setOperationCode(StepOperationEnum.RETRY_ALL_IP.getValue());
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
            // 作业实例ID需大于0
            req.setTaskInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试作业步骤操作-作业步骤ID异常")
        void testOperateStepInstanceWithWrongStepId() {
            EsbOperateStepInstanceV3Request req = new EsbOperateStepInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(1L);
            // 作业步骤ID不能为null
            req.setStepInstanceId(null);
            req.setOperationCode(StepOperationEnum.RETRY_ALL_IP.getValue());
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
            // 作业步骤ID需大于0
            req.setStepInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试作业步骤操作-操作类型异常")
        void testOperateStepInstanceWithWrongOperate() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbOperateStepInstanceV3Request req = new EsbOperateStepInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setStepInstanceId(esbJobExecuteV3DTO.getStepInstanceId());
            // 操作类型不能为null
            req.setOperationCode(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
            // 操作类型只能为StepOperationEnum的值
            req.setOperationCode(0);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_STEP_INSTANCE, req, ErrorCode.BAD_REQUEST);
        }
    }

}
