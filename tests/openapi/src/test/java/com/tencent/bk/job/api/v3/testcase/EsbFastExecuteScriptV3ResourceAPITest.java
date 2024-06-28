package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.RollingModeEnum;
import com.tencent.bk.job.api.constant.ScriptTypeEnum;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbDynamicGroupDTO;
import com.tencent.bk.job.api.v3.model.EsbRollingConfigDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.HostDTO;
import com.tencent.bk.job.api.v3.model.request.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.api.v3.model.request.EsbFastExecuteScriptV3Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * 快速执行脚本 API 测试
 */
@DisplayName("v3.FastExecuteScriptResourceAPITest")
class EsbFastExecuteScriptV3ResourceAPITest extends BaseTest {

    @Nested
    class fastExecuteScriptTest {
        @Test
        @DisplayName("快速执行脚本")
        void testFastExecuteScript() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .body("data", notNullValue())
                .body("data.job_instance_name", notNullValue())
                .body("data.job_instance_id", greaterThan(0L))
                .body("data.step_instance_id", greaterThan(0L));
        }

        @Test
        @DisplayName("快速执行脚本-账号异常")
        void testFastExecuteScriptWithWrongAccount() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            // 账号、账号别名不能同时为空
            req.setAccountAlias(null);
            req.setAccountId(null);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));

            req.setAccountAlias(null);
            req.setAccountId(0L); //账号ID必须大于0
            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行脚本-脚本信息异常")
        void testFastExecuteScriptWithWrongScriptInfo() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 脚本、脚本版本、脚本内容至少要有一个不为空
            req.setContent(null);
            req.setScriptId(null);
            req.setScriptVersionId(null);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行脚本-脚本类型异常")
        void testFastExecuteScriptWithWrongScriptType() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            // content不为空时，脚本类型必须有效
            req.setScriptLanguage(10);
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行脚本-超时时间异常")
        void testFastExecuteScriptWithWrongTimeout() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            // timeout [1, 259200]
            req.setTimeout(-1);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行脚本-目标服务器异常")
        void testFastExecuteScriptWithWrongTargetServer() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            // "ips", "hostIds", "dynamicGroups", "topoNodes"至少一个有效
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
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
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
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
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
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
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
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
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }

        @Test
        @DisplayName("快速执行脚本-滚动策略异常")
        void testFastExecuteScriptWithWrongRollingConfig() {
            EsbFastExecuteScriptV3Request req = new EsbFastExecuteScriptV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            req.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<HostDTO> ips = new ArrayList<>();
            ips.add(TestProps.HOST_1_DEFAULT_BIZ);
            ips.add(TestProps.HOST_2_DEFAULT_BIZ);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            // 滚动执行策略为空
            rollingConfig.setMode(null);
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_EXECUTE_SCRIPT)
                .then()
                .spec(ApiUtil.failResponseSpec(ErrorCode.BAD_REQUEST));
        }
    }
}
