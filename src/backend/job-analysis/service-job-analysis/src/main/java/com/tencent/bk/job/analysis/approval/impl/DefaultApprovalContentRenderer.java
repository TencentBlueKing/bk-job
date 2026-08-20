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

import com.fasterxml.jackson.databind.JsonNode;
import com.tencent.bk.job.analysis.approval.ApprovalContentRenderer;
import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalContent;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams.PlainTextBlock;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalRiskLevelEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.model.ResolvedSummary;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 默认实现：把审批任务渲染成一份 Markdown，依次为标题、操作概要表格、脚本内容代码块、
 * 原始参数 JSON 代码块。
 * <p>
 * <b>逐步骤的解析结果不渲染</b>：单据铺得越长，审批人越容易一路划到底直接点通过。步骤解析结果
 * 仍完整记录在 approval_task.resolved_summary 里备查，正文只从中汇总出"以什么身份上机"这一行。
 * <p>
 * <b>敏感字段的脱敏由 {@link ApprovalParamsCryptoService} 完成</b>，本类拿到的已是脱敏后的参数，
 * 不自行判断哪个字段敏感。
 */
@Slf4j
@Service
public class DefaultApprovalContentRenderer implements ApprovalContentRenderer {

    /**
     * 原始参数 JSON 最多展示的字符数，超出截断：参数体可能很大（如上千台主机的 host_id 列表）
     */
    private static final int MAX_RAW_PARAMS_LENGTH = 20000;

    private static final String I18N_PREFIX = "task.approval.content.";

    private static final String LINE_SEPARATOR = "\n";

    private final MessageI18nService i18nService;
    private final CommonAppService appService;
    private final ApprovalParamsCryptoService paramsCryptoService;

    public DefaultApprovalContentRenderer(MessageI18nService i18nService,
                                          CommonAppService appService,
                                          ApprovalParamsCryptoService paramsCryptoService) {
        this.i18nService = i18nService;
        this.appService = appService;
        this.paramsCryptoService = paramsCryptoService;
    }

    @Override
    public ApprovalContent render(ApprovalTaskDTO task) {
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        ResolvedSummary summary = parseSummary(task);
        BasicApp app = loadApp(task.getAppId());
        ApprovalRiskLevelEnum riskLevel = resolveRiskLevel(summary);
        String title = buildTitle(operationType, app, summary);

        ApprovalContent content = new ApprovalContent();
        content.setApprovalTaskId(task.getApprovalTaskId());
        content.setExpireAt(task.getExpireAt());
        content.setApprovalContent(buildContent(task, operationType, app, summary, riskLevel, title));
        return content;
    }

    private String buildContent(ApprovalTaskDTO task,
                                ApprovalOperationTypeEnum operationType,
                                BasicApp app,
                                ResolvedSummary summary,
                                ApprovalRiskLevelEnum riskLevel,
                                String title) {
        StringBuilder content = new StringBuilder();
        appendHeading(content, 1, title);
        appendSummary(content, task, operationType, app, summary, riskLevel);

        // 脚本正文与原始参数出自同一份脱敏后的参数：脚本被摘出去单独展示，参数里只留占位符
        List<PlainTextBlock> scriptBlocks = new ArrayList<>();
        String rawParamsJson = renderRawParams(task, operationType, scriptBlocks);
        appendScripts(content, scriptBlocks);
        appendRawParams(content, rawParamsJson);
        return content.toString();
    }

    /**
     * 操作概要：默认最先看到的东西，用表格逐行摊开解析后的实际影响面
     */
    private void appendSummary(StringBuilder content,
                               ApprovalTaskDTO task,
                               ApprovalOperationTypeEnum operationType,
                               BasicApp app,
                               ResolvedSummary summary,
                               ApprovalRiskLevelEnum riskLevel) {
        List<TableRow> rows = new ArrayList<>();
        putRow(rows, label("operationType"), operationName(operationType), false);
        putRow(rows, label("scope"), describeScope(app, task.getAppId()), false);
        putRow(rows, label("creator"), task.getCreator(), false);
        putRow(rows, label("riskLevel"), riskLevelName(riskLevel), riskLevel == ApprovalRiskLevelEnum.HIGH);
        if (StringUtils.isNotBlank(summary.getName())) {
            putRow(rows, label("name"), summary.getName(), false);
        }
        putRow(rows, label("accounts"), describeAccounts(summary), false);
        putResolvedFields(rows, summary.getFields(), StringUtils.EMPTY);
        if (summary.getTotalExecuteObjectCount() != null) {
            putRow(rows, label("totalExecuteObjectCount"),
                String.valueOf(summary.getTotalExecuteObjectCount()), false);
        }
        if (Boolean.TRUE.equals(summary.getDangerousRuleMatched())) {
            putRow(rows, label("dangerousRuleMatched"), label("value.dangerousRuleMatched"), true);
        }
        // 动态目标的实际影响面在放行时才确定，这是已知限制，必须如实披露给审批人
        if (Boolean.TRUE.equals(summary.getContainsDynamicTarget())) {
            putRow(rows, label("containsDynamicTarget"), label("value.dynamicTargetHint"), true);
        }
        if (CollectionUtils.isNotEmpty(summary.getDefaultsApplied())) {
            // 默认生效的参数后果与显式指定一样，必须逐项披露
            putResolvedFields(rows, summary.getDefaultsApplied(), label("defaultPrefix"));
        }

        appendHeading(content, 2, label("section.summary"));
        appendTable(content, rows);
    }

    /**
     * 脚本内容：用代码块承载，换行与缩进都保持原样
     */
    private void appendScripts(StringBuilder content, List<PlainTextBlock> scriptBlocks) {
        if (CollectionUtils.isEmpty(scriptBlocks)) {
            return;
        }
        appendHeading(content, 2, label("section.scriptContent"));
        for (PlainTextBlock block : scriptBlocks) {
            content.append('`').append(block.getField()).append('`').append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            appendCodeBlock(content, StringUtils.EMPTY, block.getValue());
        }
    }

    private void appendRawParams(StringBuilder content, String rawParamsJson) {
        if (rawParamsJson == null) {
            appendHeading(content, 2, label("section.rawParams"));
            content.append(label("value.rawParamsRenderFail")).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
            return;
        }
        if (rawParamsJson.isEmpty()) {
            // 没有参数快照时连标题都不出，避免出现空章节
            return;
        }
        appendHeading(content, 2, label("section.rawParams"));
        appendCodeBlock(content, "json", rawParamsJson);
    }

    /**
     * 把参数快照渲染成脱敏后的 JSON 文本，同时把需要原样展示的明文段（脚本内容）摘出到
     * {@code scriptBlocks}，参数里只留一个指向脚本章节的占位符。
     *
     * @return 脱敏后的 JSON 文本；无参数快照时为空串，渲染失败时为 null
     */
    private String renderRawParams(ApprovalTaskDTO task,
                                   ApprovalOperationTypeEnum operationType,
                                   List<PlainTextBlock> scriptBlocks) {
        if (StringUtils.isBlank(task.getOperationParams())) {
            return StringUtils.EMPTY;
        }
        JsonNode root;
        try {
            ApprovalDisplayParams displayParams =
                paramsCryptoService.desensitizeFromSnapshot(operationType, task.getOperationParams());
            scriptBlocks.addAll(displayParams.getPlainTextBlocks());
            // 未传的字段一律不展示：一屏的 null 会把真正传了什么淹没掉
            root = JsonUtils.toJsonNode(JsonUtils.toNonNullJson(displayParams.getParams()));
        } catch (Exception e) {
            // 参数区渲染失败不能让整份内容出不来，概要与步骤仍然可用
            log.error("Render raw params failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
            return null;
        }
        if (root == null) {
            return null;
        }
        // 统一换行符：Jackson 的缩进输出跟随运行平台，不统一会让同一份内容在不同节点上长得不一样
        String json = root.toPrettyString().replace("\r\n", LINE_SEPARATOR);
        if (json.length() > MAX_RAW_PARAMS_LENGTH) {
            return json.substring(0, MAX_RAW_PARAMS_LENGTH) + LINE_SEPARATOR
                + labelWithArgs("value.rawParamsTruncated", MAX_RAW_PARAMS_LENGTH);
        }
        return json;
    }

    /**
     * 风险等级只是给审批人的提示强度，不参与放行校验
     */
    private ApprovalRiskLevelEnum resolveRiskLevel(ResolvedSummary summary) {
        int count = summary.getTotalExecuteObjectCount() == null ? 0 : summary.getTotalExecuteObjectCount();
        if (Boolean.TRUE.equals(summary.getDangerousRuleMatched())
            || count > ApprovalRiskLevelEnum.HIGH_RISK_EXECUTE_OBJECT_COUNT) {
            return ApprovalRiskLevelEnum.HIGH;
        }
        if (count > ApprovalRiskLevelEnum.MEDIUM_RISK_EXECUTE_OBJECT_COUNT) {
            return ApprovalRiskLevelEnum.MEDIUM;
        }
        return ApprovalRiskLevelEnum.LOW;
    }

    /**
     * 标题形如「快速执行脚本 - 某业务 - 37 个执行对象」
     */
    private String buildTitle(ApprovalOperationTypeEnum operationType, BasicApp app, ResolvedSummary summary) {
        StringJoiner joiner = new StringJoiner(" - ");
        joiner.add(operationName(operationType));
        if (app != null && StringUtils.isNotBlank(app.getName())) {
            joiner.add(app.getName());
        }
        if (summary.getTotalExecuteObjectCount() != null) {
            joiner.add(labelWithArgs("executeObjectCount", summary.getTotalExecuteObjectCount()));
        } else if (StringUtils.isNotBlank(summary.getName())) {
            joiner.add(summary.getName());
        }
        return joiner.toString();
    }

    private String describeScope(BasicApp app, Long appId) {
        if (app == null || app.getScope() == null) {
            return String.valueOf(appId);
        }
        ResourceScope scope = app.getScope();
        String scopeDesc = scope.getType().getValue() + ":" + scope.getId();
        return StringUtils.isBlank(app.getName()) ? scopeDesc : app.getName() + "(" + scopeDesc + ")";
    }

    /**
     * 各步骤用到的执行账号去重后合并成一行：步骤明细不展示，但"以什么身份上机"是审批人必须看到的
     */
    private String describeAccounts(ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return null;
        }
        Set<String> accounts = new LinkedHashSet<>();
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            if (StringUtils.isNotBlank(step.getAccountAlias())) {
                accounts.add(step.getAccountAlias());
            }
        }
        return accounts.isEmpty() ? null : String.join(", ", accounts);
    }

    private void putResolvedFields(List<TableRow> rows,
                                   List<ResolvedSummary.ResolvedField> fields,
                                   String labelPrefix) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (ResolvedSummary.ResolvedField field : fields) {
            String fieldLabel = prefixed(labelPrefix, resolvedFieldLabel(field.getLabel()));
            putRow(rows, fieldLabel, field.getValue(), field.isHighlight());
        }
    }

    /**
     * 值为空的行直接不出现；{@code highlight} 的行加粗，作为高危项的显著标注
     */
    private void putRow(List<TableRow> rows, String label, String value, boolean highlight) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        String cellValue = escapeTableCell(value);
        rows.add(new TableRow(highlight ? bold(label) : label, highlight ? bold(cellValue) : cellValue));
    }

    private void appendTable(StringBuilder content, List<TableRow> rows) {
        content.append("| ").append(label("table.item")).append(" | ").append(label("table.value"))
            .append(" |").append(LINE_SEPARATOR)
            .append("| --- | --- |").append(LINE_SEPARATOR);
        for (TableRow row : rows) {
            content.append("| ").append(row.label).append(" | ").append(row.value)
                .append(" |").append(LINE_SEPARATOR);
        }
        content.append(LINE_SEPARATOR);
    }

    private void appendHeading(StringBuilder content, int level, String text) {
        content.append(StringUtils.repeat('#', level)).append(' ').append(StringUtils.defaultString(text))
            .append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    }

    /**
     * 围栏按内容里最长的连续反引号加长，避免脚本本身含 ``` 时把代码块提前闭合、后续内容被当成正文
     */
    private void appendCodeBlock(StringBuilder content, String language, String code) {
        String fence = StringUtils.repeat('`', Math.max(3, longestBacktickRun(code) + 1));
        content.append(fence).append(language).append(LINE_SEPARATOR)
            .append(code).append(LINE_SEPARATOR)
            .append(fence).append(LINE_SEPARATOR).append(LINE_SEPARATOR);
    }

    private int longestBacktickRun(String text) {
        int longest = 0;
        int current = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '`') {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    /**
     * 表格单元格里的竖线会切断列，换行会把一行截成两行：前者转义，后者换成 {@code <br>}
     */
    private String escapeTableCell(String value) {
        return value.replace("|", "\\|")
            .replace("\r\n", "<br>")
            .replace("\n", "<br>")
            .replace("\r", "<br>");
    }

    private String bold(String text) {
        return "**" + text + "**";
    }

    /**
     * 下游回传的 label 是稳定的字段名（如 file_target_path），能翻译就翻译，缺文案时原样展示
     */
    private String resolvedFieldLabel(String rawLabel) {
        if (StringUtils.isBlank(rawLabel)) {
            return StringUtils.EMPTY;
        }
        String translated = tryGetI18n(I18N_PREFIX + "field." + rawLabel);
        return translated == null ? rawLabel : translated;
    }

    private String operationName(ApprovalOperationTypeEnum operationType) {
        if (operationType == null) {
            return StringUtils.EMPTY;
        }
        String name = tryGetI18n(operationType.getNameI18nKey());
        return name == null ? operationType.name() : name;
    }

    private String riskLevelName(ApprovalRiskLevelEnum riskLevel) {
        String name = tryGetI18n(riskLevel.getNameI18nKey());
        return name == null ? riskLevel.name() : name;
    }

    private String label(String keySuffix) {
        String value = tryGetI18n(I18N_PREFIX + keySuffix);
        return value == null ? keySuffix : value;
    }

    private String labelWithArgs(String keySuffix, Object... args) {
        try {
            return i18nService.getI18nWithArgs(I18N_PREFIX + keySuffix, args);
        } catch (Exception e) {
            log.warn("Missing i18n message for key {}", I18N_PREFIX + keySuffix);
            return keySuffix;
        }
    }

    private String tryGetI18n(String key) {
        try {
            return i18nService.getI18n(key);
        } catch (Exception e) {
            log.warn("Missing i18n message for key {}", key);
            return null;
        }
    }

    private BasicApp loadApp(Long appId) {
        if (appId == null) {
            return null;
        }
        try {
            return appService.getApp(appId);
        } catch (Exception e) {
            // 业务信息查不到只影响标题，不该让整份内容出不来
            log.warn("Get app failed while rendering approval content, appId: {}", appId, e);
            return null;
        }
    }

    private ResolvedSummary parseSummary(ApprovalTaskDTO task) {
        if (StringUtils.isBlank(task.getResolvedSummary())) {
            return new ResolvedSummary();
        }
        try {
            ResolvedSummary summary = JsonUtils.fromJson(task.getResolvedSummary(), ResolvedSummary.class);
            return summary == null ? new ResolvedSummary() : summary;
        } catch (Exception e) {
            log.error("Parse resolved summary failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
            return new ResolvedSummary();
        }
    }

    private String prefixed(String prefix, String label) {
        return StringUtils.isEmpty(prefix) ? label : prefix + " " + label;
    }

    /**
     * 概要表格的一行
     */
    private static class TableRow {

        private final String label;

        private final String value;

        TableRow(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}
