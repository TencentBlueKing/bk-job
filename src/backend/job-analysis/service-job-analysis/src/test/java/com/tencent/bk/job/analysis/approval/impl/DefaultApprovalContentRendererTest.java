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
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayMasker;
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
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
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
    private static final String SECTION_GLOBAL_VARS = "## task.approval.content.section.globalVars";
    private static final String TABLE_HEADER =
        "| task.approval.content.table.item | task.approval.content.table.value |";
    private static final String VAR_TABLE_HEADER = "| task.approval.content.table.varName |"
        + " task.approval.content.table.varType | task.approval.content.table.varValue |";

    private static final String TRANSFER_MODE = "transfer_mode";
    private static final String FILE_SOURCE_LIST = "file_source_list";
    private static final String FILE_TARGET_PATH = "file_target_path";
    private static final String FILE_TARGET_NAME = "file_target_name";

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

        // i18n 替身原样回显 key，便于断言"用了哪个文案"而不依赖具体译文；
        // 带参文案把参数拼在 key 后面，否则主机清单这类"文案里只有参数才是关键信息"的行断言不到东西
        i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        // 匹配整个变长实参数组而不是 any()：Mockito 5 的 any() 在变长位置只匹配一个实参，
        // 主机清单这类带两个参数的文案会漏配成 null
        when(i18nService.getI18nWithArgs(anyString(), any(Object[].class)))
            .thenAnswer(invocation -> echoKeyWithArgs(invocation.getArguments()));

        cryptorSupport = new ApprovalParamsCryptorTestSupport();
        cryptorSupport.givenPlanVars(PLAN_ID,
            cryptorSupport.cipherVar(1L, "password"),
            cryptorSupport.stringVar(2L, "port"));
        cryptoService = new ApprovalParamsCryptoServiceImpl(
            new ApprovalParamsCryptorRegistry(cryptorSupport.allCryptors()));

        renderer = new DefaultApprovalContentRenderer(i18nService, appService, cryptoService,
            new ApprovalDisplayMasker(i18nService));
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

    @ParameterizedTest(name = "显式指定 {0} 时概要展示该模式本身")
    @DisplayName("步骤明细不展示，但实际生效的分发模式汇总成一行：文件怎么落盘是审批人必须看到的")
    @ValueSource(strings = {"STRICT", "FORCE", "SAFETY_IP_PREFIX", "SAFETY_DATE_PREFIX"})
    void givenExplicitTransferModeThenShowResolvedMode(String transferMode) {
        ApprovalContent rendered = renderer.render(fileTask(buildFileSummary(transferMode, null)));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.transfer_mode"))
            .contains("task.approval.content.value.transferMode." + transferMode)
            .as("显式指定过的模式不能标成按默认生效")
            .doesNotContain("task.approval.content.defaultPrefix");
    }

    @Test
    @DisplayName("未指定分发模式时展示默认生效的强制模式并标注默认，且只出一行")
    void givenTransferModeDefaultAppliedThenMarkDefaultOnce() {
        ApprovalContent rendered = renderer.render(fileTask(buildFileSummary("FORCE", "FORCE")));

        String content = rendered.getApprovalContent();
        assertThat(tableRow(content, "content.field.transfer_mode"))
            .contains("task.approval.content.defaultPrefix")
            .contains("task.approval.content.value.transferMode.FORCE");
        assertThat(Arrays.stream(content.split("\n"))
            .filter(line -> line.contains("content.field.transfer_mode"))
            .count())
            .as("默认项与概要行说的是同一件事，不能重复出行")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("步骤没带出分发模式时退回展示默认生效的模式，整行不能消失")
    void givenTransferModeOnlyInDefaultsThenStillShow() {
        ResolvedSummary summary = buildFileSummary(null, "FORCE");

        ApprovalContent rendered = renderer.render(fileTask(summary));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.transfer_mode"))
            .contains("task.approval.content.defaultPrefix")
            .contains("task.approval.content.value.transferMode.FORCE");
    }

    @Test
    @DisplayName("带默认前缀的行统一沉底，普通字段行保持原有相对顺序")
    void givenDefaultsAppliedThenSinkToBottomOfTable() {
        ResolvedSummary summary = buildFileSummary("FORCE", "FORCE");
        summary.addField("job_plan_id", String.valueOf(PLAN_ID));
        summary.addDefaultApplied("timeout", "7200");

        String content = renderer.render(fileTask(summary)).getApprovalContent();

        List<String> labels = summaryRowLabels(content);
        assertThat(labels)
            .as("默认值提示聚到表格最后，不再夹在普通字段行中间")
            .endsWith("task.approval.content.defaultPrefix task.approval.content.field.transfer_mode",
                "task.approval.content.defaultPrefix task.approval.content.field.timeout");
        assertThat(labels)
            .as("普通字段行的相对顺序不变")
            .containsSubsequence("task.approval.content.field.file_source_list",
                "task.approval.content.field.file_target_path",
                "task.approval.content.field.job_plan_id");
        assertThat(labels.stream().filter(label -> label.contains("field.transfer_mode")).count())
            .as("沉底的是同一行而不是多出一行")
            .isEqualTo(1);
    }

    @Test
    @DisplayName("显式指定分发模式时该行留在原位，不跟着默认值提示沉底")
    void givenExplicitTransferModeThenKeepRowInPlace() {
        ResolvedSummary summary = buildFileSummary("STRICT", null);
        summary.addField("job_plan_id", String.valueOf(PLAN_ID));
        summary.addDefaultApplied("timeout", "7200");

        String content = renderer.render(fileTask(summary)).getApprovalContent();

        assertThat(summaryRowLabels(content))
            .containsSubsequence("task.approval.content.field.transfer_mode",
                "task.approval.content.field.job_plan_id",
                "task.approval.content.defaultPrefix task.approval.content.field.timeout");
    }

    @Test
    @DisplayName("非文件分发场景不出文件相关行，概要里不塞无关项")
    void givenNoFileStepThenNoFileRows() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        assertThat(rendered.getApprovalContent())
            .doesNotContain("content.field.transfer_mode")
            .doesNotContain("content.field.file_source_list")
            .doesNotContain("content.field.file_target_path")
            .doesNotContain("content.field.file_target_name");
    }

    @Test
    @DisplayName("文件分发的源文件与目标路径汇总进概要：会不会覆盖生产目录，审批人得看得见")
    void givenFileStepThenShowSourceAndTargetPath() {
        ApprovalContent rendered = renderer.render(fileTask(buildFileSummary("FORCE", null)));

        String content = rendered.getApprovalContent();
        assertThat(tableRow(content, "content.field.file_source_list"))
            .contains("root: /data/a.tar.gz");
        assertThat(tableRow(content, "content.field.file_target_path")).contains("/tmp/");
    }

    @Test
    @DisplayName("源文件带出取文件的身份与源机器：只给路径，审批人不知道文件从哪台机器上来")
    void givenStructuredFileSourceThenShowAccountAndHosts() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.addStep(structuredFileStep(fileSource("root", 1, "/data/a.tar.gz", "/data/b.tar.gz")));

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), FILE_SOURCE_LIST))
            .contains("root@0:127.0.0.1: /data/a.tar.gz,/data/b.tar.gz");
    }

    @Test
    @DisplayName("本地上传的文件标成本地文件：它没有源机器与源账号，空着会被当成漏填")
    void givenLocalUploadFileSourceThenMarkAsLocalFile() {
        ResolvedSummary.ResolvedFileSource fileSource = new ResolvedSummary.ResolvedFileSource();
        fileSource.setLocalUpload(true);
        fileSource.addFilePath("/tmp/20260901/app.sh");
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.addStep(structuredFileStep(fileSource));

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), FILE_SOURCE_LIST))
            .contains("task.approval.content.value.localFile /tmp/20260901/app.sh");
    }

    @Test
    @DisplayName("源机器超过上限时只报台数：几十台源机器逐台列出会把这一行铺成一堵墙")
    void givenFileSourceHostsOverLimitThenShowHostCount() {
        int hostCount = ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 2;
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.addStep(structuredFileStep(fileSource("root", hostCount, "/data/a.tar.gz")));

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), FILE_SOURCE_LIST))
            .contains("root@task.approval.content.value.fileSourceHostCount(" + hostCount + ")")
            .doesNotContain("0:127.0.0.1");
    }

    @Test
    @DisplayName("老单据的快照里源文件是一个拼好的字符串，仍按老方式展示，不能整行消失")
    void givenLegacyFileSourceSnapshotThenStillShow() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.addStep(fileStep("/tmp/", "root: /data/legacy.tar.gz"));

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), FILE_SOURCE_LIST))
            .contains("root: /data/legacy.tar.gz");
    }

    @Test
    @DisplayName("执行对象不超上限时逐个列出：这次到底动哪几台，只给一个数字等于让人凭空想象")
    void givenFewExecuteObjectsThenListThem() {
        ResolvedSummary summary = summaryWithExecuteObjects(3, 3);

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), "content.executeObjects"))
            .contains("task.approval.content.value.executeObjectList(3,0:127.0.0.1; 0:127.0.0.2; 0:127.0.0.3)");
    }

    @Test
    @DisplayName("执行对象超过上限时只报总数：上千台逐个列出只会让人直接划到底放行")
    void givenManyExecuteObjectsThenOnlyShowCount() {
        int totalCount = ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1;
        ResolvedSummary summary = summaryWithExecuteObjects(totalCount, totalCount);

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), "content.executeObjects"))
            .contains("task.approval.content.value.executeObjectCount(" + totalCount + ")")
            .doesNotContain("0:127.0.0.1");
    }

    @Test
    @DisplayName("清单被快照上限截断时只报总数：拿截断后的一部分当全部列出来就是在骗审批人")
    void givenTruncatedExecuteObjectListThenOnlyShowCount() {
        ResolvedSummary summary = summaryWithExecuteObjects(3, 8);

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(), "content.executeObjects"))
            .contains("task.approval.content.value.executeObjectCount(8)")
            .doesNotContain("0:127.0.0.1");
    }

    @Test
    @DisplayName("脚本参数进概要：同一份脚本，参数决定了这次到底干什么")
    void givenScriptParamThenShowInSummary() {
        ResolvedSummary summary = buildFullSummary();
        summary.getSteps().get(0).setScriptParam("--env=prod --force");
        summary.getSteps().get(0).setParamSensitive(false);

        assertThat(tableRow(renderer.render(scriptTask(summary, false)).getApprovalContent(),
            "content.scriptParam")).contains("--env=prod --force");
    }

    @Test
    @DisplayName("敏感脚本参数只出占位符，但那一行必须在：空着会被读成这次执行不带参数")
    void givenSensitiveScriptParamThenShowMask() {
        ResolvedSummary summary = buildFullSummary();
        summary.getSteps().get(0).setParamSensitive(true);

        ApprovalContent rendered = renderer.render(scriptTask(summary, true));

        assertThat(tableRow(rendered.getApprovalContent(), "content.scriptParam"))
            .contains("task.approval.content.value.scriptParamSensitive(******)");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("没传脚本参数时不出这一行，免得单据上多一行空白")
    void givenNoScriptParamThenNoRow() {
        String content = renderer.render(scriptTask(buildFullSummary(), false)).getApprovalContent();

        assertThat(content).doesNotContain("content.scriptParam");
    }

    @Test
    @DisplayName("执行方案的文件步骤定义在方案里、入参只有 plan_id，概要不汇总就等于完全看不到")
    void givenJobPlanWithFileStepsThenShowMergedFileInfo() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN.name());
        summary.setName("发布流程");
        summary.addField("job_plan_id", String.valueOf(PLAN_ID));
        summary.addStep(fileStep("/data/app/", "root: /pkg/app.tar.gz"));
        summary.addStep(fileStep("/data/app/", "mysql: /pkg/conf.yaml"));
        ResolvedSummary.ResolvedStep scriptStep = new ResolvedSummary.ResolvedStep();
        scriptStep.setExecuteType("EXECUTE_SCRIPT");
        summary.addStep(scriptStep);

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, summary, null));

        String content = rendered.getApprovalContent();
        assertThat(tableRow(content, "content.field.file_source_list"))
            .contains("/pkg/app.tar.gz")
            .contains("/pkg/conf.yaml");
        assertThat(tableRow(content, "content.field.file_target_path"))
            .as("两个步骤打的是同一个目标目录，只列一次")
            .isEqualTo("| task.approval.content.field.file_target_path | /data/app/ |");
    }

    @Test
    @DisplayName("源文件超过上限只列前几条并补上总数，不把单据重新撑成一堵墙")
    void givenTooManyFileSourcesThenTruncateWithTotalCount() {
        List<String> fileSources = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            fileSources.add("root: /pkg/app-" + i + ".tar.gz");
        }
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.addStep(fileStep("/tmp/", String.join(ResolvedSummary.ITEM_SEPARATOR, fileSources)));

        ApprovalContent rendered = renderer.render(fileTask(summary));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.file_source_list"))
            .contains("/pkg/app-0.tar.gz")
            .contains("/pkg/app-4.tar.gz")
            .doesNotContain("/pkg/app-5.tar.gz")
            .contains("task.approval.content.value.itemTruncated");
    }

    @ParameterizedTest(name = "目标状态 {0} 翻译成文案而不是枚举名")
    @DisplayName("定时任务的目标状态是枚举名，审批人看不懂启的还是停的，须翻译成文案")
    @ValueSource(strings = {"ENABLED", "DISABLED"})
    void givenCronTargetStatusThenTranslateValue(String targetStatus) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS.name());
        summary.setName("每天凌晨清理日志");
        summary.addField("cron_id", "100");
        summary.addField("target_status", targetStatus);

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, summary, null));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.target_status"))
            .contains("task.approval.content.value.cronStatus." + targetStatus)
            .as("翻译后不该再把枚举名摊给审批人")
            .doesNotContain("| " + targetStatus + " |");
    }

    @ParameterizedTest(name = "新增/修改 {0} 翻译成文案而不是枚举名")
    @DisplayName("定时任务是新增还是修改，同样翻译成文案：CREATE / UPDATE 审批人看不懂")
    @ValueSource(strings = {"CREATE", "UPDATE"})
    void givenCronOperationThenTranslateValue(String operation) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.SAVE_CRON.name());
        summary.addField("operation", operation);

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.SAVE_CRON, summary, null));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.operation"))
            .contains("task.approval.content.value.operation." + operation)
            .as("翻译后不该再把枚举名摊给审批人")
            .doesNotContain("| " + operation + " |");
    }

    @Test
    @DisplayName("白名单外的字段取值原样展示：定时规则这类自由值不能被当成枚举名去翻译")
    void givenNonEnumFieldsThenKeepValueAsIs() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.SAVE_CRON.name());
        summary.addField("cron_expression", "0 2 * * *");
        summary.addField("execute_time_zone", "Asia/Shanghai");

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.SAVE_CRON, summary, null));

        String content = rendered.getApprovalContent();
        assertThat(tableRow(content, "content.field.cron_expression")).contains("0 2 * * *");
        assertThat(tableRow(content, "content.field.execute_time_zone")).contains("Asia/Shanghai");
        assertThat(content).doesNotContain("task.approval.content.value.cron_expression");
    }

    @ParameterizedTest(name = "字段 {0} 的明细单独成章节")
    @DisplayName("启用的步骤逐行列进独立章节，表格里只报条数：单元格塞不下换行")
    @ValueSource(strings = {"enable_steps", "enable_steps_all"})
    void givenMultiLineFieldThenRenderAsListSection(String fieldLabel) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.CREATE_JOB_PLAN.name());
        summary.setName("发布方案");
        summary.addField("job_template_id", "300");
        summary.addField(fieldLabel, "停止服务\n分发安装包\n启动服务");

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        String heading = "## task.approval.content.field." + fieldLabel;
        assertThat(tableRow(content, "content.field." + fieldLabel))
            .contains("task.approval.content.value.itemCount")
            .as("步骤名进了单元格，要么带出 <br>，要么把表格从该行起切断")
            .doesNotContain("停止服务");
        assertThat(sectionOf(content, heading))
            .as("明细走无序列表，一行一个步骤名")
            .contains("- 停止服务\n- 分发安装包\n- 启动服务");
        assertThat(indexOf(content, SECTION_SUMMARY)).isLessThan(indexOf(content, heading));
    }

    @Test
    @DisplayName("正文里不出现内联 HTML：审批渠道的渲染器会把标签原样展示给审批人")
    void givenValueWithNewLineThenNoInlineHtml() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.CREATE_JOB_PLAN.name());
        summary.setName("发布\n方案");
        summary.addField("enable_steps", "停止服务\n启动服务");

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(content).doesNotContain("<br>");
        assertThat(tableRow(content, "content.name"))
            .as("普通字段里混进的换行压成空格，不能把表格切断")
            .contains("发布 方案");
    }

    @Test
    @DisplayName("目标状态缺文案时原样展示枚举名，不能让整行变空")
    void givenCronStatusI18nMissingThenFallbackToEnumName() {
        when(i18nService.getI18n("task.approval.content.value.cronStatus.ENABLED"))
            .thenThrow(new IllegalStateException("no such message"));
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS.name());
        summary.addField("target_status", "ENABLED");

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, summary, null));

        assertThat(tableRow(rendered.getApprovalContent(), "content.field.target_status"))
            .contains("ENABLED");
    }

    @Test
    @DisplayName("目标文件名通常不填，为空时不出行，填了才展示")
    void givenFileTargetNameThenShowOnlyWhenPresent() {
        assertThat(renderer.render(fileTask(buildFileSummary("FORCE", null))).getApprovalContent())
            .doesNotContain("content.field.file_target_name");

        ResolvedSummary summary = buildFileSummary("FORCE", null);
        summary.getSteps().get(0).addField(FILE_TARGET_NAME, "app-renamed.tar.gz");

        assertThat(tableRow(renderer.render(fileTask(summary)).getApprovalContent(),
            "content.field.file_target_name")).contains("app-renamed.tar.gz");
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
    @DisplayName("脚本内容单独成章、按脚本语言高亮展示并已解码：不展示则审批人无从判断风险")
    void givenScriptContentThenShowDecodedPlainTextInOwnSection() {
        ApprovalContent rendered = renderer.render(scriptTask(buildFullSummary(), true));

        String scriptSection = sectionOf(rendered.getApprovalContent(), SECTION_SCRIPT);
        assertThat(scriptSection)
            .contains("```shell\n" + SCRIPT_CONTENT + "\n```");
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
        request.setScriptLanguage(ScriptTypeEnum.PYTHON.getValue());

        ApprovalContent rendered = renderer.render(taskOf(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildFullSummary(), request));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_SCRIPT))
            .contains("````python\n" + script + "\n````");
    }

    @Test
    @DisplayName("脚本语言未知时不写语言标记，代码块本身仍然成立")
    void givenUnknownScriptLanguageThenNoLanguageTag() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("quick-script");
        request.setContent(base64(SCRIPT_CONTENT));

        ApprovalContent rendered = renderer.render(taskOf(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildFullSummary(), request));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_SCRIPT))
            .contains("```\n" + SCRIPT_CONTENT + "\n```");
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
    @DisplayName("脚本参数没按协议做 BASE64 时原样展示：解出来的二进制垃圾比原值更没法看")
    void givenNotBase64ScriptParamThenShowRawValue() {
        V4FastExecuteScriptRequest request = buildScriptRequest(false);
        // "111" 会被宽松解码器解出 0xD7 0x5D，直接按 UTF-8 展示就是一串乱码
        request.setScriptParam("111");

        ApprovalContent rendered = renderer.render(
            taskOf(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, buildFullSummary(), request));

        assertThat(sectionOf(rendered.getApprovalContent(), SECTION_RAW_PARAMS))
            .contains("\"111\"")
            .doesNotContain("\uFFFD");
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
    @DisplayName("全局变量逐个列进独立章节，概要表格里只报条数：变量是这次操作拿什么参数去跑的直接答案")
    void givenGlobalVarsThenRenderAsOwnSection() {
        ResolvedSummary summary = planSummaryWithGlobalVars();

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "content.field.global_vars"))
            .contains("task.approval.content.value.itemCount(4)");
        String section = sectionOf(content, SECTION_GLOBAL_VARS);
        assertThat(section)
            .as("变量、类型、取值三列对照，审批人才看得出哪个变量指向了哪批机器")
            .contains(VAR_TABLE_HEADER + "\n| --- | --- | --- |");
        assertThat(section)
            .contains("| version | task.approval.content.value.varType.STRING | v1.2.3 |")
            .contains("task.approval.content.value.varType.EXECUTE_OBJECT_LIST");
        assertThat(indexOf(content, SECTION_SUMMARY)).isLessThan(indexOf(content, SECTION_GLOBAL_VARS));
    }

    @Test
    @DisplayName("主机变量台数不超上限时逐台列出：具体是哪批机器，审批人得看得见")
    void givenHostVarWithinLimitThenListHosts() {
        ResolvedSummary summary = planSummaryWithGlobalVars();

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "| target_hosts |"))
            .contains("task.approval.content.value.varHostList(3,0:127.0.0.1; 0:127.0.0.2; 0:127.0.0.3)");
    }

    @Test
    @DisplayName("主机变量台数超过上限时只报台数：上千台逐台列出只会让人直接划到底放行")
    void givenHostVarOverLimitThenOnlyShowCount() {
        ResolvedSummary summary = planSummaryWithGlobalVars();

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "batch_hosts"))
            .contains("task.approval.content.value.varHostCount(" + (ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1)
                + ")")
            .as("超上限的变量不该再逐台列出")
            .doesNotContain("0:127.1.0.1");
    }

    @Test
    @DisplayName("动态分组与拓扑节点算不出台数，只报个数")
    void givenDynamicTargetVarThenShowEntryCount() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.CREATE_JOB_PLAN.name());
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.setName("dynamic_hosts");
        globalVar.setType("EXECUTE_OBJECT_LIST");
        globalVar.setDynamicGroupCount(2);
        globalVar.setTopoNodeCount(3);
        globalVar.setContainerCount(4);
        summary.addGlobalVar(globalVar);

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "dynamic_hosts"))
            .contains("task.approval.content.value.varDynamicGroupCount(2)")
            .contains("task.approval.content.value.varTopoNodeCount(3)")
            .contains("task.approval.content.value.varContainerCount(4)");
    }

    @Test
    @DisplayName("密文变量只出占位符：概要以明文落库，真实取值不该出现在任何一处")
    void givenCipherVarThenOnlyShowMask() {
        ResolvedSummary summary = planSummaryWithGlobalVars();

        ApprovalContent rendered = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null));

        assertThat(tableRow(rendered.getApprovalContent(), "db_password")).contains("******");
        assertContentFreeOfSecrets(rendered);
    }

    @Test
    @DisplayName("沿用现值的变量加前缀并沉底：同一个取值是本次改成这样还是一直如此，审批结论可能完全不同")
    void givenNotAssignedVarThenPrefixedAndSunkToBottom() {
        ResolvedSummary summary = planSummaryWithGlobalVars();

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        List<String> varNames = globalVarRowNames(content);
        assertThat(varNames)
            .endsWith("task.approval.content.value.varNotAssignedPrefix batch_hosts");
        assertThat(varNames)
            .as("本次指定的变量保持原有相对顺序")
            .containsSubsequence("version", "target_hosts", "db_password");
    }

    @Test
    @DisplayName("变量取值为空时不留空单元格，给出「未设置取值」")
    void givenVarWithoutValueThenShowNoValueHint() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.SAVE_CRON.name());
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.setName("empty_var");
        globalVar.setType("STRING");
        summary.addGlobalVar(globalVar);

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.SAVE_CRON, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "empty_var")).contains("task.approval.content.value.varNoValue");
    }

    @Test
    @DisplayName("没有全局变量时不出章节，也不在概要里留空行")
    void givenNoGlobalVarThenNoSection() {
        String content = renderer.render(scriptTask(buildFullSummary(), true)).getApprovalContent();

        assertThat(content)
            .doesNotContain(SECTION_GLOBAL_VARS)
            .doesNotContain("content.field.global_vars");
    }

    @Test
    @DisplayName("主机变量台数计入风险等级：创建执行方案没有执行对象总数，影响面全在变量里")
    void givenHostVarCountThenRaiseRiskLevel() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.CREATE_JOB_PLAN.name());
        summary.addGlobalVar(hostVar("target_hosts", "127.0.0.", 101, true));

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "riskLevel")).contains(ApprovalRiskLevelEnum.HIGH.getNameI18nKey());
    }

    @Test
    @DisplayName("与执行对象总数取较大值而非相加：启动执行方案时主机变量已被算进执行对象总数")
    void givenHostVarAndExecuteObjectsThenTakeMaxNotSum() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN.name());
        summary.setTotalExecuteObjectCount(100);
        summary.addGlobalVar(hostVar("target_hosts", "127.0.0.", 100, true));

        String content = renderer.render(
            buildTask(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, summary, null)).getApprovalContent();

        assertThat(tableRow(content, "riskLevel"))
            .as("相加会把同一批机器算两次，把中危误判成高危")
            .contains(ApprovalRiskLevelEnum.MEDIUM.getNameI18nKey());
    }

    @Test
    @DisplayName("参数渲染失败时概要仍然可用，只在参数章节给出提示")
    void givenParamsDecryptFailThenKeepSummaryUsable() {
        DefaultApprovalContentRenderer failingRenderer = new DefaultApprovalContentRenderer(
            i18nService, appService, new FailingParamsCryptoService(), new ApprovalDisplayMasker(i18nService));

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
     * 把 key 与参数一起回显，形如 {@code key(参数1,参数2)}
     */
    private static String echoKeyWithArgs(Object[] invocationArgs) {
        List<Object> args = new ArrayList<>();
        for (int i = 1; i < invocationArgs.length; i++) {
            if (invocationArgs[i] instanceof Object[]) {
                args.addAll(Arrays.asList((Object[]) invocationArgs[i]));
            } else {
                args.add(invocationArgs[i]);
            }
        }
        return invocationArgs[0] + "(" + args.stream().map(String::valueOf)
            .collect(Collectors.joining(",")) + ")";
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
     * 一份"该有的变量形态都有"的创建执行方案概要：普通变量 + 台数在上限内的主机变量 +
     * 密文变量 + 沿用现值且台数超上限的主机变量
     */
    private ResolvedSummary planSummaryWithGlobalVars() {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.CREATE_JOB_PLAN.name());
        summary.setName("发布流程-灰度");
        summary.addField("job_template_id", "1000");

        ResolvedSummary.ResolvedGlobalVar stringVar = new ResolvedSummary.ResolvedGlobalVar();
        stringVar.setName("version");
        stringVar.setType("STRING");
        stringVar.setValue("v1.2.3");
        stringVar.setAssigned(true);
        summary.addGlobalVar(stringVar);

        summary.addGlobalVar(hostVar("target_hosts", "127.0.0.", 3, true));

        ResolvedSummary.ResolvedGlobalVar cipherVar = new ResolvedSummary.ResolvedGlobalVar();
        cipherVar.setName("db_password");
        cipherVar.setType("CIPHER");
        cipherVar.setAssigned(true);
        summary.addGlobalVar(cipherVar);

        summary.addGlobalVar(hostVar("batch_hosts", "127.1.0.",
            ResolvedSummary.MAX_DISPLAY_ITEM_COUNT + 1, false));
        return summary;
    }

    private ResolvedSummary.ResolvedGlobalVar hostVar(String name,
                                                      String ipPrefix,
                                                      int hostCount,
                                                      boolean assigned) {
        ResolvedSummary.ResolvedGlobalVar globalVar = new ResolvedSummary.ResolvedGlobalVar();
        globalVar.setName(name);
        globalVar.setType("EXECUTE_OBJECT_LIST");
        globalVar.setAssigned(assigned);
        for (int i = 1; i <= hostCount; i++) {
            globalVar.addHost((long) i, "0:" + ipPrefix + i);
        }
        return globalVar;
    }

    /**
     * 按渲染顺序取出全局变量表格各行的变量名，用于校验沿用现值的行是否沉底
     */
    private List<String> globalVarRowNames(String content) {
        return Arrays.stream(sectionOf(content, SECTION_GLOBAL_VARS).split("\n"))
            .filter(line -> line.startsWith("|"))
            .map(line -> line.split("\\|")[1].trim())
            .filter(name -> !"---".equals(name) && !name.endsWith("table.varName"))
            .collect(Collectors.toList());
    }

    /**
     * 按渲染顺序取出概要表格各行的标签，用于校验行顺序
     */
    private List<String> summaryRowLabels(String content) {
        return Arrays.stream(sectionOf(content, SECTION_SUMMARY).split("\n"))
            .filter(line -> line.startsWith("|"))
            .map(line -> line.split("\\|")[1].trim())
            .filter(label -> !"---".equals(label) && !label.endsWith("table.item"))
            .collect(Collectors.toList());
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

    /**
     * 文件分发单据：分发模式由预检解析后写在步骤上，未显式指定时另有一条默认生效记录
     *
     * @param stepTransferMode     步骤解析出的实际生效模式，为 null 表示步骤未带出
     * @param defaultAppliedMode   按默认生效的模式，为 null 表示调用方显式指定过
     */
    private ResolvedSummary buildFileSummary(String stepTransferMode, String defaultAppliedMode) {
        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.setName("分发安装包");
        summary.setTotalExecuteObjectCount(3);
        ResolvedSummary.ResolvedStep step = fileStep("/tmp/", "root: /data/a.tar.gz");
        step.addField(TRANSFER_MODE, stepTransferMode);
        summary.addStep(step);
        if (defaultAppliedMode != null) {
            summary.addDefaultApplied(TRANSFER_MODE, defaultAppliedMode);
        }
        return summary;
    }

    /**
     * 一个文件分发步骤，字段名与 job-execute 侧 ResolvedSummaryBuilder 写入的保持一致
     */
    private ResolvedSummary.ResolvedStep fileStep(String targetPath, String fileSourceList) {
        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setExecuteType("SEND_FILE");
        step.setAccountAlias("root");
        step.addField(FILE_TARGET_PATH, targetPath);
        step.addField(FILE_SOURCE_LIST, fileSourceList);
        return step;
    }

    private ApprovalTaskDTO fileTask(ResolvedSummary summary) {
        return buildTask(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE, summary, null);
    }

    /**
     * 结构化源文件的文件分发步骤，与 job-execute 侧新版 ResolvedSummaryBuilder 写入的形态一致
     */
    private ResolvedSummary.ResolvedStep structuredFileStep(ResolvedSummary.ResolvedFileSource... fileSources) {
        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setExecuteType("SEND_FILE");
        step.setAccountAlias("root");
        step.addField(FILE_TARGET_PATH, "/tmp/");
        for (ResolvedSummary.ResolvedFileSource fileSource : fileSources) {
            step.addFileSource(fileSource);
        }
        return step;
    }

    private ResolvedSummary.ResolvedFileSource fileSource(String accountAlias, int hostCount, String... filePaths) {
        ResolvedSummary.ResolvedFileSource fileSource = new ResolvedSummary.ResolvedFileSource();
        fileSource.setAccountAlias(accountAlias);
        for (int i = 1; i <= hostCount; i++) {
            fileSource.addHost((long) i, "0:127.0.0." + i);
        }
        for (String filePath : filePaths) {
            fileSource.addFilePath(filePath);
        }
        return fileSource;
    }

    /**
     * @param listedCount 步骤里逐条带回的执行对象条数，小于总数即表示清单被快照上限截断过
     * @param totalCount  跨步骤去重后的执行对象总数
     */
    private ResolvedSummary summaryWithExecuteObjects(int listedCount, int totalCount) {
        List<ResolvedSummary.ResolvedExecuteObject> executeObjects = new ArrayList<>(listedCount);
        for (int i = 1; i <= listedCount; i++) {
            executeObjects.add(new ResolvedSummary.ResolvedExecuteObject("HOST", (long) i, "0:127.0.0." + i));
        }
        ResolvedSummary.ResolvedStep step = new ResolvedSummary.ResolvedStep();
        step.setExecuteType("SEND_FILE");
        step.setExecuteObjects(executeObjects);
        step.setExecuteObjectCount(totalCount);
        step.setExecuteObjectTruncated(listedCount < totalCount);

        ResolvedSummary summary = new ResolvedSummary();
        summary.setOperationType(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE.name());
        summary.setTotalExecuteObjectCount(totalCount);
        summary.addStep(step);
        return summary;
    }

    private V4FastExecuteScriptRequest buildScriptRequest(boolean paramSensitive) {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("quick-script");
        request.setContent(base64(SCRIPT_CONTENT));
        request.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
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
