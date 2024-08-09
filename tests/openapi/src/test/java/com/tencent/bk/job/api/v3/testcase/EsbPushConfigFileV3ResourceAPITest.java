package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbDynamicGroupDTO;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.api.v3.model.request.EsbIpDTO;
import com.tencent.bk.job.api.v3.model.request.EsbPushConfigFileV3Request;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 配置文件分发 API 测试
 */
@DisplayName("v3.PushConfigFileResourceAPITest")
class EsbPushConfigFileV3ResourceAPITest extends BaseTest {

    @Nested
    class PushConfigFileTest {
        @Test
        @DisplayName("分发配置文件")
        void testPushConfigFile() {
            EsbPushConfigFileV3Request req = new EsbPushConfigFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            List<EsbPushConfigFileV3Request.EsbConfigFileDTO> fileList = new ArrayList<>();
            EsbPushConfigFileV3Request.EsbConfigFileDTO configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("test.txt");
            configFileDTO.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            req.setTargetServer(Operations.buildCommonTargetServer());
            EsbJobExecuteV3DTO jobExecuteV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.PUSH_CONFIG_FILE)
                .then()
                .spec(ApiUtil.successResponseSpec())
                .extract()
                .body()
                .as(new TypeRef<EsbResp<EsbJobExecuteV3DTO>>() {
                })
                .getData();
            assertThat(jobExecuteV3DTO).isNotNull();
            assertThat(jobExecuteV3DTO.getTaskInstanceId()).isGreaterThan(0L);
            assertThat(jobExecuteV3DTO.getStepInstanceId()).isGreaterThan(0L);
        }

        @Test
        @DisplayName("分发配置文件-账号异常")
        void testPushConfigFileWithWrongAccount() {
            EsbPushConfigFileV3Request req = new EsbPushConfigFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 账号、账号别名不能同时为空
            req.setAccountAlias(null);
            req.setAccountId(null);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            List<EsbPushConfigFileV3Request.EsbConfigFileDTO> fileList = new ArrayList<>();
            EsbPushConfigFileV3Request.EsbConfigFileDTO configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("test.txt");
            configFileDTO.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            req.setTargetServer(Operations.buildCommonTargetServer());
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            req.setAccountAlias(null);
            req.setAccountId(0L); //账号ID必须大于0
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("分发配置文件-目标路径异常")
        void testPushConfigFileWithWrongScriptInfo() {
            EsbPushConfigFileV3Request req = new EsbPushConfigFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            List<EsbPushConfigFileV3Request.EsbConfigFileDTO> fileList = new ArrayList<>();
            EsbPushConfigFileV3Request.EsbConfigFileDTO configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("test.txt");
            configFileDTO.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            req.setTargetServer(Operations.buildCommonTargetServer());
            // 目标路径为空
            req.setTargetPath("");
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("分发配置文件-目标服务器异常")
        void testPushConfigFileWithWrongTargetServer() {
            EsbPushConfigFileV3Request req = new EsbPushConfigFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            List<EsbPushConfigFileV3Request.EsbConfigFileDTO> fileList = new ArrayList<>();
            EsbPushConfigFileV3Request.EsbConfigFileDTO configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("test.txt");
            configFileDTO.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            // "ips", "hostIds", "dynamicGroups", "topoNodes"至少一个有效
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // hostId无效
            targetServer = new EsbServerV3DTO();
            List<Long> hostIds = new ArrayList<>();
            hostIds.add(-1L);
            targetServer.setHostIds(hostIds);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.EXECUTE_OBJECT_NOT_EXIST);

            // cloudId无效
            targetServer = new EsbServerV3DTO();
            List<EsbIpDTO> ips = new ArrayList<>();
            EsbIpDTO hostDTO = new EsbIpDTO();
            hostDTO.setBkCloudId(-1L);
            ips.add(hostDTO);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // dynamicGroupId无效
            targetServer = new EsbServerV3DTO();
            List<EsbDynamicGroupDTO> dynamicGroupDTOS = new ArrayList<>();
            EsbDynamicGroupDTO esbDynamicGroupDTO = new EsbDynamicGroupDTO();
            esbDynamicGroupDTO.setId(null);
            dynamicGroupDTOS.add(esbDynamicGroupDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // topo无效
            targetServer = new EsbServerV3DTO();
            List<EsbCmdbTopoNodeDTO> esbCmdbTopoNodeDTOS = new ArrayList<>();
            EsbCmdbTopoNodeDTO esbCmdbTopoNodeDTO = new EsbCmdbTopoNodeDTO();
            esbCmdbTopoNodeDTO.setId(null);
            esbCmdbTopoNodeDTO.setNodeType(null);
            esbCmdbTopoNodeDTOS.add(esbCmdbTopoNodeDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("分发配置文件-配置文件列表异常")
        void testPushConfigFileWithWrongFileSourceList() {
            EsbPushConfigFileV3Request req = new EsbPushConfigFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setTargetServer(Operations.buildCommonTargetServer());
            // 配置文件列表为null
            req.setFileList(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // 配置文件列表为空
            req.setFileList(new ArrayList<EsbPushConfigFileV3Request.EsbConfigFileDTO>());
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // 配置文件名称为空
            List<EsbPushConfigFileV3Request.EsbConfigFileDTO> fileList = new ArrayList<>();
            EsbPushConfigFileV3Request.EsbConfigFileDTO configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("");
            configFileDTO.setContent(TestProps.DEFAULT_SHELL_SCRIPT_CONTENT);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);

            // 配置文件内容为空
            fileList.clear();
            configFileDTO =
                new EsbPushConfigFileV3Request.EsbConfigFileDTO();
            configFileDTO.setFileName("test.txt");
            configFileDTO.setContent(null);
            fileList.add(configFileDTO);
            req.setFileList(fileList);
            ApiUtil.assertPostErrorResponse(APIV3Urls.PUSH_CONFIG_FILE, req, ErrorCode.BAD_REQUEST);
        }
    }
}
