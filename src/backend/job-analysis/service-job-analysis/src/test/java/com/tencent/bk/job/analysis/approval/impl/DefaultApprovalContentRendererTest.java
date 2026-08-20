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
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalRiskLevelEnum;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptorRegistry;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptorTestSupport;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.model.esb.v3.EsbCustomHostPasswordDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批内容渲染与脱敏。
 * <p>
 * 本类盯死三件"破了就等于没做"的事：
 * <ul>
 *     <li><b>正文必须看清操作影响面</b>（业务、操作类型、执行对象总数、高危与动态目标提示），否则等于逼审批人盲签；</li>
 *     <li><b>密码类字段的明文与密文都不得出现在正文的任何位置</b>，脚本内容是唯一例外；</li>
 *     <li><b>逐步骤的解析结果不进正文</b>：单据铺得越长审批人越容易一路划到底直接点通过，
 *     但它仍须参与风险定级。</li>
 * </ul>
 * 断言一律落在 {@code approvalContent} 这一份 Markdown 正文上：它是审批人唯一看得到的东西。
 * <p>
 * 参数快照用真实的加密实现产出，而不是塞一段手写 JSON：只有走同一条链路，
 * "加密了就一定打码"这个不变式才真的被验证到。
 */
class DefaultApprovalContentRendererTest {

    private static final String SECTION_SUMMARY = "## task.approval.content.section.summary";
    private static final String SECTION_SCRIPT = "## task.approval.content.section.scriptContent";
    private static final String SECTION_RAW_PARAMS = "## task.approval.content.section.rawParams";
    private static final String TABLE_HEADER =
        "| task.approval.content.table.item | task.approval.content.table.value |";

    private static final String TASK_ID = "e2a1c0d4111122223333444455556666";
    private static final String CREATOR = "admin";
    private static final Long APP_ID = 2L;
    private static final long PLAN_ID = 100L;
    private static final String SCRIPT_CONTENT = "echo hello && rm -rf /tmp/a";
    private static final String PLAIN_PASSWORD = "P@ssw0rd-should-never-appear";
    private static final String HOST_PASSWORD = "CIPHER-3f8a9b-should-never-appear";
    private static final String SENSITIVE_SCRIPT_PARAM = "--token=secret-should-never-appear";

    private ApprovalParamsCryptorTestSupport cryptorSupport;
    private ApprovalParamsCryptoService cryptoService;
    private CommonAppService appService;
    private MessageI18nService i18nService;
    private DefaultApprovalContentRenderer renderer;

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

        cryptorSupport = new ApprovalParamsCryptorTestSupport();
        cryptorSupport.givenPlanVars(PLAN_ID,
            cryptorSupport.cipherVar(1L, "password"),
            cryptorSupport.stringVar(2L, "port"));
        cryptoService = new ApprovalParamsCryptoServiceImpl(
            new ApprovalParamsCryptorRegistry(cryptorSupport.allCryptors()));

        renderer = new DefaultApprovalContentRenderer(i18nService, appService, cryptoService);
    }

    @Test
    @DisplayName("正文是一份 Markdown：一级标题 + 概要表格 + 脚本章节 + 原始参数代码块，顺序固定")
    void givenTaskThenRenderMarkdownWithFixedSectionOrder() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        String content = rendered.getApprovalContent();
        assertThat(content)
            .as("标题只在正文里给，返回体不再有结构化的 title 字段")
            .startsWith("# " + ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.getNameI18nKey());
        assertThat(content).contains(SECTION_SUMMARY);
        assertThat(content)
            .as("概要必须以表格呈现，逐项对照才看得清")
            .contains(TABLE_HEADER + "\n| --- | --- |");
        assertThat(content).contains("```json");
        assertThat(indexOf(content, SECTION_SUMMARY))
            .isLessThan(indexOf(content, SECTION_SCRIPT))
            .isLessThan(indexOf(content, SECTION_RAW_PARAMS));
        assertThat(indexOf(content, SECTION_SCRIPT)).isLessThan(indexOf(content, SECTION_RAW_PARAMS));
    }

    @Test
    @DisplayName("正文看得清影响面：操作对象、执行对象总数与业务都在概要里")
    void givenSummaryThenShowScaleAndScope() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        String summarySection = sectionOf(rendered.getApprovalContent(), SECTION_SUMMARY);
        assertThat(summarySection)
            .contains("运基线")
            .contains("37")
            .contains("root");
        assertThat(firstLine(rendered.getApprovalContent()))
            .as("标题须带上操作名与业务名")
            .contains(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT.getNameI18nKey())
            .contains("运维测试业务");
    }

    @Test
    @DisplayName("逐步骤的解析结果不进正文：单据越长审批人越容易一路划到底直接点通过")
    void givenStepsThenNotRendered() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(rendered.getApprovalContent())
            .doesNotContain("0:127.0.0.1")
            .doesNotContain("check_disk.sh")
            .doesNotContain("执行脚本")
            .doesNotContain("task.approval.content.stepPrefix");
    }

    @Test
    @DisplayName("步骤明细不展示，但用到的执行账号汇总成一行：以什么身份上机是审批人必须看到的")
    void givenStepAccountsThenShowInSummary() {
        ResolvedSummary summary = buildFullSummary();
        ResolvedSummary.ResolvedStep secondStep = new ResolvedSummary.ResolvedStep();
        secondStep.setAccountAlias("mysql");
        summary.addStep(secondStep);
        // 同一个账号在多个步骤里出现只列一次
        ResolvedSummary.ResolvedStep thirdStep = new ResolvedSummary.ResolvedStep();
        thirdStep.setAccountAlias("root");
        summary.addStep(thirdStep);

        ApprovalContent rendered = renderer.render(scriptTask(summary, true));

        assertThat(tableRow(rendered.getApprovalContent(), "content.accounts")).contains("root, mysql");
    }

    @ParameterizedTest(name = "命中高危={0}、执行对象数={1} 时风险等级为 {2}")
    @DisplayName("风险等级只看高危语句命中与执行对象规模")
    @MethodSource("riskLevels")
    void givenSummaryThenResolveRiskLevel(boolean dangerousRuleMatched,
                                          Integer executeObjectCount,
                                          ApprovalRiskLevelEnum expected) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setDangerousRuleMatched(dangerousRuleMatched);
        summary.setTotalExecuteObjectCount(executeObjectCount);

        ApprovalContent rendered = renderer.render(scriptTask(summary, true));

        assertThat(tableRow(rendered.getApprovalContent(), "riskLevel")).contains(expected.getNameI18nKey());
    }

    static Stream<Arguments> riskLevels() {
        return Stream.of(
            // 命中高危语句一律高危，与规模无关
            Arguments.of(true, 1, ApprovalRiskLevelEnum.HIGH),
            Arguments.of(false, 101, ApprovalRiskLevelEnum.HIGH),
            // 100 台不算超过 100
            Arguments.of(false, 100, ApprovalRiskLevelEnum.MEDIUM),
            Arguments.of(false, 11, ApprovalRiskLevelEnum.MEDIUM),
            // 10 台不算超过 10
            Arguments.of(false, 10, ApprovalRiskLevelEnum.LOW),
            Arguments.of(false, null, ApprovalRiskLevelEnum.LOW));
    }

    @Test
    @DisplayName("动态目标只作提示，不再抬高风险等级")
    void givenDynamicTargetThenRiskLevelUnaffected() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setContainsDynamicTarget(true);
        summary.setTotalExecuteObjectCount(1);

        ApprovalContent rendered = renderer.render(scriptTask(summary, true));

        assertThat(tableRow(rendered.getApprovalContent(), "riskLevel"))
            .contains(ApprovalRiskLevelEnum.LOW.getNameI18nKey());
    }

    @Test
    @DisplayName("高危规则命中加粗，风险等级为 HIGH")
    void givenHighRiskThenBoldAndHighRiskLevel() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(tableRow(rendered.getApprovalContent(), "riskLevel"))
            .contains(ApprovalRiskLevelEnum.HIGH.getNameI18nKey());
        assertThat(tableRow(rendered.getApprovalContent(), "dangerousRuleMatched"))
            .contains("**task.approval.content.dangerousRuleMatched**");
    }

    @Test
    @DisplayName("没传的字段不出现在原始参数里：一屏 null 会把真正传了什么淹没掉")
    void givenNullFieldsThenNotRenderedInRawParams() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_RAW_PARAMS))
            .doesNotContain("null")
            .contains("quick-script");
    }

    @Test
    @DisplayName("动态分组目标在正文中可见且加粗：这是对「放行时重新解析」这一已知限制的如实披露")
    void givenDynamicTargetThenBoldHintVisible() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        String row = tableRow(rendered.getApprovalContent(), "containsDynamicTarget");
        assertThat(row)
            .contains("**task.approval.content.containsDynamicTarget**")
            .contains("dynamicTargetHint");
    }

    @Test
    @DisplayName("脚本内容单独成章、代码块原样展示并已解码：不展示则审批人无从判断风险")
    void givenScriptContentThenShowDecodedPlainTextInOwnSection() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        String scriptSection = sectionOf(rendered.getApprovalContent(), SECTION_SCRIPT);
        assertThat(scriptSection)
            .contains("`script_content`")
            .contains("```\n" + SCRIPT_CONTENT + "\n```");
        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_RAW_PARAMS))
            .as("参数里只留指向脚本章节的占位符，避免同一段脚本出现两遍")
            .contains("task.approval.content.value.scriptInSection")
            .doesNotContain(SCRIPT_CONTENT);
    }

    @Test
    @DisplayName("脚本自带 ``` 时代码块围栏自动加长，不会把后续内容挤出代码块")
    void givenScriptWithFenceThenExtendFence() {
        String script = "echo '```'";
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("quick-script");
        request.setContent(base64(script));

        ApprovalContent rendered = renderer.render(taskOf(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildFullSummary(), request));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_SCRIPT))
            .contains("````\n" + script + "\n````");
    }

    @Test
    @DisplayName("主机账号密码只披露「提供了自定义密码」，明文与密文都不出现")
    void givenHostPasswordThenOnlyDiscloseProvided() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(rendered.getApprovalContent()).contains("task.approval.content.value.passwordProvided");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("用户声明为敏感的脚本参数打固定掩码，连长度都不泄露")
    void givenSensitiveScriptParamThenMasked() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_RAW_PARAMS)).contains("******");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("未声明为敏感的脚本参数原样解码展示：它同样是判断风险的必要信息")
    void givenNonSensitiveScriptParamThenShowDecoded() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), false));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_RAW_PARAMS))
            .contains(SENSITIVE_SCRIPT_PARAM);
    }

    @Test
    @DisplayName("执行方案全局变量：密文变量打码，普通变量原样展示供审批人判断影响面")
    void givenJobPlanGlobalVarThenMaskCipherVarOnly() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setPlanId(PLAN_ID);
        V4GlobalVarDTO cipherVar = new V4GlobalVarDTO();
        cipherVar.setId(1L);
        cipherVar.setName("password");
        cipherVar.setValue(PLAIN_PASSWORD);
        V4GlobalVarDTO plainVar = new V4GlobalVarDTO();
        plainVar.setId(2L);
        plainVar.setName("port");
        plainVar.setValue("8080");
        request.setGlobalVars(Arrays.asList(cipherVar, plainVar));

        ApprovalContent rendered = renderer.render(taskOf(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, new ResolvedSummary(), request));

        assertThat(rendered.getApprovalContent())
            .contains("******")
            .contains("8080");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("参数渲染失败时概要仍然可用，只在参数章节给出提示")
    void givenParamsDecryptFailThenKeepSummaryUsable() {
        DefaultApprovalContentRenderer failingRenderer = new DefaultApprovalContentRenderer(
            i18nService, appService, new FailingParamsCryptoService());

        ApprovalContent rendered = failingRenderer.render(scriptTask(buildFullSummary(), true));

        String content = rendered.getApprovalContent();
        assertThat(content).contains(SECTION_SUMMARY).contains("运基线");
        assertThat(sectionOf(content, SECTION_RAW_PARAMS)).contains("rawParamsRenderFail");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("概要为空也能出内容，不因缺字段抛异常")
    void givenEmptySummaryThenStillRender() {
        ApprovalTaskDTO task = buildTask(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, new ResolvedSummary(), null);
        task.setResolvedSummary(null);

        ApprovalContent rendered = renderer.render(task);

        assertThat(rendered.getApprovalTaskId()).isEqualTo(TASK_ID);
        assertThat(tableRow(rendered.getApprovalContent(), "riskLevel"))
            .contains(ApprovalRiskLevelEnum.LOW.getNameI18nKey());
        assertThat(rendered.getApprovalContent()).contains(SECTION_SUMMARY).contains(TABLE_HEADER);
        assertThat(rendered.getApprovalContent())
            .as("没有参数快照时不该留下一个空的参数章节")
            .doesNotContain(SECTION_RAW_PARAMS);
    }

    /**
     * 正文任何一处都不得出现密码明文或密文
     */
    private void assertContentFreeOfSecrets(ApprovalContent rendered) {
        assertThat(StringUtils.defaultString(rendered.getApprovalContent()))
            .doesNotContain(PLAIN_PASSWORD)
            .doesNotContain(HOST_PASSWORD)
            .doesNotContain(SENSITIVE_SCRIPT_PARAM);
    }

    /**
     * 取出某个二级章节的正文，避免跨章节误判
     */
    private String sectionOf(String content, String heading) {
        int start = indexOf(content, heading);
        int end = content.indexOf("\n## ", start + heading.length());
        return end < 0 ? content.substring(start) : content.substring(start, end);
    }

    /**
     * 取出表格中标签命中 {@code labelKeyword} 的那一行
     */
    private String tableRow(String content, String labelKeyword) {
        return Arrays.stream(content.split("\n"))
            .filter(line -> line.startsWith("|") && line.contains(labelKeyword))
            .findFirst()
            .orElseThrow(() -> new AssertionError("未找到包含 " + labelKeyword + " 的表格行"));
    }

    private String firstLine(String content) {
        return content.split("\n")[0];
    }

    private int indexOf(String content, String text) {
        int index = content.indexOf(text);
        assertThat(index).as("正文缺少内容：%s", text).isNotNegative();
        return index;
    }

    private ApprovalTaskDTO scriptTask(ResolvedSummary summary, boolean paramSensitive) {
        return taskOf(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, summary,
            buildScriptRequest(paramSensitive));
    }

    /**
     * 参数快照走真实加密产出，与线上落库的内容一致
     */
    private ApprovalTaskDTO taskOf(ApprovalOperationTypeEnum operationType,
                                   ResolvedSummary summary,
                                   Object params) {
        return buildTask(operationType, summary, cryptoService.encryptToSnapshot(operationType, params));
    }

    private ApprovalTaskDTO buildTask(ApprovalOperationTypeEnum operationType,
                                      ResolvedSummary summary,
                                      String snapshot) {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(TASK_ID);
        task.setTenantId("default");
        task.setAppId(APP_ID);
        task.setOperationType(operationType.name());
        task.setCreator(CREATOR);
        task.setApprovalChannel("IMATE");
        task.setStatus("PENDING");
        task.setCreateTime(System.currentTimeMillis());
        task.setExpireAt(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(8));
        task.setResolvedSummary(JsonUtils.toJson(summary));
        task.setOperationParams(snapshot);
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

    private V4FastExecuteScriptRequest buildScriptRequest(boolean paramSensitive) {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("quick-script");
        request.setContent(base64(SCRIPT_CONTENT));
        request.setScriptParam(base64(SENSITIVE_SCRIPT_PARAM));
        request.setParamSensitive(paramSensitive);
        EsbCustomHostPasswordDTO hostPassword = new EsbCustomHostPasswordDTO();
        hostPassword.setHostId(1L);
        hostPassword.setEncryptedPassword(HOST_PASSWORD);
        request.setHostPasswordList(Collections.singletonList(hostPassword));
        return request;
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static class FailingParamsCryptoService implements ApprovalParamsCryptoService {

        @Override
        public String encryptToSnapshot(ApprovalOperationTypeEnum operationType, Object params) {
            return JsonUtils.toJson(params);
        }

        @Override
        public Object decryptFromSnapshot(ApprovalOperationTypeEnum operationType, String snapshot) {
            throw new IllegalStateException("decrypt failed");
        }

        @Override
        public ApprovalDisplayParams desensitizeFromSnapshot(ApprovalOperationTypeEnum operationType,
                                                             String snapshot) {
            throw new IllegalStateException("decrypt failed");
        }
    }
}
