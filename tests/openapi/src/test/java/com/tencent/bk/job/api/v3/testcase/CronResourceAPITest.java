package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.CronStatusEnum;
import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.util.TestValueGenerator;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbCronInfoV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbDeleteCronV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbGetCronDetailV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbGetCronListV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbSaveCronV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbUpdateCronStatusV3Request;
import io.restassured.common.mapper.TypeRef;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 定时作业管理 API 测试
 */
@DisplayName("v3.CronResourceAPITest")
class CronResourceAPITest extends BaseTest {

    private static final List<EsbCronInfoV3DTO> createdCronList = new ArrayList<>();

    @AfterAll
    static void tearDown() {
        // 清理测试数据
        if (CollectionUtils.isNotEmpty(createdCronList)) {
            createdCronList.forEach(cron -> {
                // 清理定时任务
                EsbDeleteCronV3Request req = new EsbDeleteCronV3Request();
                req.setScopeId(cron.getScopeId());
                req.setScopeType(cron.getScopeType());
                req.setId(cron.getId());
                Operations.deleteCron(req);
            });
        }
    }

    @Nested
    class CronCreateTest {
        @Test
        @DisplayName("测试定时作业正常创建")
        void testCreateCron() {
            Long planId = Operations.getTaskPlanId();
            if (planId == null) {
                return;
            }
            EsbSaveCronV3Request req = new EsbSaveCronV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setName(TestValueGenerator.generateUniqueStrValue("cron_task", 50));
            req.setCronExpression("3 * * * 3");
            req.setPlanId(planId);

            EsbCronInfoV3DTO createdCron =
                given()
                    .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.SAVE_CRON)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", greaterThan(0))
                    .body("data.name", equalTo(req.getName()))
                    .body("data.status", equalTo(CronStatusEnum.STOPPING.getStatus()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()))
                    .body("data.expression", equalTo(req.getCronExpression()))
                    .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.create_time", greaterThan(0))
                    .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                    .body("data.last_modify_time", greaterThan(0))
                    .extract()
                    .body()
                    .as(new TypeRef<EsbResp<EsbCronInfoV3DTO>>() {
                    })
                    .getData();

            createdCronList.add(createdCron);
        }

        @Test
        @DisplayName("创建定时作业异常场景测试")
        void givenInvalidCreateCronThenFail() {
            Long planId = Operations.getTaskPlanId();
            if (planId == null) {
                return;
            }
            EsbSaveCronV3Request req = new EsbSaveCronV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setName(TestValueGenerator.generateUniqueStrValue("cron_task", 50));
            // 不是Unix格式的表达式
            req.setCronExpression("* 0/5 * * * * *");
            req.setPlanId(planId);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.SAVE_CRON)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON));

            // 执行方案id为null
            req.setPlanId(null);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.SAVE_CRON)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // 定时作业名称为null
            req.setName(null);
            req.setPlanId(planId);
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.SAVE_CRON)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }

    @Nested
    class CronGetTest {
        @Test
        @DisplayName("测试获取定时作业列表")
        void testGetCronList() {
            EsbCronInfoV3DTO createdCron = Operations.createCron();
            if (createdCron == null) {
                return;
            }
            createdCronList.add(createdCron);
            EsbGetCronListV3Request req = new EsbGetCronListV3Request();
            req.setScopeId(createdCron.getScopeId());
            req.setScopeType(createdCron.getScopeType());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_CRON_LIST)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.total", greaterThan(0));
        }

        @Test
        @DisplayName("测试获取定时作业详情")
        void testGetCronDetail() {
            EsbCronInfoV3DTO createdCron = Operations.createCron();
            if (createdCron == null) {
                return;
            }
            createdCronList.add(createdCron);
            EsbGetCronDetailV3Request req = new EsbGetCronDetailV3Request();
            req.setScopeId(createdCron.getScopeId());
            req.setScopeType(createdCron.getScopeType());
            req.setId(createdCron.getId());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.GET_CRON_DETAIL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", greaterThan(0))
                .body("data.name", equalTo(createdCron.getName()))
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()))
                .body("data.expression", equalTo(createdCron.getCronExpression()))
                .body("data.job_plan_id", equalTo(createdCron.getPlanId().intValue()))
                .body("data.creator", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.create_time", greaterThan(0))
                .body("data.last_modify_user", equalTo(TestProps.DEFAULT_TEST_USER))
                .body("data.last_modify_time", greaterThan(0));
        }
    }

    @Nested
    class CronUpdateTest {
        // 更新操作
        @Test
        @DisplayName("测试更新定时任务状态")
        void testUpdateCronStatus() {
            EsbCronInfoV3DTO createdCron = Operations.createCron();
            if (createdCron == null) {
                return;
            }
            createdCronList.add(createdCron);
            EsbUpdateCronStatusV3Request req = new EsbUpdateCronStatusV3Request();
            req.setScopeId(createdCron.getScopeId());
            req.setScopeType(createdCron.getScopeType());
            req.setId(createdCron.getId());
            // 启用
            req.setStatus(CronStatusEnum.RUNNING.getStatus());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_CRON_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId().intValue()));

            // 停用
            req.setStatus(CronStatusEnum.STOPPING.getStatus());
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.UPDATE_CRON_STATUS)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId().intValue()));
        }

        // 更新定时任务
        @Test
        @DisplayName("测试更新定时任务")
        void testUpdateCron() {
            EsbCronInfoV3DTO createdCron = Operations.createCron();
            if (createdCron == null) {
                return;
            }
            createdCronList.add(createdCron);

            EsbSaveCronV3Request req = new EsbSaveCronV3Request();
            req.setId(createdCron.getId());
            req.setScopeId(createdCron.getScopeId());
            req.setScopeType(createdCron.getScopeType());
            req.setName(TestValueGenerator.generateUniqueStrValue("cron_task", 50));
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                    .body(JsonUtil.toJson(req))
                    .post(APIV3Urls.SAVE_CRON)
                    .then()
                    .spec(ApiUtil.successResponseSpec())
                    .body("data", notNullValue())
                    .body("data.id", equalTo(req.getId().intValue()))
                    .body("data.name", equalTo(req.getName()))
                    .body("data.bk_scope_type", equalTo(req.getScopeType()))
                    .body("data.bk_scope_id", equalTo(req.getScopeId()));

            req.setName(null);
            req.setCronExpression("* * * * *");
            given().spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.SAVE_CRON)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.id", equalTo(req.getId().intValue()))
                .body("data.expression", equalTo(req.getCronExpression()))
                .body("data.bk_scope_type", equalTo(req.getScopeType()))
                .body("data.bk_scope_id", equalTo(req.getScopeId()));
        }
    }

    @Nested
    class CronDeleteTest {
        @Test
        @DisplayName("测试定时任务删除")
        void testDeleteCron() {
            EsbCronInfoV3DTO createdScript = Operations.createCron();
            EsbDeleteCronV3Request req = new EsbDeleteCronV3Request();
            req.setScopeId(createdScript.getScopeId());
            req.setScopeType(createdScript.getScopeType());
            req.setId(createdScript.getId());

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.DELETE_CRON)
                .then()
                .spec(ApiUtil.successResponseSpec());
        }
    }
}
