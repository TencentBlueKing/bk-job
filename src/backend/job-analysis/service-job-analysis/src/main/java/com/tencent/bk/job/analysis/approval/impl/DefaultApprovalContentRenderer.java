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
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayMasker;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * 默认实现：把审批任务渲染成一份 Markdown，依次为标题、操作概要表格、逐行展示的字段列表、
 * 脚本内容代码块、原始参数 JSON 代码块。
 * <p>
 * <b>只用最基础的 Markdown 语法</b>：审批渠道的渲染器不允许内联 HTML，凡是靠 {@code <br>} 之类标签
 * 排版的写法都会被原样展示给审批人。
 * <p>
 * <b>逐步骤的解析结果不渲染</b>：单据铺得越长，审批人越容易一路划到底直接点通过。步骤解析结果
 * 仍完整记录在 approval_task.resolved_summary 里备查，正文只从中汇总出"以什么身份上机""文件从哪来、
 * 落到哪、怎么落盘"这类审批人必须看到的关键信息，且逐行都按条数封顶。
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

    /**
     * 文件分发相关的字段名，由下游在步骤解析结果里回带
     */
    private static final String FIELD_TRANSFER_MODE = "transfer_mode";
    private static final String FIELD_FILE_SOURCE_LIST = "file_source_list";
    private static final String FIELD_FILE_TARGET_PATH = "file_target_path";
    private static final String FIELD_FILE_TARGET_NAME = "file_target_name";

    /**
     * 全局变量章节在概要表格里对应的引导行字段名
     */
    private static final String FIELD_GLOBAL_VARS = "global_vars";

    /**
     * 主机类变量的类型枚举名，取值形态与其余类型不同（走主机清单而不是 value）
     */
    private static final String VAR_TYPE_EXECUTE_OBJECT_LIST = "EXECUTE_OBJECT_LIST";

    /**
     * 密文类变量的类型枚举名。这类变量的取值不进概要，渲染成占位符
     */
    private static final String VAR_TYPE_CIPHER = "CIPHER";

    /**
     * 值由下游按行拼好、需要逐行展示的字段。
     * <p>
     * <b>这类字段不能塞进表格单元格</b>：审批渠道的 Markdown 渲染器不允许内联 HTML，{@code <br>} 会
     * 原样吐出来；而单元格里放真实换行会把表格结构从该行起切断。因此表格里只报条数，明细另起章节用列表逐行列出
     */
    private static final Set<String> MULTI_LINE_FIELDS = buildMultiLineFields();

    /**
     * 值是枚举名、需要翻译成人话的操作级字段：字段名 -> 取值文案的 i18n key 前缀。
     * <p>
     * <b>刻意做成白名单而不是"所有字段都试着翻译一下"</b>：概要里的字段值大多是自由值（定时规则、
     * 执行方案 ID、步骤名称），隐式匹配会让日后新增一条 value.xxx 就静默改掉某个字段的展示行为
     */
    private static final Map<String, String> ENUM_VALUE_I18N_PREFIXES = buildEnumValueI18nPrefixes();

    /**
     * 一个概要行最多展示的条目数，超出只补一句总数。
     * <p>
     * 取 5 是因为概要行是表格里的一个单元格：文件分发常见的就是一两个来源、一个目标路径，5 条足以覆盖，
     * 而执行方案可能有十几个文件步骤，全列出来会把单元格撑成一堵墙，等于把当初移除的步骤明细又搬了回来。
     * 需要逐条核对的审批人可以看原始参数，或去作业平台查这次预检的步骤快照
     */
    private static final int MAX_ROW_ITEM_COUNT = 5;

    private final MessageI18nService i18nService;
    private final CommonAppService appService;
    private final ApprovalParamsCryptoService paramsCryptoService;
    private final ApprovalDisplayMasker displayMasker;

    private static Map<String, String> buildEnumValueI18nPrefixes() {
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("target_status", "value.cronStatus.");
        prefixes.put("operation", "value.operation.");
        return Collections.unmodifiableMap(prefixes);
    }

    private static Set<String> buildMultiLineFields() {
        Set<String> fields = new LinkedHashSet<>();
        fields.add("enable_steps");
        fields.add("enable_steps_all");
        return Collections.unmodifiableSet(fields);
    }

    public DefaultApprovalContentRenderer(MessageI18nService i18nService,
                                          CommonAppService appService,
                                          ApprovalParamsCryptoService paramsCryptoService,
                                          ApprovalDisplayMasker displayMasker) {
        this.i18nService = i18nService;
        this.appService = appService;
        this.paramsCryptoService = paramsCryptoService;
        this.displayMasker = displayMasker;
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
        appendMultiLineFields(content, summary);
        appendGlobalVars(content, summary);

        // 脚本正文与原始参数出自同一份脱敏后的参数：脚本被摘出去单独展示，参数里只留占位符
        List<PlainTextBlock> scriptBlocks = new ArrayList<>();
        String rawParamsJson = renderRawParams(task, operationType, scriptBlocks);
        appendScripts(content, scriptBlocks);
        appendRawParams(content, rawParamsJson);
        return content.toString();
    }

    /**
     * 操作概要：默认最先看到的东西，用表格逐行摊开解析后的实际影响面。
     * <p>
     * <b>带默认前缀的行统一沉底</b>：夹在普通字段行中间时表格观感杂乱，而沉底聚在一起的恰好都是
     * "用户没选、系统替他选了"的项，反倒更醒目。分发模式的默认标记是行内前缀而不是独立行，
     * 因此它按默认生效时也随之沉底、显式指定时留在原位，但无论哪种都只出一行
     */
    private void appendSummary(StringBuilder content,
                               ApprovalTaskDTO task,
                               ApprovalOperationTypeEnum operationType,
                               BasicApp app,
                               ResolvedSummary summary,
                               ApprovalRiskLevelEnum riskLevel) {
        List<TableRow> rows = new ArrayList<>();
        List<TableRow> defaultRows = new ArrayList<>();
        putRow(rows, label("operationType"), operationName(operationType), false);
        putRow(rows, label("scope"), describeScope(app, task.getAppId()), false);
        putRow(rows, label("creator"), task.getCreator(), false);
        putRow(rows, label("riskLevel"), riskLevelName(riskLevel), riskLevel == ApprovalRiskLevelEnum.HIGH);
        if (StringUtils.isNotBlank(summary.getName())) {
            putRow(rows, label("name"), summary.getName(), false);
        }
        putRow(rows, label("accounts"), describeAccounts(summary), false);
        // 同一份脚本，参数决定了这次到底干什么：是灰度还是全量、清的是哪个目录
        putRow(rows, label("scriptParam"), describeScriptParams(summary), false);
        // 文件从哪来、落到哪、怎么落盘：执行方案的文件步骤定义在方案里而不在入参里，
        // 不在这里汇总出来，审批人在正文中就再也看不到这三件事
        putFileSourceRow(rows, summary);
        putFileFieldRow(rows, summary, FIELD_FILE_TARGET_PATH);
        putFileFieldRow(rows, summary, FIELD_FILE_TARGET_NAME);
        putTransferModeRow(rows, defaultRows, summary);
        putResolvedFields(rows, summary.getFields(), StringUtils.EMPTY);
        if (CollectionUtils.isNotEmpty(summary.getGlobalVars())) {
            putRow(rows, resolvedFieldLabel(FIELD_GLOBAL_VARS),
                labelWithArgs("value.itemCount", summary.getGlobalVars().size()), false);
        }
        putRow(rows, label("executeObjects"), describeExecuteObjects(summary), false);
        if (Boolean.TRUE.equals(summary.getDangerousRuleMatched())) {
            putRow(rows, label("dangerousRuleMatched"), label("value.dangerousRuleMatched"), true);
        }
        // 动态目标的实际影响面在放行时才确定，这是已知限制，必须如实披露给审批人
        if (Boolean.TRUE.equals(summary.getContainsDynamicTarget())) {
            putRow(rows, label("containsDynamicTarget"), label("value.dynamicTargetHint"), true);
        }
        if (CollectionUtils.isNotEmpty(summary.getDefaultsApplied())) {
            // 默认生效的参数后果与显式指定一样，必须逐项披露；分发模式已单独成行，此处不再重复
            putResolvedFields(defaultRows, defaultsAppliedExceptTransferMode(summary), label("defaultPrefix"));
        }
        rows.addAll(defaultRows);

        appendHeading(content, 2, label("section.summary"));
        appendTable(content, Arrays.asList(label("table.item"), label("table.value")), rows);
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
            appendCodeBlock(content, StringUtils.defaultString(block.getLanguage()), block.getValue());
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
     * 风险等级只是给审批人的提示强度，不参与放行校验。
     * <p>
     * 主机类全局变量的台数一并计入：创建执行方案、保存定时任务没有执行对象总数，影响面全在变量里，
     * 不计入的话一个指向上千台机器的方案会被判成低风险。<b>与执行对象总数取较大值而不是相加</b>：
     * 启动执行方案时主机变量已被解析进步骤目标、计入了执行对象总数，相加会把同一批机器算两次
     */
    private ApprovalRiskLevelEnum resolveRiskLevel(ResolvedSummary summary) {
        int executeObjectCount =
            summary.getTotalExecuteObjectCount() == null ? 0 : summary.getTotalExecuteObjectCount();
        int count = Math.max(executeObjectCount, summary.totalGlobalVarHostCount());
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

    /**
     * 执行对象：数量在上限内时逐个列出，超出只报总数。
     * <p>
     * "将影响哪几台机器"是审批人判断影响面的第一依据，只给一个数字等于让人凭空想象；
     * 但上千台逐个列出只会把单据铺成一堵墙，反而让人直接划到底放行，此时总数本身已足够。
     * <b>逐个列出的前提是清单完整</b>：概要里的执行对象清单本身按
     * {@link ResolvedSummary#MAX_EXECUTE_OBJECT_COUNT} 截断过，与总数对不上时只报总数，
     * 免得把截断后的一部分说成全部
     */
    private String describeExecuteObjects(ResolvedSummary summary) {
        Integer totalCount = summary.getTotalExecuteObjectCount();
        if (totalCount == null) {
            return null;
        }
        List<String> displays = collectExecuteObjectDisplays(summary);
        if (totalCount <= ResolvedSummary.MAX_DISPLAY_ITEM_COUNT && displays.size() == totalCount) {
            return labelWithArgs("value.executeObjectList", totalCount,
                String.join(ResolvedSummary.ITEM_SEPARATOR, displays));
        }
        return labelWithArgs("value.executeObjectCount", totalCount);
    }

    /**
     * 各步骤的执行对象去重后合并，与总数的口径保持一致（总数也是跨步骤去重后的）
     */
    private List<String> collectExecuteObjectDisplays(ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return Collections.emptyList();
        }
        Set<String> displays = new LinkedHashSet<>();
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            if (CollectionUtils.isEmpty(step.getExecuteObjects())) {
                continue;
            }
            for (ResolvedSummary.ResolvedExecuteObject executeObject : step.getExecuteObjects()) {
                if (StringUtils.isNotBlank(executeObject.getDisplay())) {
                    displays.add(executeObject.getDisplay());
                }
            }
        }
        return new ArrayList<>(displays);
    }

    /**
     * 脚本参数：同一份脚本，参数不同做的就是完全不同的事，因此与脚本内容一样是必须看到的。
     * <p>
     * <b>调用方声明为敏感的参数只出占位符</b>：它压根没进概要，这里连"取值为空"都不能说成没参数，
     * 否则审批人会以为这次执行不带参数
     */
    private String describeScriptParams(ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return null;
        }
        Set<String> params = new LinkedHashSet<>();
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            if (Boolean.TRUE.equals(step.getParamSensitive())) {
                params.add(labelWithArgs("value.scriptParamSensitive", displayMasker.mask()));
            } else if (StringUtils.isNotBlank(step.getScriptParam())) {
                params.add(step.getScriptParam());
            }
        }
        return joinWithLimit(new ArrayList<>(params));
    }

    /**
     * 源文件：以什么身份、从哪台机器、取哪些文件。
     * <p>
     * 结构化的源文件由此处拼文案，<b>老单据的快照里源文件是下游拼好的一个字符串</b>，
     * 按老方式原样展示，不能因为换了结构就让历史单据的这一行整个消失
     */
    private void putFileSourceRow(List<TableRow> rows, ResolvedSummary summary) {
        List<String> items = collectFileSourceItems(summary);
        if (items.isEmpty()) {
            putFileFieldRow(rows, summary, FIELD_FILE_SOURCE_LIST);
            return;
        }
        putRow(rows, resolvedFieldLabel(FIELD_FILE_SOURCE_LIST), joinWithLimit(items), false);
    }

    private List<String> collectFileSourceItems(ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return Collections.emptyList();
        }
        Set<String> items = new LinkedHashSet<>();
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            if (CollectionUtils.isEmpty(step.getFileSources())) {
                continue;
            }
            for (ResolvedSummary.ResolvedFileSource fileSource : step.getFileSources()) {
                String item = describeFileSource(fileSource);
                if (StringUtils.isNotBlank(item)) {
                    items.add(item);
                }
            }
        }
        return new ArrayList<>(items);
    }

    /**
     * 一个源文件形如 {@code root@0:127.0.0.9: /data/a.tar.gz}；
     * 本地文件没有源机器与源账号，标成本地文件，否则会被当成漏填
     */
    private String describeFileSource(ResolvedSummary.ResolvedFileSource fileSource) {
        String filePaths = describeFileSourcePaths(fileSource);
        String origin = describeFileSourceOrigin(fileSource);
        if (StringUtils.isBlank(origin)) {
            return filePaths;
        }
        if (StringUtils.isBlank(filePaths)) {
            return origin;
        }
        // 本地文件的前缀是个标记而不是取文件的身份，用冒号连起来反倒像是"以本地文件这个账号去取"
        return Boolean.TRUE.equals(fileSource.getLocalUpload())
            ? origin + " " + filePaths : origin + ": " + filePaths;
    }

    private String describeFileSourcePaths(ResolvedSummary.ResolvedFileSource fileSource) {
        if (CollectionUtils.isEmpty(fileSource.getFilePaths())) {
            return null;
        }
        String filePaths = String.join(",", fileSource.getFilePaths());
        Integer totalCount = fileSource.getFilePathCount();
        if (totalCount != null && totalCount > fileSource.getFilePaths().size()) {
            filePaths = filePaths + "," + labelWithArgs("value.itemTruncated", totalCount);
        }
        return filePaths;
    }

    /**
     * 取文件的身份与来源机器。机器台数在上限内时逐台列出，超出只报台数，与执行对象、主机变量一致
     */
    private String describeFileSourceOrigin(ResolvedSummary.ResolvedFileSource fileSource) {
        if (Boolean.TRUE.equals(fileSource.getLocalUpload())) {
            return label("value.localFile");
        }
        String hosts = null;
        if (CollectionUtils.isNotEmpty(fileSource.getHosts())) {
            hosts = String.join(",", fileSource.getHosts());
        } else if (fileSource.getHostCount() != null && fileSource.getHostCount() > 0) {
            hosts = labelWithArgs("value.fileSourceHostCount", fileSource.getHostCount());
        }
        String accountAlias = fileSource.getAccountAlias();
        if (StringUtils.isBlank(accountAlias)) {
            return hosts;
        }
        return hosts == null ? accountAlias : accountAlias + "@" + hosts;
    }

    /**
     * 源文件清单、目标路径、目标文件名同样从各步骤汇总成一行：这几项决定了"文件从哪来、会覆盖哪个目录"，
     * 是审批人判断影响面的直接依据。<b>快速分发文件还能从原始参数里翻到，执行方案的文件步骤定义在方案里、
     * 入参只有 plan_id，不汇总就等于完全不可见。</b>
     * <p>
     * 值为空的步骤字段（如通常不填的目标文件名）由 {@link #putRow} 自动跳过，不会留下空行
     */
    private void putFileFieldRow(List<TableRow> rows, ResolvedSummary summary, String fieldLabel) {
        putRow(rows, resolvedFieldLabel(fieldLabel), joinWithLimit(collectStepFieldItems(summary, fieldLabel)), false);
    }

    /**
     * 各步骤同名字段的内容去重后按条汇总。源文件清单这类字段本身就是下游按
     * {@link ResolvedSummary#ITEM_SEPARATOR} 拼好的多条，须拆开才能按条截断
     */
    private List<String> collectStepFieldItems(ResolvedSummary summary, String fieldLabel) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return Collections.emptyList();
        }
        Set<String> items = new LinkedHashSet<>();
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            ResolvedSummary.ResolvedField field = findField(step.getFields(), fieldLabel);
            if (field == null || StringUtils.isBlank(field.getValue())) {
                continue;
            }
            for (String item : StringUtils.split(field.getValue(), ResolvedSummary.ITEM_SEPARATOR.trim())) {
                if (StringUtils.isNotBlank(item)) {
                    items.add(item.trim());
                }
            }
        }
        return new ArrayList<>(items);
    }

    /**
     * 超过上限的条目不再逐条列出，只补一句总数：单据铺得越长，审批人越容易一路划到底直接点通过
     */
    private String joinWithLimit(List<String> items) {
        if (items.isEmpty()) {
            return null;
        }
        if (items.size() <= MAX_ROW_ITEM_COUNT) {
            return String.join(ResolvedSummary.ITEM_SEPARATOR, items);
        }
        return String.join(ResolvedSummary.ITEM_SEPARATOR, items.subList(0, MAX_ROW_ITEM_COUNT))
            + ResolvedSummary.ITEM_SEPARATOR + labelWithArgs("value.itemTruncated", items.size());
    }

    /**
     * 分发模式同样从各步骤汇总成一行：同名文件是被覆盖还是分目录存放、目标路径不存在时是直接失败还是
     * 自动建目录，后果差别很大，与"以什么身份上机"一样是审批人必须看到的，不能因为步骤明细不展示就丢失。
     * <p>
     * 未显式指定而按默认生效时给标签加上默认前缀：默认落到的强制模式破坏性最大，不能让审批人以为用户选过。
     * 此时该行与其余默认值提示一同沉底，落在 {@code defaultRows} 而不是 {@code rows}
     */
    private void putTransferModeRow(List<TableRow> rows, List<TableRow> defaultRows, ResolvedSummary summary) {
        ResolvedSummary.ResolvedField defaultApplied =
            findField(summary.getDefaultsApplied(), FIELD_TRANSFER_MODE);
        String transferModes = describeTransferModes(summary);
        if (StringUtils.isBlank(transferModes) && defaultApplied != null) {
            // 步骤解析结果里没带出模式时，至少把默认生效的那个亮出来，不能整行消失
            transferModes = transferModeName(defaultApplied.getValue());
        }
        String fieldLabel = resolvedFieldLabel(FIELD_TRANSFER_MODE);
        if (defaultApplied == null) {
            putRow(rows, fieldLabel, transferModes, false);
            return;
        }
        putRow(defaultRows, prefixed(label("defaultPrefix"), fieldLabel), transferModes, false);
    }

    /**
     * 各步骤解析出的分发模式去重后合并，多个文件分发步骤用了不同模式时逐一列出
     */
    private String describeTransferModes(ResolvedSummary summary) {
        return joinWithLimit(collectStepFieldItems(summary, FIELD_TRANSFER_MODE).stream()
            .map(this::transferModeName)
            .distinct()
            .collect(Collectors.toList()));
    }

    /**
     * 模式枚举名对审批人来说是天书，翻译成说清后果的文案；缺文案时原样展示
     */
    private String transferModeName(String transferMode) {
        if (StringUtils.isBlank(transferMode)) {
            return null;
        }
        String translated = tryGetI18n(I18N_PREFIX + "value.transferMode." + transferMode);
        return translated == null ? transferMode : translated;
    }

    private List<ResolvedSummary.ResolvedField> defaultsAppliedExceptTransferMode(ResolvedSummary summary) {
        return summary.getDefaultsApplied().stream()
            .filter(field -> !FIELD_TRANSFER_MODE.equals(field.getLabel()))
            .collect(Collectors.toList());
    }

    private ResolvedSummary.ResolvedField findField(List<ResolvedSummary.ResolvedField> fields, String label) {
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }
        return fields.stream()
            .filter(field -> label.equals(field.getLabel()))
            .findFirst()
            .orElse(null);
    }

    private void putResolvedFields(List<TableRow> rows,
                                   List<ResolvedSummary.ResolvedField> fields,
                                   String labelPrefix) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (ResolvedSummary.ResolvedField field : fields) {
            String fieldLabel = prefixed(labelPrefix, resolvedFieldLabel(field.getLabel()));
            putRow(rows, fieldLabel, summarizeFieldValue(field), field.isHighlight());
        }
    }

    /**
     * 逐行展示的字段在表格里只报条数，明细由 {@link #appendMultiLineFields} 另起章节列出
     */
    private String summarizeFieldValue(ResolvedSummary.ResolvedField field) {
        if (MULTI_LINE_FIELDS.contains(field.getLabel())) {
            return labelWithArgs("value.itemCount", splitLines(field.getValue()).size());
        }
        return resolvedFieldValue(field.getLabel(), field.getValue());
    }

    /**
     * 逐行展示的字段各自成章节，用无序列表一行一条。
     * <p>
     * 走列表而不是表格单元格，是因为审批渠道的 Markdown 渲染器不允许内联 HTML：{@code <br>} 会被原样展示，
     * 而单元格里放真实换行会切断表格。<b>此处不做条数截断</b>：条数上限是人工编排出来的步骤数，
     * 截掉几行恰好截掉的是本章节唯一要说明的事
     */
    private void appendMultiLineFields(StringBuilder content, ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getFields())) {
            return;
        }
        for (ResolvedSummary.ResolvedField field : summary.getFields()) {
            if (!MULTI_LINE_FIELDS.contains(field.getLabel())) {
                continue;
            }
            List<String> items = splitLines(field.getValue());
            if (items.isEmpty()) {
                continue;
            }
            appendHeading(content, 2, resolvedFieldLabel(field.getLabel()));
            appendList(content, items);
        }
    }

    /**
     * 全局变量章节：一行一个变量，列出变量名、类型与生效取值。
     * <p>
     * 变量是"这次操作到底拿什么参数去跑"的直接答案，因此<b>全部变量都要列出</b>，包括本次没指定、
     * 沿用现有配置的那些——沿用来的取值一样会被执行，只列本次改动的等于让审批人蒙着眼放行。
     * 沿用现值的行加前缀并统一沉底，与概要表格里默认值行的处理一致。
     * <p>
     * 主机类变量的取值可能是上千台机器，超过 {@link ResolvedSummary#MAX_DISPLAY_ITEM_COUNT} 台时
     * 下游只回带台数，此处相应只报总数
     */
    private void appendGlobalVars(StringBuilder content, ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getGlobalVars())) {
            return;
        }
        List<TableRow> rows = new ArrayList<>();
        List<TableRow> notAssignedRows = new ArrayList<>();
        for (ResolvedSummary.ResolvedGlobalVar globalVar : summary.getGlobalVars()) {
            boolean assigned = !Boolean.FALSE.equals(globalVar.getAssigned());
            String name = escapeTableCell(StringUtils.defaultString(globalVar.getName()));
            (assigned ? rows : notAssignedRows).add(new TableRow(
                assigned ? name : prefixed(label("value.varNotAssignedPrefix"), name),
                varTypeName(globalVar.getType()),
                escapeTableCell(describeGlobalVarValue(globalVar))
            ));
        }
        rows.addAll(notAssignedRows);

        appendHeading(content, 2, label("section.globalVars"));
        appendTable(content,
            Arrays.asList(label("table.varName"), label("table.varType"), label("table.varValue")), rows);
    }

    /**
     * 变量取值的展示：主机类变量按台数/清单描述，密文类只出占位符，其余原样展示
     */
    private String describeGlobalVarValue(ResolvedSummary.ResolvedGlobalVar globalVar) {
        if (VAR_TYPE_CIPHER.equals(globalVar.getType())) {
            return displayMasker.mask();
        }
        if (VAR_TYPE_EXECUTE_OBJECT_LIST.equals(globalVar.getType())) {
            return describeGlobalVarTarget(globalVar);
        }
        return StringUtils.isBlank(globalVar.getValue()) ? label("value.varNoValue") : globalVar.getValue();
    }

    /**
     * 主机类变量的取值：台数在上限内时连同主机清单一起给出，超出只报台数；
     * 动态分组与拓扑节点算不出台数，只报个数，实际机器在放行时才解析（概要表格里另有一行动态目标提示）
     */
    private String describeGlobalVarTarget(ResolvedSummary.ResolvedGlobalVar globalVar) {
        List<String> parts = new ArrayList<>();
        Integer hostCount = globalVar.getHostCount();
        if (CollectionUtils.isNotEmpty(globalVar.getHosts())) {
            parts.add(labelWithArgs("value.varHostList", globalVar.getHosts().size(),
                String.join(ResolvedSummary.ITEM_SEPARATOR, globalVar.getHosts())));
        } else if (hostCount != null && hostCount > 0) {
            parts.add(labelWithArgs("value.varHostCount", hostCount));
        }
        if (globalVar.getDynamicGroupCount() != null && globalVar.getDynamicGroupCount() > 0) {
            parts.add(labelWithArgs("value.varDynamicGroupCount", globalVar.getDynamicGroupCount()));
        }
        if (globalVar.getTopoNodeCount() != null && globalVar.getTopoNodeCount() > 0) {
            parts.add(labelWithArgs("value.varTopoNodeCount", globalVar.getTopoNodeCount()));
        }
        if (globalVar.getContainerCount() != null && globalVar.getContainerCount() > 0) {
            parts.add(labelWithArgs("value.varContainerCount", globalVar.getContainerCount()));
        }
        return parts.isEmpty() ? label("value.varNoValue") : String.join(ResolvedSummary.ITEM_SEPARATOR, parts);
    }

    /**
     * 变量类型的枚举名对审批人是天书，翻译成人话；缺文案时原样展示，与 {@link #transferModeName} 同构
     */
    private String varTypeName(String varType) {
        if (StringUtils.isBlank(varType)) {
            return StringUtils.EMPTY;
        }
        String translated = tryGetI18n(I18N_PREFIX + "value.varType." + varType);
        return translated == null ? varType : translated;
    }

    private List<String> splitLines(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("\\R"))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    private void appendList(StringBuilder content, List<String> items) {
        for (String item : items) {
            content.append("- ").append(item).append(LINE_SEPARATOR);
        }
        content.append(LINE_SEPARATOR);
    }

    /**
     * 白名单里的字段值是枚举名，对审批人来说与天书无异（"启的还是停的"要靠猜），翻译成人话；
     * 缺文案时原样展示，与 {@link #transferModeName} 同构
     */
    private String resolvedFieldValue(String fieldName, String value) {
        String keyPrefix = ENUM_VALUE_I18N_PREFIXES.get(fieldName);
        if (keyPrefix == null || StringUtils.isBlank(value)) {
            return value;
        }
        String translated = tryGetI18n(I18N_PREFIX + keyPrefix + value);
        return translated == null ? value : translated;
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

    private void appendTable(StringBuilder content, List<String> headers, List<TableRow> rows) {
        appendTableLine(content, headers);
        List<String> separators = new ArrayList<>(headers.size());
        for (int i = 0; i < headers.size(); i++) {
            separators.add("---");
        }
        appendTableLine(content, separators);
        for (TableRow row : rows) {
            appendTableLine(content, row.cells);
        }
        content.append(LINE_SEPARATOR);
    }

    private void appendTableLine(StringBuilder content, List<String> cells) {
        content.append("|");
        for (String cell : cells) {
            content.append(' ').append(StringUtils.defaultString(cell)).append(" |");
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
     * 表格单元格里的竖线会切断列，换行会把一行截成两行、整张表从该行起散架：前者转义，后者压成空格。
     * <p>
     * <b>不能换成 {@code <br>}</b>：审批渠道的 Markdown 渲染器不允许内联 HTML，标签会原样展示给审批人。
     * 本就需要逐行展示的字段走 {@link #appendMultiLineFields} 的列表章节，不进单元格
     */
    private String escapeTableCell(String value) {
        return value.replace("|", "\\|")
            .replaceAll("\\R", " ");
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
     * 表格的一行，单元格个数与表头一致
     */
    private static class TableRow {

        private final List<String> cells;

        TableRow(String... cells) {
            this.cells = Arrays.asList(cells);
        }
    }
}
