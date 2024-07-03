package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.TaskOperationEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbOperateJobInstanceV3Request;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 作业实例操作 API 测试
 */
@DisplayName("v3.EsbOperateJobInstanceV3ResourceAPITest")
public class EsbOperateJobInstanceV3ResourceAPITest extends BaseTest {

    @Nested
    class OperateJobInstanceTest {
        @Test
        @DisplayName("测试作业实例操作")
        void testOperateJobInstance() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbOperateJobInstanceV3Request req = new EsbOperateJobInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            req.setOperationCode(TaskOperationEnum.TERMINATE_JOB.getValue());

            EsbJobExecuteV3DTO jobExecuteV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.OPERATE_JOB_INSTANCE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
                })
                .getData();
            assertThat(jobExecuteV3DTO).isNotNull();
            assertThat(jobExecuteV3DTO.getTaskInstanceId()).isEqualTo(esbJobExecuteV3DTO.getTaskInstanceId());
        }

        @Test
        @DisplayName("测试作业实例操作-作业实例ID异常")
        void testOperateJobInstanceWithWrongInstanceId() {
            EsbOperateJobInstanceV3Request req = new EsbOperateJobInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 作业实例ID不能为null
            req.setTaskInstanceId(null);
            req.setOperationCode(TaskOperationEnum.TERMINATE_JOB.getValue());
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_JOB_INSTANCE, req, ErrorCode.BAD_REQUEST);
            // 作业实例ID需大于0
            req.setTaskInstanceId(0L);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_JOB_INSTANCE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("测试作业实例操作-操作类型异常")
        void testOperateJobInstanceWithWrongOperate() {
            EsbJobExecuteV3DTO esbJobExecuteV3DTO = Operations.fastExecuteScriptTask();
            EsbOperateJobInstanceV3Request req = new EsbOperateJobInstanceV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setTaskInstanceId(esbJobExecuteV3DTO.getTaskInstanceId());
            // 操作类型不能为null
            req.setOperationCode(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_JOB_INSTANCE, req, ErrorCode.BAD_REQUEST);
            // 操作类型只能是1
            req.setOperationCode(0);
            ApiUtil.assertPostErrorResponse(APIV3Urls.OPERATE_JOB_INSTANCE, req, ErrorCode.BAD_REQUEST);
        }
    }

}
