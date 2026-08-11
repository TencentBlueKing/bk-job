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

package com.tencent.bk.job.analysis.approval.impl;

import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalTicket;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalRiskLevelEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批单据渲染与脱敏。
 * <p>
 * 本类盯死两件"破了就等于没做"的事：
 * <ul>
 *     <li><b>概要区必须看清"在哪些机器上执行什么"</b>，否则等于逼审批人盲签；</li>
 *     <li><b>密码类字段的明文与密文都不得出现在单据的任何字段里</b>，脚本内容是唯一例外。</li>
 * </ul>
 */
class DefaultApprovalTicketRendererTest {

    private static final String TASK_ID = "e2a1c0d4-1111-2222-3333-444455556666";
    private static final String CREATOR = "admin";
    private static final Long APP_ID = 2L;
    private static final String SCRIPT_CONTENT = "echo hello && rm -rf /tmp/a";
    private static final String PLAIN_PASSWORD = "P@ssw0rd-should-never-appear";
    private static final String ENCRYPTED_PASSWORD = "CIPHER-3f8a9b-should-never-appear";
    private static final String SENSITIVE_SCRIPT_PARAM = "--token=secret-should-never-appear";

    private CommonAppService appService;
    private MessageI18nService i18nService;
    private DefaultApprovalTicketRenderer renderer;

    @BeforeEach
    void setUp() {
        appService = mock(CommonAppService.class);
        BasicApp app = new BasicApp();
        app.setId(APP_ID);
        app.setName("运维测试业务");
        app.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        when(appService.getApp(anyLong())).thenReturn(app);

        // i18n 替身原样回显 key，便于断言"用了哪个文案"而不依赖具体译文
        i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(i18nService.getI18nWithArgs(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        renderer = new DefaultApprovalTicketRenderer(i18nService, appService, new NoopParamsCryptoService());
    }

    @Test
    @DisplayName("概要区默认展开、原始参数区默认折叠")
    void givenTaskThenSummaryExpandedAndRawParamsCollapsed() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        assertThat(ticket.getSections()).hasSize(2);
        ApprovalTicket.Section summary = ticket.getSections().get(0);
        ApprovalTicket.Section rawParams = ticket.getSections().get(1);
        assertThat(summary.getKey()).isEqualTo(ApprovalTicket.SECTION_SUMMARY);
        assertThat(summary.isCollapsed()).isFalse();
        assertThat(rawParams.getKey()).isEqualTo(ApprovalTicket.SECTION_RAW_PARAMS);
        assertThat(rawParams.isCollapsed()).isTrue();
    }

    @Test
    @DisplayName("概要区看得清在哪些机器上执行什么：执行对象、台数、账号、脚本都在")
    void givenSummaryThenShowExecuteObjectsAndScript() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        List<ApprovalTicket.Field> fields = ticket.getSections().get(0).getFields();
        assertThat(valuesOf(fields)).anySatisfy(value -> assertThat(value).contains("0:127.0.0.1"));
        assertThat(valuesOf(fields)).contains("37");
        assertThat(valuesOf(fields)).contains("root");
        assertThat(valuesOf(fields)).contains("check_disk.sh");
        assertThat(ticket.getTitle())
            .as("标题须带上操作名与业务名")
            .contains(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.getNameI18nKey())
            .contains("运维测试业务");
    }

    @Test
    @DisplayName("高危账号与高危规则命中都打 highlight，风险等级为 HIGH")
    void givenHighRiskThenHighlightAndHighRiskLevel() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        assertThat(ticket.getRiskLevel()).isEqualTo(ApprovalRiskLevelEnum.HIGH.name());
        List<ApprovalTicket.Field> highlighted = ticket.getSections().get(0).getFields().stream()
            .filter(ApprovalTicket.Field::isHighlight)
            .collect(Collectors.toList());
        assertThat(valuesOf(highlighted)).contains("root");
        assertThat(labelsOf(highlighted))
            .anySatisfy(label -> assertThat(label).contains("dangerousRuleMatched"));
    }

    @Test
    @DisplayName("动态分组目标在单据上可见且高亮：这是对「放行时重新解析」这一已知限制的如实披露")
    void givenDynamicTargetThenHighlightHintVisible() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        List<ApprovalTicket.Field> fields = ticket.getSections().get(0).getFields();
        Optional<ApprovalTicket.Field> hint = fields.stream()
            .filter(field -> field.getLabel().contains("containsDynamicTarget"))
            .findFirst();
        assertThat(hint).isPresent();
        assertThat(hint.get().isHighlight()).isTrue();
        assertThat(hint.get().getValue()).contains("dynamicTargetHint");
    }

    @Test
    @DisplayName("脚本内容原样展示并已解码：不展示则审批人无从判断风险")
    void givenScriptContentThenShowDecodedPlainText() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        ApprovalTicket.Field field = findField(ticket, "script_content");
        assertThat(field).isNotNull();
        assertThat(field.getValue()).isEqualTo(SCRIPT_CONTENT);
        assertThat(field.isSensitive()).isFalse();
    }

    @Test
    @DisplayName("主机账号密码只披露「提供了自定义密码」，明文与密文都不出现")
    void givenHostPasswordThenOnlyDiscloseProvided() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        ApprovalTicket.Field field = findField(ticket, "host_password_list[0].encrypted_password");
        assertThat(field).isNotNull();
        assertThat(field.isSensitive()).isTrue();
        assertThat(field.getValue()).contains("passwordProvided");
        assertAllValuesFreeOfSecrets(ticket);
    }

    @Test
    @DisplayName("用户声明为敏感的脚本参数打固定掩码，连长度都不泄露")
    void givenSensitiveScriptParamThenMasked() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        ApprovalTicket.Field field = findField(ticket, "script_param");
        assertThat(field).isNotNull();
        assertThat(field.isSensitive()).isTrue();
        assertThat(field.getValue()).isEqualTo("******");
        assertAllValuesFreeOfSecrets(ticket);
    }

    @Test
    @DisplayName("未声明为敏感的脚本参数原样解码展示：它同样是判断风险的必要信息")
    void givenNonSensitiveScriptParamThenShowDecoded() {
        ApprovalTicket ticket = renderer.render(buildTask(buildFullSummary(), buildScriptParams(false)));

        ApprovalTicket.Field field = findField(ticket, "script_param");
        assertThat(field).isNotNull();
        assertThat(field.isSensitive()).isFalse();
        assertThat(field.getValue()).isEqualTo(SENSITIVE_SCRIPT_PARAM);
    }

    @Test
    @DisplayName("执行方案全局变量值一律打码：v4 请求体无变量类型，宁可全打也不能漏掉密码类变量")
    void givenJobPlanGlobalVarThenAlwaysMasked() {
        String params = "{\"job_plan_id\":100,\"global_var_list\":["
            + "{\"name\":\"password\",\"value\":\"" + PLAIN_PASSWORD + "\"},"
            + "{\"name\":\"port\",\"value\":\"8080\"}]}";
        ApprovalTaskDTO task = buildTask(new ResolvedSummary(), params);
        task.setOperationType(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN.name());

        ApprovalTicket ticket = renderer.render(task);

        assertThat(findField(ticket, "global_var_list[0].value").getValue()).isEqualTo("******");
        assertThat(findField(ticket, "global_var_list[0].value").isSensitive()).isTrue();
        assertThat(findField(ticket, "global_var_list[1].value").getValue()).isEqualTo("******");
        assertAllValuesFreeOfSecrets(ticket);
    }

    @Test
    @DisplayName("参数解密失败时概要区仍然可用，只在参数区给出提示")
    void givenParamsDecryptFailThenKeepSummaryUsable() {
        DefaultApprovalTicketRenderer failingRenderer = new DefaultApprovalTicketRenderer(
            i18nService, appService, new FailingParamsCryptoService());

        ApprovalTicket ticket = failingRenderer.render(buildTask(buildFullSummary(), buildScriptParams(true)));

        assertThat(ticket.getSections().get(0).getFields()).isNotEmpty();
        List<String> rawValues = valuesOf(ticket.getSections().get(1).getFields());
        assertThat(rawValues).hasSize(1);
        assertThat(rawValues.get(0)).contains("rawParamsRenderFail");
    }

    @Test
    @DisplayName("概要为空也能出单据，不因缺字段抛异常")
    void givenEmptySummaryThenStillRender() {
        ApprovalTaskDTO task = buildTask(new ResolvedSummary(), null);
        task.setResolvedSummary(null);

        ApprovalTicket ticket = renderer.render(task);

        assertThat(ticket.getApprovalTaskId()).isEqualTo(TASK_ID);
        assertThat(ticket.getRiskLevel()).isEqualTo(ApprovalRiskLevelEnum.LOW.name());
        assertThat(ticket.getSections()).hasSize(2);
    }

    /**
     * 单据里任何一个字段值都不得出现密码明文或密文
     */
    private void assertAllValuesFreeOfSecrets(ApprovalTicket ticket) {
        for (ApprovalTicket.Section section : ticket.getSections()) {
            if (section.getFields() == null) {
                continue;
            }
            for (ApprovalTicket.Field field : section.getFields()) {
                assertThat(StringUtils.defaultString(field.getValue()))
                    .as("字段 %s 泄露了敏感值", field.getLabel())
                    .doesNotContain(PLAIN_PASSWORD)
                    .doesNotContain(ENCRYPTED_PASSWORD)
                    .doesNotContain(SENSITIVE_SCRIPT_PARAM);
            }
        }
    }

    private ApprovalTicket.Field findField(ApprovalTicket ticket, String label) {
        for (ApprovalTicket.Section section : ticket.getSections()) {
            if (section.getFields() == null) {
                continue;
            }
            for (ApprovalTicket.Field field : section.getFields()) {
                if (label.equals(field.getLabel())) {
                    return field;
                }
            }
        }
        return null;
    }

    private List<String> valuesOf(List<ApprovalTicket.Field> fields) {
        return fields.stream().map(ApprovalTicket.Field::getValue).collect(Collectors.toList());
    }

    private List<String> labelsOf(List<ApprovalTicket.Field> fields) {
        return fields.stream().map(ApprovalTicket.Field::getLabel).collect(Collectors.toList());
    }

    private ApprovalTaskDTO buildTask(ResolvedSummary summary, String paramsJson) {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(TASK_ID);
        task.setTenantId("default");
        task.setAppId(APP_ID);
        task.setOperationType(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
        task.setCreator(CREATOR);
        task.setApprovalChannel("IMATE");
        task.setStatus("PENDING");
        task.setCreateTime(System.currentTimeMillis());
        task.setExpireAt(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8));
        task.setResolvedSummary(JsonUtils.toJson(summary));
        task.setOperationParams(paramsJson);
        return task;
    }

    /**
     * 一个"该有的都有"的概要：动态分组 + 高危账号 + 高危规则命中 + 执行对象清单
     */
    private ResolvedSummary buildFullSummary() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.name());
        summary.setName("运基线");
        summary.setTotalExecuteObjectCount(37);
        summary.setDangerousRuleMatched(true);
        summary.setContainsDynamicTarget(true);
        summary.addDefaultApplied("timeout", "7200");

        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setName("执行脚本");
        step.setExecuteType("EXECUTE_SCRIPT");
        step.setAccountAlias("root");
        step.setHighRiskAccount(true);
        step.setScriptName("check_disk.sh");
        step.setScriptVersionId(1001L);
        step.setDangerousCheckSummary("命中高危规则：rm -rf");
        step.setExecuteObjectCount(37);
        step.setContainsDynamicTarget(true);
        step.setExecuteObjects(Collections.singletonList(
            new ResolvedSummary.ResolvedExecuteObject("HOST", 1L, "0:127.0.0.1")));
        summary.addStep(step);
        return summary;
    }

    private String buildScriptParams(boolean paramSensitive) {
        return "{\"name\":\"quick-script\",\"script_content\":\"" + base64(SCRIPT_CONTENT) + "\","
            + "\"script_param\":\"" + base64(SENSITIVE_SCRIPT_PARAM) + "\","
            + "\"param_sensitive\":" + paramSensitive + ","
            + "\"host_password_list\":[{\"host_id\":1,\"account\":\"root\","
            + "\"encrypted_password\":\"" + ENCRYPTED_PASSWORD + "\"}]}";
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 不做任何加解密的替身：本类验证的是"渲染时是否脱敏"，与加密实现无关
     */
    private static class NoopParamsCryptoService implements ApprovalParamsCryptoService {

        @Override
        public String encryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            return paramsJson;
        }

        @Override
        public String decryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            return paramsJson;
        }
    }

    private static class FailingParamsCryptoService implements ApprovalParamsCryptoService {

        @Override
        public String encryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            return paramsJson;
        }

        @Override
        public String decryptSensitiveFields(ApprovalOperationTypeEnum operationType, String paramsJson) {
            throw new IllegalStateException("decrypt failed");
        }
    }
}
