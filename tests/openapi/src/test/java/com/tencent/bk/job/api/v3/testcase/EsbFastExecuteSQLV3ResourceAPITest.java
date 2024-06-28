package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbDynamicGroupDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.request.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.api.v3.model.request.EsbFastExecuteSQLV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 快速执行SQL脚本 API 测试
 */
@DisplayName("v3.FastExecuteSQLResourceAPITest")
class EsbFastExecuteSQLV3ResourceAPITest extends BaseTest {

    @Nested
    class fastExecuteSQLTest {
        @Test
        @DisplayName("快速执行SQL脚本")
        void testFastExecuteSQL() {
            EsbFastExecuteSQLV3Request req = new EsbFastExecuteSQLV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SQL_SCRIPT_CONTENT);
            req.setDbAccountId(TestProps.DEFAULT_DB_ACCOUNT_ID);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.job_instance_name", notNullValue())
                .body("data.job_instance_id", greaterThan(0L))
                .body("data.step_instance_id", greaterThan(0L));
        }

        @Test
        @DisplayName("快速执行SQL脚本-账号异常")
        void testFastExecuteSQLWithWrongAccount() {
            EsbFastExecuteSQLV3Request req = new EsbFastExecuteSQLV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SQL_SCRIPT_CONTENT);
            // db账号id必须大于0
            req.setDbAccountId(0L);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行SQL脚本-脚本信息异常")
        void testFastExecuteSQLWithWrongScriptInfo() {
            EsbFastExecuteSQLV3Request req = new EsbFastExecuteSQLV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 脚本、脚本版本、脚本内容至少要有一个不为空
            req.setContent(null);
            req.setScriptId(null);
            req.setScriptVersionId(null);
            req.setDbAccountId(TestProps.DEFAULT_DB_ACCOUNT_ID);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行SQL脚本-超时时间异常")
        void testFastExecuteSQLWithWrongTimeout() {
            EsbFastExecuteSQLV3Request req = new EsbFastExecuteSQLV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SQL_SCRIPT_CONTENT);
            req.setDbAccountId(TestProps.DEFAULT_DB_ACCOUNT_ID);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            // timeout [1, 259200]
            req.setTimeout(-1);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行SQL脚本-目标服务器异常")
        void testFastExecuteSQLWithWrongTargetServer() {
            EsbFastExecuteSQLV3Request req = new EsbFastExecuteSQLV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SQL_SCRIPT_CONTENT);
            req.setDbAccountId(TestProps.DEFAULT_DB_ACCOUNT_ID);
            req.setName(Operations.buildJobName());
            // "ips", "hostIds", "dynamicGroups", "topoNodes"至少一个有效
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            req.setTargetServer(targetServer);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // hostId无效
            targetServer = new EsbServerV3DTO();
            List<Long> hostIds = new ArrayList<>();
            hostIds.add(-1L);
            targetServer.setHostIds(hostIds);
            req.setTargetServer(targetServer);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.EXECUTE_OBJECT_NOT_EXIST));

            // cloudId无效
            targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            HostDTO hostDTO = TestProps.HOST_1_DEFAULT_BIZ;
            hostDTO.setBkCloudId(-1L);
            ips.add(hostDTO);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // dynamicGroupId无效
            targetServer = new EsbServerV3DTO();
            List<EsbDynamicGroupDTO> dynamicGroupDTOS = new ArrayList<>();
            EsbDynamicGroupDTO esbDynamicGroupDTO = new EsbDynamicGroupDTO();
            esbDynamicGroupDTO.setId(null);
            dynamicGroupDTOS.add(esbDynamicGroupDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            // topo无效
            targetServer = new EsbServerV3DTO();
            List<EsbCmdbTopoNodeDTO> esbCmdbTopoNodeDTOS = new ArrayList<>();
            EsbCmdbTopoNodeDTO esbCmdbTopoNodeDTO = new EsbCmdbTopoNodeDTO();
            esbCmdbTopoNodeDTO.setId(null);
            esbCmdbTopoNodeDTO.setNodeType(null);
            esbCmdbTopoNodeDTOS.add(esbCmdbTopoNodeDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SQL)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
