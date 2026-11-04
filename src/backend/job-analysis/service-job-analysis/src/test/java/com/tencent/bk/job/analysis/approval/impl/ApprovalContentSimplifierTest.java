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

import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSection;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.GLOBAL_VARS;
import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.MULTI_LINE;
import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.RAW_PARAMS;
import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.SCRIPT;
import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.SUMMARY;
import static com.tencent.bk.job.analysis.approval.channel.model.ApprovalContentSectionKind.TITLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 审批简要内容裁剪。
 * <p>
 * 传入渲染产出的 typed sections，不解析 Markdown。丢弃顺序必须是：
 * 原始参数 → 脚本内容 → 全局变量 → 逐行展示字段；操作概要整章不丢，交给截断。
 */
class ApprovalContentSimplifierTest {

    private static final String TITLE_MD = "# 快速执行脚本 - 蓝鲸 - 1个执行对象\n\n";
    private static final String SUMMARY_MD = "## 操作概要\n\n| 项目 | 内容 |\n| --- | --- |\n| 账号 | root |\n\n";
    private static final String STEPS_MD = "## 启用的步骤\n\n- 停止服务\n- 启动服务\n\n";
    private static final String GLOBAL_VARS_MD = "## 全局变量\n\n| 变量 | 类型 | 取值 |\n| --- | --- |\n| v | 字符串 | 1 |\n\n";
    private static final String SCRIPT_MD = "## 脚本内容\n\n```shell\necho hello\n```\n\n";
    private static final String RAW_PARAMS_MD = "## 原始参数\n\n```json\n{\"a\":1}\n```\n";
    private static final String ZH_TRUNCATED = "（内容过长已截断）";
    private static final String EN_TRUNCATED = "(Content truncated due to length limit)";

    private ApprovalProperties properties;
    private MessageI18nService i18nService;
    private ApprovalContentSimplifier simplifier;

    @BeforeEach
    void setUp() {
        properties = new ApprovalProperties();
        i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenReturn(ZH_TRUNCATED);
        simplifier = new ApprovalContentSimplifier(properties, i18nService);
    }

    @Test
    @DisplayName("未超限时简要内容与全文相同")
    void underLimitReturnsOriginal() {
        ApprovalContent content = contentOf(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            section(SCRIPT, SCRIPT_MD),
            section(RAW_PARAMS, RAW_PARAMS_MD)
        );
        properties.setContentSimpleMaxBytes(utf8Len(content.getApprovalContent()) + 10);

        assertThat(simplifier.simplify(content)).isEqualTo(content.getApprovalContent());
    }

    @Test
    @DisplayName("null 原样返回 null，空串原样返回空串")
    void nullAndEmptyStayUnchanged() {
        assertThat(simplifier.simplify((ApprovalContent) null)).isNull();
        assertThat(simplifier.simplify((List<ApprovalContentSection>) null)).isNull();

        ApprovalContent empty = new ApprovalContent();
        empty.setApprovalContent("");
        assertThat(simplifier.simplify(empty)).isEmpty();

        ApprovalContent missing = new ApprovalContent();
        assertThat(simplifier.simplify(missing)).isNull();
    }

    @Test
    @DisplayName("仅原始参数导致超限时丢掉原始参数整章，无截断后缀")
    void dropRawParamsOnly() {
        ApprovalContentSection hugeRaw = section(RAW_PARAMS, "```json\n" + "x".repeat(200) + "\n```\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            section(SCRIPT, SCRIPT_MD),
            hugeRaw
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD + SCRIPT_MD) + 8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD + SCRIPT_MD);
        assertThat(simple).doesNotContain(hugeRaw.getMarkdown());
        assertThat(simple).doesNotContain(ZH_TRUNCATED);
    }

    @Test
    @DisplayName("无脚本时原始参数和全局变量仍超限：先丢原始参数，再丢全局变量")
    void dropRawParamsThenGlobalVars() {
        ApprovalContentSection hugeVars = section(GLOBAL_VARS, "| 变量 | " + "v".repeat(80) + " |\n\n");
        ApprovalContentSection hugeRaw = section(RAW_PARAMS, "```json\n" + "r".repeat(80) + "\n```\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            hugeVars,
            hugeRaw
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD) + 8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD);
        assertThat(simple).doesNotContain(hugeVars.getMarkdown());
        assertThat(simple).doesNotContain(hugeRaw.getMarkdown());
        assertThat(simple).doesNotContain(ZH_TRUNCATED);
    }

    @Test
    @DisplayName("脚本极大时先丢原始参数再丢脚本，保留标题、操作概要、启用的步骤和全局变量")
    void hugeScriptDroppedBeforeStepsAndVars() {
        ApprovalContentSection hugeScript = section(SCRIPT, "```shell\n" + "s".repeat(300) + "\n```\n\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            section(MULTI_LINE, STEPS_MD),
            section(GLOBAL_VARS, GLOBAL_VARS_MD),
            hugeScript,
            section(RAW_PARAMS, RAW_PARAMS_MD)
        );
        int keptLen = utf8Len(TITLE_MD + SUMMARY_MD + STEPS_MD + GLOBAL_VARS_MD);
        properties.setContentSimpleMaxBytes(keptLen + 8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD + STEPS_MD + GLOBAL_VARS_MD);
        assertThat(simple).contains(STEPS_MD);
        assertThat(simple).contains(GLOBAL_VARS_MD);
        assertThat(simple).doesNotContain(hugeScript.getMarkdown());
        assertThat(simple).doesNotContain(RAW_PARAMS_MD);
        assertThat(simple).doesNotContain(ZH_TRUNCATED);
    }

    @Test
    @DisplayName("丢弃顺序：原始参数 → 脚本 → 全局变量 → 逐行字段；操作概要不整章丢")
    void dropOrderRawScriptVarsThenMultiLine() {
        ApprovalContentSection hugeSteps = section(MULTI_LINE, "- " + "step".repeat(40) + "\n\n");
        ApprovalContentSection hugeVars = section(GLOBAL_VARS, "| v | " + "g".repeat(40) + " |\n\n");
        ApprovalContentSection hugeScript = section(SCRIPT, "```shell\n" + "s".repeat(80) + "\n```\n\n");
        ApprovalContentSection hugeRaw = section(RAW_PARAMS, "```json\n" + "r".repeat(80) + "\n```\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            hugeSteps,
            hugeVars,
            hugeScript,
            hugeRaw
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD) + 8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD);
        assertThat(simple).doesNotContain(hugeSteps.getMarkdown());
        assertThat(simple).doesNotContain(hugeVars.getMarkdown());
        assertThat(simple).doesNotContain(hugeScript.getMarkdown());
        assertThat(simple).doesNotContain(hugeRaw.getMarkdown());
    }

    @Test
    @DisplayName("只有标题、概要和原始参数时只丢原始参数")
    void titleSummaryAndRawParamsDropsRawOnly() {
        ApprovalContentSection hugeRaw = section(RAW_PARAMS, "```json\n" + "r".repeat(120) + "\n```\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            hugeRaw
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD) + 8);

        assertThat(simplifier.simplify(sections)).isEqualTo(TITLE_MD + SUMMARY_MD);
    }

    @Test
    @DisplayName("同优先级多章从文档后往前丢")
    void samePriorityDroppedFromBack() {
        ApprovalContentSection steps = section(MULTI_LINE, "## 启用的步骤\n\n- a\n\n");
        ApprovalContentSection stepsAll = section(MULTI_LINE, "## 启用的步骤（全部）\n\n- " + "b".repeat(80) + "\n\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            steps,
            stepsAll
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD + steps.getMarkdown()) + 4);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD + steps.getMarkdown());
        assertThat(simple).doesNotContain(stepsAll.getMarkdown());
    }

    @Test
    @DisplayName("标题加操作概要自身超限时截断并带后缀，结果不超过上限")
    void truncateTitleAndSummaryWithSuffix() {
        ApprovalContentSection hugeSummary = section(SUMMARY,
            "## 操作概要\n\n| 项目 | 内容 |\n| --- | --- |\n| 账号 | " + "测".repeat(40) + " |\n\n");
        List<ApprovalContentSection> sections = List.of(section(TITLE, TITLE_MD), hugeSummary);
        int maxBytes = utf8Len(TITLE_MD) + 40;
        properties.setContentSimpleMaxBytes(maxBytes);

        String simple = simplifier.simplify(sections);

        assertThat(simple).endsWith("\n\n" + ZH_TRUNCATED);
        assertThat(utf8Len(simple)).isLessThanOrEqualTo(maxBytes);
        assertThat(simple).startsWith("# 快速执行脚本");
        assertThat(new String(simple.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(simple);
    }

    @Test
    @DisplayName("截断落在多字节汉字中间时结果仍是合法 UTF-8")
    void truncateDoesNotSplitChineseCharacter() {
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD)
        );
        byte[] titleBytes = TITLE_MD.getBytes(StandardCharsets.UTF_8);
        properties.setContentSimpleMaxBytes(titleBytes.length + 1);

        String simple = simplifier.simplify(sections);

        assertThat(utf8Len(simple)).isLessThanOrEqualTo(properties.getContentSimpleMaxBytes());
        assertThat(simple).doesNotContain("\uFFFD");
        assertThat(new String(simple.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(simple);
    }

    @Test
    @DisplayName("4 字节 emoji 截断不会切出半个码点")
    void truncateDoesNotSplitEmoji() {
        ApprovalContentSection titleWithEmoji = section(TITLE, TITLE_MD + "😀😀😀");
        List<ApprovalContentSection> sections = List.of(
            titleWithEmoji,
            section(SUMMARY, SUMMARY_MD)
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD) + 6);

        String simple = simplifier.simplify(sections);

        assertThat(utf8Len(simple)).isLessThanOrEqualTo(properties.getContentSimpleMaxBytes());
        assertThat(simple).doesNotContain("\uFFFD");
        assertThat(new String(simple.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(simple);
    }

    @Test
    @DisplayName("脚本片段内即使含其它章节标题，也按 SCRIPT 整段丢弃")
    void scriptFragmentDroppedAsWholeEvenIfMarkdownLooksLikeOtherSections() {
        ApprovalContentSection scriptWithFakeHeading = section(SCRIPT,
            "## 脚本内容\n\n```shell\necho start\n## 原始参数\n"
                + "s".repeat(200) + "\necho should_keep\n```\n\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD),
            scriptWithFakeHeading
        );
        properties.setContentSimpleMaxBytes(utf8Len(TITLE_MD + SUMMARY_MD) + 8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).isEqualTo(TITLE_MD + SUMMARY_MD);
        assertThat(simple).doesNotContain(scriptWithFakeHeading.getMarkdown());
        assertThat(simple).doesNotContain("echo should_keep");
    }

    @Test
    @DisplayName("suffix 长于上限时不加后缀，只硬截到字符边界")
    void suffixLongerThanLimitIsOmitted() {
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD)
        );
        properties.setContentSimpleMaxBytes(8);

        String simple = simplifier.simplify(sections);

        assertThat(simple).doesNotContain(ZH_TRUNCATED);
        assertThat(utf8Len(simple)).isLessThanOrEqualTo(8);
        assertThat(new String(simple.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)).isEqualTo(simple);
    }

    @Test
    @DisplayName("contentSimpleMaxBytes 为 null 或小于等于 0 时回退 20480")
    void invalidMaxBytesFallbackToDefault() {
        ApprovalContent content = contentOf(
            section(TITLE, TITLE_MD),
            section(SUMMARY, SUMMARY_MD)
        );
        properties.setContentSimpleMaxBytes(0);
        assertThat(simplifier.simplify(content)).isEqualTo(content.getApprovalContent());

        properties.setContentSimpleMaxBytes(-1);
        assertThat(simplifier.simplify(content)).isEqualTo(content.getApprovalContent());

        properties.setContentSimpleMaxBytes(null);
        assertThat(simplifier.simplify(content)).isEqualTo(content.getApprovalContent());
    }

    @Test
    @DisplayName("截断后缀跟随 Locale，章节身份不依赖标题文案")
    void truncationSuffixFollowsLocale() {
        when(i18nService.getI18n(ApprovalContentSimplifier.TRUNCATED_I18N_KEY)).thenReturn(EN_TRUNCATED);
        ApprovalContentSection hugeSummary = section(SUMMARY,
            "## Operation summary\n\n| Item | Content |\n| --- | --- |\n| Account | " + "x".repeat(80) + " |\n\n");
        ApprovalContentSection raw = section(RAW_PARAMS, "```json\n" + "r".repeat(80) + "\n```\n");
        List<ApprovalContentSection> sections = List.of(
            section(TITLE, "# Fast execute script\n\n"),
            hugeSummary,
            raw
        );
        int maxBytes = utf8Len("# Fast execute script\n\n") + 30;
        properties.setContentSimpleMaxBytes(maxBytes);

        String simple = simplifier.simplify(sections);

        assertThat(simple).doesNotContain(raw.getMarkdown());
        assertThat(simple).endsWith("\n\n" + EN_TRUNCATED);
        assertThat(utf8Len(simple)).isLessThanOrEqualTo(maxBytes);
    }

    @Test
    @DisplayName("静态 UTF-8 截断不会切出半个汉字")
    void utf8TruncateStopsAtCharacterBoundary() {
        String text = "测";
        assertThat(ApprovalContentSimplifier.utf8Len(text)).isEqualTo(3);
        assertThat(ApprovalContentSimplifier.truncateToUtf8Bytes(text, 3)).isEqualTo("测");
        assertThat(ApprovalContentSimplifier.truncateToUtf8Bytes(text, 2)).isEmpty();
        assertThat(ApprovalContentSimplifier.truncateToUtf8Bytes(text, 1)).isEmpty();
        assertThat(ApprovalContentSimplifier.truncateToUtf8Bytes("测A", 3)).isEqualTo("测");
        assertThat(ApprovalContentSimplifier.truncateToUtf8Bytes("测A", 4)).isEqualTo("测A");
    }

    private static ApprovalContent contentOf(ApprovalContentSection... sections) {
        ApprovalContent content = new ApprovalContent();
        content.setSections(List.of(sections));
        content.setApprovalContent(ApprovalContent.joinMarkdown(content.getSections()));
        return content;
    }

    private static ApprovalContentSection section(ApprovalContentSectionKind kind, String markdown) {
        return new ApprovalContentSection(kind, markdown);
    }

    private static int utf8Len(String text) {
        return ApprovalContentSimplifier.utf8Len(text);
    }
}
