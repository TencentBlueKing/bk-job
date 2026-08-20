/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.constant.RollingTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v3.EsbRollingConfigDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastTransferFileRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FileSourceDTO;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * v4 分发文件请求转换的字段级对账单测。
 * <p>
 * 该转换是从零新写的（仓库里没有 v4 直接分发文件接口），审批预检与放行两次都用它，
 * 这里逐字段断言 v4 请求到内部执行模型的映射，并锁住校验失败的错误码。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class V4FastTransferFileRequestConverterTest {

    private static final long APP_ID = 2L;
    private static final String APP_CODE = "bk_ai";
    private static final User OPERATOR = new User("tenant_a", "admin", "管理员");

    @Mock
    private ServiceFileSourceResource fileSourceResource;
    @Mock
    private ArtifactoryLocalFileService artifactoryLocalFileService;

    private V4FastTransferFileRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new V4FastTransferFileRequestConverter(fileSourceResource, artifactoryLocalFileService);
    }

    @Test
    @DisplayName("v4 分发文件请求逐字段转换为快速任务")
    void convertAllFields() {
        V4FastTransferFileRequest request = baseRequest();
        request.setName("test_file_task");
        request.setTargetName("target.txt");
        request.setAccountId(1000L);
        request.setAccountAlias("root");
        request.setCallbackUrl("http://127.0.0.1/callback");
        request.setUploadSpeedLimit(1);
        request.setDownloadSpeedLimit(2);
        request.setTimeout(300);
        request.setStartTask(false);

        FastTaskDTO fastTask = converter.convert(request, OPERATOR, APP_CODE, false);

        TaskInstanceDTO taskInstance = fastTask.getTaskInstance();
        assertThat(taskInstance.getName()).isEqualTo("test_file_task");
        assertThat(taskInstance.getType()).isEqualTo(TaskTypeEnum.FILE.getValue());
        assertThat(taskInstance.getAppId()).isEqualTo(APP_ID);
        assertThat(taskInstance.getAppCode()).isEqualTo(APP_CODE);
        assertThat(taskInstance.getOperator()).isEqualTo("admin");
        assertThat(taskInstance.getStartupMode()).isEqualTo(TaskStartupModeEnum.API.getValue());
        assertThat(taskInstance.getStatus()).isEqualTo(RunStatusEnum.BLANK);
        assertThat(taskInstance.getPlanId()).isEqualTo(-1L);
        assertThat(taskInstance.getCronTaskId()).isEqualTo(-1L);
        assertThat(taskInstance.getTaskTemplateId()).isEqualTo(-1L);
        assertThat(taskInstance.getCallbackUrl()).isEqualTo("http://127.0.0.1/callback");

        StepInstanceDTO stepInstance = fastTask.getStepInstance();
        // 任务名与步骤名取同一次解析结果
        assertThat(stepInstance.getName()).isEqualTo("test_file_task");
        assertThat(stepInstance.getExecuteType()).isEqualTo(StepExecuteTypeEnum.SEND_FILE);
        assertThat(stepInstance.getFileTargetPath()).isEqualTo("/tmp/target/");
        assertThat(stepInstance.getFileTargetName()).isEqualTo("target.txt");
        assertThat(stepInstance.getAccountId()).isEqualTo(1000L);
        assertThat(stepInstance.getAccountAlias()).isEqualTo("root");
        assertThat(stepInstance.getTimeout()).isEqualTo(300);
        // 限速单位由 MB 换算为 KB
        assertThat(stepInstance.getFileUploadSpeedLimit()).isEqualTo(1024);
        assertThat(stepInstance.getFileDownloadSpeedLimit()).isEqualTo(2048);
        // 不传传输模式默认强制模式：目标路径不存在时创建
        assertThat(stepInstance.getNotExistPathHandler())
            .isEqualTo(NotExistPathHandlerEnum.CREATE_DIR.getValue());
        assertThat(stepInstance.getTargetExecuteObjects().getStaticIpList().get(0).getHostId()).isEqualTo(101L);

        List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
        assertThat(fileSources).hasSize(1);
        FileSourceDTO fileSource = fileSources.get(0);
        assertThat(fileSource.getFileType()).isEqualTo(TaskFileTypeEnum.SERVER.getType());
        assertThat(fileSource.isLocalUpload()).isFalse();
        // v4 把源文件账号从 account 对象平铺为 account_id / account_alias
        assertThat(fileSource.getAccountId()).isEqualTo(2000L);
        assertThat(fileSource.getAccountAlias()).isEqualTo("root2");
        assertThat(fileSource.getFiles()).hasSize(1);
        assertThat(fileSource.getFiles().get(0).getFilePath()).isEqualTo("/tmp/1.txt");
        assertThat(fileSource.getServers().getStaticIpList().get(0).getHostId()).isEqualTo(201L);

        assertThat(fastTask.getStartTask()).isFalse();
        assertThat(fastTask.getDryRun()).isFalse();
        assertThat(fastTask.getOperator()).isEqualTo(OPERATOR);
    }

    @Test
    @DisplayName("不传超时时间与传输模式时用默认值，dryRun 标记透传")
    void convertWithDefaults() {
        FastTaskDTO fastTask = converter.convert(baseRequest(), OPERATOR, APP_CODE, true);

        assertThat(fastTask.getStepInstance().getTimeout()).isEqualTo(JobConstants.DEFAULT_JOB_TIMEOUT_SECONDS);
        assertThat(fastTask.getDryRun()).isTrue();
    }

    @Test
    @DisplayName("严谨传输模式转换为步骤失败的路径处理方式")
    void convertStrictTransferMode() {
        V4FastTransferFileRequest request = baseRequest();
        request.setTransferMode(1);

        FastTaskDTO fastTask = converter.convert(request, OPERATOR, APP_CODE, false);

        assertThat(fastTask.getStepInstance().getNotExistPathHandler())
            .isEqualTo(NotExistPathHandlerEnum.STEP_FAIL.getValue());
        assertThat(fastTask.getStepInstance().getFileDuplicateHandle())
            .isEqualTo(DuplicateHandlerEnum.OVERWRITE.getId());
    }

    @ParameterizedTest(name = "传输模式 {0} 对应同名处理 {1}、路径不存在处理 {2}")
    @DisplayName("四种传输模式各自转换为对应的同名文件与不存在路径处理方式")
    @MethodSource("transferModes")
    void convertEachTransferMode(Integer transferMode,
                                 DuplicateHandlerEnum duplicateHandler,
                                 NotExistPathHandlerEnum notExistPathHandler) {
        V4FastTransferFileRequest request = baseRequest();
        request.setTransferMode(transferMode);

        FastTaskDTO fastTask = converter.convert(request, OPERATOR, APP_CODE, false);

        assertThat(fastTask.getStepInstance().getFileDuplicateHandle()).isEqualTo(duplicateHandler.getId());
        assertThat(fastTask.getStepInstance().getNotExistPathHandler()).isEqualTo(notExistPathHandler.getValue());
    }

    static Stream<Arguments> transferModes() {
        return Stream.of(
            Arguments.of(1, DuplicateHandlerEnum.OVERWRITE, NotExistPathHandlerEnum.STEP_FAIL),
            Arguments.of(2, DuplicateHandlerEnum.OVERWRITE, NotExistPathHandlerEnum.CREATE_DIR),
            Arguments.of(3, DuplicateHandlerEnum.GROUP_BY_IP, NotExistPathHandlerEnum.CREATE_DIR),
            Arguments.of(4, DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP, NotExistPathHandlerEnum.CREATE_DIR));
    }

    @Test
    @DisplayName("不传传输模式按强制模式处理：目标路径自动创建、同名文件覆盖")
    void convertWithoutTransferModeThenForce() {
        FastTaskDTO fastTask = converter.convert(baseRequest(), OPERATOR, APP_CODE, false);

        assertThat(fastTask.getStepInstance().getFileDuplicateHandle())
            .isEqualTo(DuplicateHandlerEnum.OVERWRITE.getId());
        assertThat(fastTask.getStepInstance().getNotExistPathHandler())
            .isEqualTo(NotExistPathHandlerEnum.CREATE_DIR.getValue());
    }

    @Test
    @DisplayName("传枚举外的传输模式直接报错，不静默降级为强制模式")
    void convertRejectUnknownTransferMode() {
        V4FastTransferFileRequest request = baseRequest();
        request.setTransferMode(99);

        assertThatThrownBy(() -> converter.convert(request, OPERATOR, APP_CODE, false))
            .isInstanceOf(InvalidParamException.class);

        ValidateResult result = converter.validate(request);
        assertThat(result.isPass()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly("transfer_mode");
    }

    @Test
    @DisplayName("第三方文件源按 code 解析出文件源 ID")
    void convertFileSourceCodeToId() {
        V4FastTransferFileRequest request = baseRequest();
        V4FileSourceDTO fileSource = new V4FileSourceDTO();
        fileSource.setFileType(TaskFileTypeEnum.FILE_SOURCE.getType());
        fileSource.setFiles(Collections.singletonList("/tmp/1.txt"));
        fileSource.setFileSourceCode("code-1");
        request.setFileSources(Collections.singletonList(fileSource));
        when(fileSourceResource.getFileSourceIdByCode(anyLong(), anyString())).thenReturn(successResp(8));

        FastTaskDTO fastTask = converter.convert(request, OPERATOR, APP_CODE, false);

        FileSourceDTO converted = fastTask.getStepInstance().getFileSourceList().get(0);
        assertThat(converted.getFileSourceId()).isEqualTo(8);
        assertThat(converted.getAccountId()).isNull();
    }

    @Test
    @DisplayName("目标路径非法时判为非法参数")
    void validateTargetPath() {
        V4FastTransferFileRequest request = baseRequest();
        request.setTargetPath("relative/path");

        assertThatThrownBy(() -> converter.convert(request, OPERATOR, APP_CODE, true))
            .isInstanceOf(InvalidParamException.class);
        assertThat(converter.validate(request).getErrorCode())
            .isEqualTo(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME);
    }

    @Test
    @DisplayName("目标账号、分发目标、源文件列表缺失时分别报出对应参数名")
    void validateRequiredParams() {
        V4FastTransferFileRequest noAccount = baseRequest();
        noAccount.setAccountAlias(null);
        noAccount.setAccountId(null);
        assertFailParam(converter.validate(noAccount), "account_id|account_alias");

        V4FastTransferFileRequest noTarget = baseRequest();
        noTarget.setExecuteTarget(new V4ExecuteTargetDTO());
        assertFailParam(converter.validate(noTarget), "execute_target");

        V4FastTransferFileRequest noFileSource = baseRequest();
        noFileSource.setFileSources(Collections.emptyList());
        assertFailParam(converter.validate(noFileSource), "file_source_list");
    }

    @Test
    @DisplayName("服务器文件的账号与所在主机缺失时判为非法参数")
    void validateServerFileSource() {
        V4FastTransferFileRequest noSourceAccount = baseRequest();
        noSourceAccount.getFileSources().get(0).setAccountId(null);
        noSourceAccount.getFileSources().get(0).setAccountAlias(null);
        assertFailParam(converter.validate(noSourceAccount),
            "file_source.account_id|file_source.account_alias");

        V4FastTransferFileRequest noSourceTarget = baseRequest();
        noSourceTarget.getFileSources().get(0).setExecuteTarget(null);
        assertFailParam(converter.validate(noSourceTarget), "file_source.execute_target");
    }

    @Test
    @DisplayName("按文件源滚动只支持服务器文件")
    void validateRollingOnlySupportServerFile() {
        V4FastTransferFileRequest request = baseRequest();
        request.getFileSources().get(0).setFileType(TaskFileTypeEnum.LOCAL.getType());
        EsbRollingConfigDTO rollingConfig = new EsbRollingConfigDTO();
        rollingConfig.setType(RollingTypeEnum.FILE_SOURCE.getValue());
        rollingConfig.setExpression("1");
        request.setRollingConfig(rollingConfig);

        ValidateResult result = converter.validate(request);

        assertThat(result.isPass()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.FILE_SOURCE_ROLLING_ONLY_SUPPORT_SERVER_FILE);
    }

    /**
     * 不用 InternalResponse.buildSuccessResp：它会走 I18nUtil 取错误文案，单测里没有 Spring 上下文
     */
    private <T> InternalResponse<T> successResp(T data) {
        InternalResponse<T> resp = new InternalResponse<>();
        resp.setSuccess(true);
        resp.setCode(ErrorCode.RESULT_OK);
        resp.setData(data);
        return resp;
    }

    private void assertFailParam(ValidateResult result, String paramName) {
        assertThat(result.isPass()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(ErrorCode.MISSING_PARAM_WITH_PARAM_NAME);
        assertThat(result.getErrorParams()).containsExactly(paramName);
    }

    private V4FastTransferFileRequest baseRequest() {
        V4FastTransferFileRequest request = new V4FastTransferFileRequest();
        request.setAppId(APP_ID);
        request.setName("test_file_task");
        request.setTargetPath(" /tmp/target/ ");
        request.setAccountAlias("root");
        request.setExecuteTarget(executeTarget(101L));
        V4FileSourceDTO fileSource = new V4FileSourceDTO();
        fileSource.setFiles(Collections.singletonList(" /tmp/1.txt "));
        fileSource.setAccountId(2000L);
        fileSource.setAccountAlias("root2");
        fileSource.setExecuteTarget(executeTarget(201L));
        request.setFileSources(Collections.singletonList(fileSource));
        return request;
    }

    private V4ExecuteTargetDTO executeTarget(long hostId) {
        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(hostId);
        executeTarget.setHostList(Collections.singletonList(host));
        return executeTarget;
    }
}
