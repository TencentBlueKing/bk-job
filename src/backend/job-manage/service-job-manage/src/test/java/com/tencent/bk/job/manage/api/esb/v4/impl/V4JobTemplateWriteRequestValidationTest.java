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

package com.tencent.bk.job.manage.api.esb.v4.impl;

import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureManager;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateAccountReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateExecuteTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileDestinationReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileSourceReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateScriptStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 模板写入请求的 Bean Validation 分支校验：步骤类型、脚本来源、源文件类型三处分支由
 * GroupSequenceProvider 动态启用。
 */
class V4JobTemplateWriteRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        // EsbAppScopeReq 的分组provider会读特性开关，脱离Spring容器时需要先塞一个可用的上下文
        mockSpringContextForFeatureToggle();
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
        resetStaticField(FeatureToggle.class, "featureManager");
        resetStaticField(ApplicationContextRegister.class, "context");
    }

    private static void mockSpringContextForFeatureToggle() {
        FeatureManager featureManager = mock(FeatureManager.class);
        when(featureManager.checkFeature(anyString(), any())).thenReturn(false);
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(FeatureManager.class)).thenReturn(featureManager);
        new ApplicationContextRegister().setApplicationContext(context);
    }

    /**
     * 静态缓存会串到同一 JVM 里的其它用例，用完必须清掉。
     */
    private static void resetStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reset static field fail: " + clazz.getName() + "#" + fieldName, e);
        }
    }

    @Test
    @DisplayName("合法的创建请求无校验错误")
    void valid_create_request_passes() {
        V4CreateJobTemplateRequest request = createRequest(localScriptStep());
        assertThat(validate(request)).isEmpty();
    }

    @Test
    @DisplayName("模板名称为空时报错")
    void blank_name_is_rejected() {
        V4CreateJobTemplateRequest request = createRequest(localScriptStep());
        request.setName("  ");
        assertThat(violatedPaths(request)).contains("name");
    }

    @Test
    @DisplayName("步骤列表为空时报错")
    void empty_step_list_is_rejected() {
        V4CreateJobTemplateRequest request = createRequest(localScriptStep());
        request.setStepList(Collections.emptyList());
        assertThat(violatedPaths(request)).contains("stepList");
    }

    @Test
    @DisplayName("脚本步骤缺少 script_info 时报错")
    void script_step_without_script_info_is_rejected() {
        V4JobTemplateStepReq step = localScriptStep();
        step.setScriptInfo(null);
        assertThat(violatedPaths(createRequest(step))).contains("stepList[0].scriptInfo");
    }

    @Test
    @DisplayName("本地脚本缺少脚本内容时报错，但不要求 script_id")
    void local_script_requires_content_only() {
        V4JobTemplateStepReq step = localScriptStep();
        step.getScriptInfo().setScriptContent(null);

        Set<String> paths = violatedPaths(createRequest(step));
        assertThat(paths).contains("stepList[0].scriptInfo.scriptContent");
        assertThat(paths).doesNotContain("stepList[0].scriptInfo.scriptId");
    }

    @Test
    @DisplayName("引用脚本缺少 script_id 与版本时报错，但不要求脚本内容")
    void cited_script_requires_script_id_and_version() {
        V4JobTemplateStepReq step = localScriptStep();
        step.getScriptInfo().setScriptType(TaskScriptSourceEnum.CITING.getType());
        step.getScriptInfo().setScriptContent(null);
        step.getScriptInfo().setScriptLanguage(null);

        Set<String> paths = violatedPaths(createRequest(step));
        assertThat(paths).contains("stepList[0].scriptInfo.scriptId", "stepList[0].scriptInfo.scriptVersionId");
        assertThat(paths).doesNotContain("stepList[0].scriptInfo.scriptContent");
    }

    @Test
    @DisplayName("文件源文件缺少 file_source_id 时报错")
    void file_source_file_requires_file_source_id() {
        V4JobTemplateStepReq step = fileStep(TaskFileTypeEnum.FILE_SOURCE);
        Set<String> paths = violatedPaths(createRequest(step));
        assertThat(paths).contains("stepList[0].fileInfo.fileSourceList[0].fileSourceId");
    }

    @Test
    @DisplayName("服务器文件缺少执行目标与账号时报错")
    void server_file_requires_target_and_account() {
        V4JobTemplateStepReq step = fileStep(TaskFileTypeEnum.SERVER);
        Set<String> paths = violatedPaths(createRequest(step));
        assertThat(paths).contains(
            "stepList[0].fileInfo.fileSourceList[0].executeTarget",
            "stepList[0].fileInfo.fileSourceList[0].account");
    }

    @Test
    @DisplayName("本地文件不要求执行目标、账号与文件源 ID")
    void local_file_requires_nothing_extra() {
        V4JobTemplateStepReq step = fileStep(TaskFileTypeEnum.LOCAL);
        Set<String> paths = violatedPaths(createRequest(step));
        assertThat(paths).noneMatch(path -> path.startsWith("stepList[0].fileInfo.fileSourceList[0]"));
    }

    @Test
    @DisplayName("步骤 ID 传非正数时报错")
    void non_positive_step_id_is_rejected() {
        V4JobTemplateStepReq step = localScriptStep();
        step.setId(-1L);

        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setId(1000L);
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("api-template");
        request.setStepList(Collections.singletonList(step));

        assertThat(violatedPaths(request)).contains("stepList[0].id");
    }

    @Test
    @DisplayName("更新请求缺少模板 ID 时报错")
    void update_request_requires_template_id() {
        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("api-template");
        request.setStepList(Collections.singletonList(localScriptStep()));

        assertThat(violatedPaths(request)).contains("id");
    }

    // ------------------------------------------------------------ 构造工具

    private <T> Set<ConstraintViolation<T>> validate(T request) {
        return validator.validate(request);
    }

    private <T> Set<String> violatedPaths(T request) {
        return validate(request).stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }

    private V4CreateJobTemplateRequest createRequest(V4JobTemplateStepReq step) {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setName("api-template");
        request.setStepList(Collections.singletonList(step));
        return request;
    }

    private V4JobTemplateStepReq localScriptStep() {
        V4JobTemplateScriptStepReq scriptInfo = new V4JobTemplateScriptStepReq();
        scriptInfo.setScriptType(TaskScriptSourceEnum.LOCAL.getType());
        scriptInfo.setScriptContent("ZWNobyBoZWxsbw==");
        scriptInfo.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
        scriptInfo.setAccount(accountReq());
        scriptInfo.setExecuteTarget(variableTarget());

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setName("run-script");
        step.setType(TaskStepTypeEnum.SCRIPT.getValue());
        step.setScriptInfo(scriptInfo);
        return step;
    }

    /**
     * 只填 file_type 与 file_list，其余分支字段留空，用来观察各类型各自要求哪些字段。
     */
    private V4JobTemplateStepReq fileStep(TaskFileTypeEnum fileType) {
        V4JobTemplateFileSourceReq fileSource = new V4JobTemplateFileSourceReq();
        fileSource.setFileType(fileType.getType());
        fileSource.setFileList(Collections.singletonList("/data/a.txt"));

        V4JobTemplateFileDestinationReq destination = new V4JobTemplateFileDestinationReq();
        destination.setPath("/data/dest");
        destination.setAccount(accountReq());
        destination.setExecuteTarget(variableTarget());

        V4JobTemplateFileStepReq fileInfo = new V4JobTemplateFileStepReq();
        fileInfo.setFileSourceList(Collections.singletonList(fileSource));
        fileInfo.setFileDestination(destination);

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setName("transfer-file");
        step.setType(TaskStepTypeEnum.FILE.getValue());
        step.setFileInfo(fileInfo);
        return step;
    }

    private V4JobTemplateAccountReq accountReq() {
        V4JobTemplateAccountReq account = new V4JobTemplateAccountReq();
        account.setId(1L);
        return account;
    }

    private V4JobTemplateExecuteTargetReq variableTarget() {
        V4JobTemplateExecuteTargetReq target = new V4JobTemplateExecuteTargetReq();
        target.setVariable("TARGET_HOSTS");
        return target;
    }
}
