package com.tencent.bk.job.api.v3.testcase;

import com.tencent.bk.job.api.constant.ErrorCode;
import com.tencent.bk.job.api.constant.EsbTaskFileTypeEnum;
import com.tencent.bk.job.api.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.api.constant.RollingModeEnum;
import com.tencent.bk.job.api.model.EsbResp;
import com.tencent.bk.job.api.props.TestProps;
import com.tencent.bk.job.api.util.ApiUtil;
import com.tencent.bk.job.api.util.JsonUtil;
import com.tencent.bk.job.api.util.Operations;
import com.tencent.bk.job.api.v3.constants.APIV3Urls;
import com.tencent.bk.job.api.v3.model.EsbAccountV3BasicDTO;
import com.tencent.bk.job.api.v3.model.EsbDynamicGroupDTO;
import com.tencent.bk.job.api.v3.model.EsbFileSourceV3DTO;
import com.tencent.bk.job.api.v3.model.EsbJobExecuteV3DTO;
import com.tencent.bk.job.api.v3.model.EsbRollingConfigDTO;
import com.tencent.bk.job.api.v3.model.EsbServerV3DTO;
import com.tencent.bk.job.api.v3.model.request.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.api.v3.model.request.EsbFastTransferFileV3Request;
import com.tencent.bk.job.api.v3.model.request.EsbIpDTO;
import io.restassured.common.mapper.TypeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 快速分发文件 API 测试
 */
@DisplayName("v3.FastTransferFileResourceAPITest")
class EsbFastTransferFileV3ResourceAPITest extends BaseTest {

    @Nested
    class FastTransferFileTest {
        @Test
        @DisplayName("快速分发文件")
        void testFastTransferFile() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setFileSources(Operations.buildCommonFileSource());
            req.setTargetServer(Operations.buildCommonTargetServer());
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            rollingConfig.setMode(RollingModeEnum.IGNORE_ERROR.getValue());
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);

            EsbJobExecuteV3DTO jobExecuteV3DTO = given()
                .spec(ApiUtil.requestSpec(TestProps.DEFAULT_TEST_USER))
                .body(JsonUtil.toJson(req))
                .post(APIV3Urls.FAST_TRANSFER_FILE)
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
            assertThat(jobExecuteV3DTO.getTaskName()).isNotNull();
        }

        @Test
        @DisplayName("快速分发文件-账号异常")
        void testFastTransferFileWithWrongAccount() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // 账号、账号别名不能同时为空
            req.setAccountAlias(null);
            req.setAccountId(null);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setFileSources(Operations.buildCommonFileSource());
            req.setTargetServer(Operations.buildCommonTargetServer());
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            req.setAccountAlias(null);
            req.setAccountId(0L); //账号ID必须大于0
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("快速分发文件-目标路径异常")
        void testFastTransferFileWithWrongScriptInfo() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            // 目标路径为空
            req.setTargetPath("");
            req.setFileSources(Operations.buildCommonFileSource());
            req.setTargetServer(Operations.buildCommonTargetServer());
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("快速分发文件-超时时间异常")
        void testFastTransferFileWithWrongTimeout() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            // timeout [1, 259200]
            req.setTimeout(-1);
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setFileSources(Operations.buildCommonFileSource());
            req.setTargetServer(Operations.buildCommonTargetServer());
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("快速分发文件-目标服务器异常")
        void testFastTransferFileWithWrongTargetServer() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setFileSources(Operations.buildCommonFileSource());
            // "ips", "hostIds", "dynamicGroups", "topoNodes"至少一个有效
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // hostId无效
            targetServer = new EsbServerV3DTO();
            List<Long> hostIds = new ArrayList<>();
            hostIds.add(-1L);
            targetServer.setHostIds(hostIds);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.EXECUTE_OBJECT_NOT_EXIST);

            // cloudId无效
            targetServer = new EsbServerV3DTO();
            List<EsbIpDTO> ips = new ArrayList<>();
            EsbIpDTO EsbIpDTO = new EsbIpDTO();
            EsbIpDTO.setBkCloudId(-1L);
            ips.add(EsbIpDTO);
            targetServer.setIps(ips);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // dynamicGroupId无效
            targetServer = new EsbServerV3DTO();
            List<EsbDynamicGroupDTO> dynamicGroupDTOS = new ArrayList<>();
            EsbDynamicGroupDTO esbDynamicGroupDTO = new EsbDynamicGroupDTO();
            esbDynamicGroupDTO.setId(null);
            dynamicGroupDTOS.add(esbDynamicGroupDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // topo无效
            targetServer = new EsbServerV3DTO();
            List<EsbCmdbTopoNodeDTO> esbCmdbTopoNodeDTOS = new ArrayList<>();
            EsbCmdbTopoNodeDTO esbCmdbTopoNodeDTO = new EsbCmdbTopoNodeDTO();
            esbCmdbTopoNodeDTO.setId(null);
            esbCmdbTopoNodeDTO.setNodeType(null);
            esbCmdbTopoNodeDTOS.add(esbCmdbTopoNodeDTO);
            targetServer.setDynamicGroups(dynamicGroupDTOS);
            req.setTargetServer(targetServer);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("快速分发文件-滚动策略异常")
        void testFastTransferFileWithWrongRollingConfig() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            req.setFileSources(Operations.buildCommonFileSource());
            req.setTargetServer(Operations.buildCommonTargetServer());
            EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
            // 滚动执行策略为空
            rollingConfig.setMode(null);
            rollingConfig.setExpression("10%");
            req.setRollingConfig(rollingConfig);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }

        @Test
        @DisplayName("快速分发文件-源文件列表异常")
        void testFastTransferFileWithWrongFileSourceList() {
            EsbFastTransferFileV3Request req = new EsbFastTransferFileV3Request();
            req.setScopeId(String.valueOf(TestProps.DEFAULT_BIZ));
            req.setScopeType(ResourceScopeTypeEnum.BIZ.getValue());
            req.setAccountAlias(TestProps.DEFAULT_OS_ACCOUNT_ALIAS);
            req.setName(Operations.buildJobName());
            req.setTargetPath("/tmp/");
            // req.setFileSources(Operations.buildCommonFileSource());
            EsbServerV3DTO targetServer = new EsbServerV3DTO();
            List<Long> hostId = new ArrayList<>();
            hostId.add(TestProps.HOST_2_DEFAULT_BIZ.getHostId());
            targetServer.setHostIds(hostId);
            req.setTargetServer(targetServer);
            // 源文件列表为null
            req.setFileSources(null);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // 源文件列表为空
            req.setFileSources(new ArrayList<EsbFileSourceV3DTO>());
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // 服务器文件分发-文件列表不能为空
            List<EsbFileSourceV3DTO> fileSources = new ArrayList<>();
            EsbFileSourceV3DTO fileSource = new EsbFileSourceV3DTO();
            fileSource.setAccount(Operations.buildCommonFileSourceAccount());
            fileSource.setFiles(new ArrayList<>());
            fileSource.setServer(Operations.buildCommonSourceServer());
            fileSources.add(fileSource);
            req.setFileSources(fileSources);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // 服务器文件分发-账号别名、ID不能同时为空
            fileSources = new ArrayList<>();
            fileSource = new EsbFileSourceV3DTO();
            fileSource.setAccount(new EsbAccountV3BasicDTO());
            List<String> files = new ArrayList<>();
            files.add("/tmp/1.log");
            fileSource.setFiles(files);
            fileSource.setServer(Operations.buildCommonSourceServer());
            fileSources.add(fileSource);
            req.setFileSources(fileSources);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);

            // 服务器文件分发-源文件服务器错误（省略，跟目标服务器逻辑一样）

            // 源文件文件分发-文件源别名、code不能同时为空
            fileSources = new ArrayList<>();
            fileSource = new EsbFileSourceV3DTO();
            fileSource.setFileType(EsbTaskFileTypeEnum.FILE_SOURCE.getType());
            fileSource.setFileSourceId(null);
            fileSource.setFileSourceCode(null);
            fileSource.setFiles(files);
            fileSources.add(fileSource);
            req.setFileSources(fileSources);
            ApiUtil.assertPostErrorResponse(APIV3Urls.FAST_TRANSFER_FILE, req, ErrorCode.BAD_REQUEST);
        }
    }
}
