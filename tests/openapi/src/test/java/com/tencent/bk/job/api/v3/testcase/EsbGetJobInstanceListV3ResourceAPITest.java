package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.DateUtils;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.request.EsbGetJobInstanceListV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 查询作业实例列表（执行历史） API 测试
 */
@DisplayName("v3.EsbGetJobInstanceListV3ResourceAPITest")
public class EsbGetJobInstanceListV3ResourceAPITest extends BaseTest {
    @Nested
    class getJobInstanceListTest {
        @Test
        @DisplayName("查询作业实例列表（执行历史）")
        void testGetInstanceList() {
            EsbGetJobInstanceListV3Request req = new EsbGetJobInstanceListV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCreateTimeStart(DateUtils.currentTimeMillis() - 24 * 60 * 60 * 1000);
            req.setCreateTimeEnd(DateUtils.currentTimeMillis());
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_JOB_INSTANCE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());
        }

        @Test
        @DisplayName("查询作业实例列表（执行历史）-get请求")
        void testGetInstanceListUsingGet() {
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .param("bk_scope_type", ResourceScopeTypeEnum.BIZ.getValue())
                .param("bk_scope_id", TestProps.DEFAULT_BIZ)
                .param("create_time_start", DateUtils.currentTimeMillis() - 24 * 60 * 60 * 1000)
                .param("create_time_end", DateUtils.currentTimeMillis())
                .get(APIV3Urls.GET_JOB_INSTANCE_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue());
        }

        @Test
        @DisplayName("查询作业实例列表（执行历史）- 任务类型异常")
        void testGetInstanceListWithWrongTaskType() {
            EsbGetJobInstanceListV3Request req = new EsbGetJobInstanceListV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setCreateTimeStart(DateUtils.currentTimeMillis() - 24 * 60 * 60 * 1000);
            req.setCreateTimeEnd(DateUtils.currentTimeMillis());
            // 类型类型异常
            req.setTaskType(-1);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_LIST, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("查询作业实例列表（执行历史）- 任务创建时间异常")
        void testGetInstanceListWithWrongTime() {
            EsbGetJobInstanceListV3Request req = new EsbGetJobInstanceListV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 开始结束时间不能相同
            req.setCreateTimeStart(DateUtils.currentTimeMillis());
            req.setCreateTimeEnd(DateUtils.currentTimeMillis());
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_LIST, req, ErrorCode.ILLEGAL_PARAM);

            // 开始结束时间间隔不能大于30天
            req.setCreateTimeStart(DateUtils.currentTimeMillis() - 31 * 24 * 60 * 60 * 1000);
            ApiUtil.assertPostErrorResponse(APIV3Urls.GET_JOB_INSTANCE_LIST, req, ErrorCode.ILLEGAL_PARAM);
        }
    }

}
