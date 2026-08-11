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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tencent.bk.job.analysis.approval.ApprovalParamsCryptoService;
import com.tencent.bk.job.analysis.approval.ApprovalSensitiveFields;
import com.tencent.bk.job.analysis.approval.ApprovalSensitiveFields.SensitiveField;
import com.tencent.bk.job.analysis.approval.ApprovalTicketRenderer;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalTicket;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalRiskLevelEnum;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.api.model.ResolvedSummary;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 默认单据渲染实现。
 * <p>
 * <b>概要区（默认展开）承担"让审批人看清要在哪些机器上执行什么"的全部责任</b>：它把 resolved_summary
 * 里解析后的实际影响面（目标机、账号、脚本、高危命中、生效的默认值）逐条摊开，高危项打 highlight。
 * 原始参数区默认折叠 —— 完整参数必须可查，但摊在最前面只会淹没关键信息、加重盲签。
 * <p>
 * <b>敏感字段的呈现方式由 {@link ApprovalSensitiveFields} 统一登记</b>，本类不自行判断哪个字段敏感。
 * 脚本内容是唯一原样展示的敏感字段：不展示则审批人无从判断风险。
 */
@Slf4j
@Service
public class DefaultApprovalTicketRenderer implements ApprovalTicketRenderer {

    /**
     * 掩码占位符。长度固定，连原值长度都不泄露
     */
    private static final String MASK = "******";

    /**
     * 原始参数区最多展开的字段数。参数体理论上可以很大（如上千台主机的 host_id 列表），
     * 全量摊开会把单据撑到没人看得完，反而不利于审批
     */
    private static final int MAX_RAW_PARAM_FIELDS = 200;

    /**
     * 执行对象列表在单据中最多逐个列出的条数，超出只给总数
     */
    private static final int MAX_LISTED_EXECUTE_OBJECTS = 20;

    private static final String I18N_PREFIX = "task.approval.ticket.";

    private final MessageI18nService i18nService;
    private final CommonAppService appService;
    private final ApprovalParamsCryptoService paramsCryptoService;

    public DefaultApprovalTicketRenderer(MessageI18nService i18nService,
                                         CommonAppService appService,
                                         ApprovalParamsCryptoService paramsCryptoService) {
        this.i18nService = i18nService;
        this.appService = appService;
        this.paramsCryptoService = paramsCryptoService;
    }

    @Override
    public ApprovalTicket render(ApprovalTaskDTO task) {
        ApprovalOperationTypeEnum operationType = ApprovalOperationTypeEnum.valOf(task.getOperationType());
        ResolvedSummary summary = parseSummary(task);
        BasicApp app = loadApp(task.getAppId());

        ApprovalTicket ticket = new ApprovalTicket();
        ticket.setApprovalTaskId(task.getApprovalTaskId());
        ticket.setOperationType(task.getOperationType());
        ticket.setCreator(task.getCreator());
        ticket.setExpireAt(task.getExpireAt());
        ticket.setScope(app == null ? null : app.getScope());
        ApprovalRiskLevelEnum riskLevel = resolveRiskLevel(summary);
        ticket.setRiskLevel(riskLevel.name());
        ticket.setTitle(buildTitle(operationType, app, summary));

        fillSummarySection(ticket, task, operationType, app, summary, riskLevel);
        fillRawParamsSection(ticket, task, operationType);
        return ticket;
    }

    /**
     * 概要区：默认展开
     */
    private void fillSummarySection(ApprovalTicket ticket,
                                    ApprovalTaskDTO task,
                                    ApprovalOperationTypeEnum operationType,
                                    BasicApp app,
                                    ResolvedSummary summary,
                                    ApprovalRiskLevelEnum riskLevel) {
        ApprovalTicket.Section section = ticket.addSection(
            ApprovalTicket.SECTION_SUMMARY, label("section.summary"), false);

        section.addField(ApprovalTicket.Field.of(label("operationType"), operationName(operationType)));
        section.addField(ApprovalTicket.Field.of(label("scope"), describeScope(app, task.getAppId())));
        section.addField(ApprovalTicket.Field.of(label("creator"), task.getCreator()));
        section.addField(ApprovalTicket.Field.of(label("riskLevel"), riskLevelName(riskLevel)));
        if (StringUtils.isNotBlank(summary.getName())) {
            section.addField(ApprovalTicket.Field.of(label("name"), summary.getName()));
        }
        addResolvedFields(section, summary.getFields(), StringUtils.EMPTY);

        if (summary.getTotalExecuteObjectCount() != null) {
            section.addField(ApprovalTicket.Field.of(label("totalExecuteObjectCount"),
                String.valueOf(summary.getTotalExecuteObjectCount())));
        }
        if (Boolean.TRUE.equals(summary.getDangerousRuleMatched())) {
            section.addField(ApprovalTicket.Field.highlighted(
                label("dangerousRuleMatched"), label("value.dangerousRuleMatched")));
        }
        // 动态目标的实际影响面在放行时才确定，这是 B3 已知限制，必须如实披露给审批人
        if (Boolean.TRUE.equals(summary.getContainsDynamicTarget())) {
            section.addField(ApprovalTicket.Field.highlighted(
                label("containsDynamicTarget"), label("value.dynamicTargetHint")));
        }

        List<ResolvedSummary.ResolvedStep> steps = summary.getSteps();
        if (CollectionUtils.isNotEmpty(steps)) {
            for (int i = 0; i < steps.size(); i++) {
                fillStepFields(section, steps.get(i), steps.size() == 1 ? StringUtils.EMPTY
                    : labelWithArgs("stepPrefix", i + 1));
            }
        }

        if (CollectionUtils.isNotEmpty(summary.getDefaultsApplied())) {
            // 默认生效的参数是用户没写、系统替他决定的部分，后果与显式指定完全一样，必须逐项披露
            addResolvedFields(section, summary.getDefaultsApplied(), label("defaultPrefix"));
        }
    }

    private void fillStepFields(ApprovalTicket.Section section,
                                ResolvedSummary.ResolvedStep step,
                                String labelPrefix) {
        if (StringUtils.isNotBlank(step.getName())) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.name")), step.getName()));
        }
        if (StringUtils.isNotBlank(step.getExecuteType())) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.executeType")),
                step.getExecuteType()));
        }
        if (StringUtils.isNotBlank(step.getAccountAlias())) {
            // root 等高危账号意味着目标机上不受限，必须显著标注
            boolean highRisk = Boolean.TRUE.equals(step.getHighRiskAccount());
            String accountLabel = prefixed(labelPrefix, label("step.account"));
            section.addField(highRisk
                ? ApprovalTicket.Field.highlighted(accountLabel, step.getAccountAlias())
                : ApprovalTicket.Field.of(accountLabel, step.getAccountAlias()));
        }
        if (StringUtils.isNotBlank(step.getScriptName())) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.scriptName")),
                step.getScriptName()));
        }
        if (step.getScriptVersionId() != null) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.scriptVersionId")),
                String.valueOf(step.getScriptVersionId())));
        }
        if (StringUtils.isNotBlank(step.getDangerousCheckSummary())) {
            section.addField(ApprovalTicket.Field.highlighted(prefixed(labelPrefix, label("step.dangerousCheck")),
                step.getDangerousCheckSummary()));
        }
        if (step.getExecuteObjectCount() != null) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.executeObjectCount")),
                String.valueOf(step.getExecuteObjectCount())));
        }
        String executeObjects = describeExecuteObjects(step);
        if (executeObjects != null) {
            section.addField(ApprovalTicket.Field.of(prefixed(labelPrefix, label("step.executeObjects")),
                executeObjects));
        }
        if (Boolean.TRUE.equals(step.getContainsDynamicTarget())) {
            section.addField(ApprovalTicket.Field.highlighted(prefixed(labelPrefix, label("containsDynamicTarget")),
                label("value.dynamicTargetHint")));
        }
        addResolvedFields(section, step.getFields(), labelPrefix);
    }

    /**
     * 原始参数区：默认折叠，敏感字段只出占位符
     */
    private void fillRawParamsSection(ApprovalTicket ticket,
                                      ApprovalTaskDTO task,
                                      ApprovalOperationTypeEnum operationType) {
        ApprovalTicket.Section section = ticket.addSection(
            ApprovalTicket.SECTION_RAW_PARAMS, label("section.rawParams"), true);
        if (StringUtils.isBlank(task.getOperationParams())) {
            return;
        }
        JsonNode root;
        try {
            root = JsonUtils.toJsonNode(paramsCryptoService.decryptSensitiveFields(
                operationType, task.getOperationParams()));
        } catch (Exception e) {
            // 参数区渲染失败不能让整张单据出不来：概要区仍然可用，审批人至少不是完全瞎的
            log.error("Render raw params failed, approvalTaskId: {}", task.getApprovalTaskId(), e);
            root = null;
        }
        if (root == null) {
            section.addField(ApprovalTicket.Field.of(label("section.rawParams"), label("value.rawParamsRenderFail")));
            return;
        }

        Set<String> sensitivePaths = new HashSet<>();
        for (SensitiveField field : ApprovalSensitiveFields.of(operationType)) {
            applyTicketDisplay(root, field, 0, StringUtils.EMPTY, sensitivePaths);
        }
        flatten(root, StringUtils.EMPTY, section, sensitivePaths);
    }

    /**
     * 按登记的呈现方式改写敏感字段的值，并记录哪些扁平化路径是敏感字段。
     * <p>
     * <b>改写发生在扁平化之前</b>：先把树里的敏感值换成占位符，再逐叶子输出，
     * 这样即便敏感字段藏在数组元素里，也不会有任何一条 Field 的 value 携带原值。
     */
    private void applyTicketDisplay(JsonNode node,
                                    SensitiveField field,
                                    int depth,
                                    String pathPrefix,
                                    Set<String> sensitivePaths) {
        if (node == null || node.isNull()) {
            return;
        }
        List<String> path = field.getPath();
        String segment = path.get(depth);
        boolean lastSegment = depth == path.size() - 1;
        if (ApprovalSensitiveFields.ARRAY_WILDCARD.equals(segment)) {
            if (!(node instanceof ArrayNode)) {
                return;
            }
            for (int i = 0; i < node.size(); i++) {
                applyTicketDisplay(node.get(i), field, depth + 1,
                    pathPrefix + "[" + i + "]", sensitivePaths);
            }
            return;
        }
        if (!(node instanceof ObjectNode)) {
            return;
        }
        ObjectNode objectNode = (ObjectNode) node;
        String childPath = joinPath(pathPrefix, segment);
        if (!lastSegment) {
            applyTicketDisplay(objectNode.get(segment), field, depth + 1, childPath, sensitivePaths);
            return;
        }
        JsonNode leaf = objectNode.get(segment);
        if (leaf == null || !leaf.isTextual() || StringUtils.isEmpty(leaf.asText())) {
            return;
        }
        if (!shouldMask(objectNode, field)) {
            // 用户没声明为敏感：原样展示才有助于判断风险，只是 BASE64 需先解码
            if (field.isBase64Encoded()) {
                objectNode.put(segment, decodeBase64(leaf.asText()));
            }
            return;
        }
        switch (field.getTicketDisplay()) {
            case PLAIN_TEXT:
                // 脚本内容：审批人要审的对象本身，不展示等于盲签
                if (field.isBase64Encoded()) {
                    objectNode.put(segment, decodeBase64(leaf.asText()));
                }
                break;
            case PASSWORD_PROVIDED:
                objectNode.put(segment, label("value.passwordProvided"));
                sensitivePaths.add(childPath);
                break;
            case MASKED:
            default:
                objectNode.put(segment, MASK);
                sensitivePaths.add(childPath);
                break;
        }
    }

    /**
     * 有脱敏条件时只有条件为 true 才脱敏（如 script_param 仅在用户声明为敏感参数时打码）
     */
    private boolean shouldMask(ObjectNode parent, SensitiveField field) {
        List<String> conditionPath = field.getMaskConditionPath();
        if (CollectionUtils.isEmpty(conditionPath)) {
            return true;
        }
        JsonNode node = parent;
        for (String segment : conditionPath) {
            if (node == null || !node.isObject()) {
                return false;
            }
            node = node.get(segment);
        }
        return node != null && node.asBoolean(false);
    }

    /**
     * 把 JSON 树摊成"路径 - 值"的字段列表。标量叶子逐条输出，方便渠道逐行展示
     */
    private void flatten(JsonNode node, String path, ApprovalTicket.Section section, Set<String> sensitivePaths) {
        if (node == null || node.isNull()) {
            return;
        }
        if (isFieldLimitReached(section)) {
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                flatten(entry.getValue(), joinPath(path, entry.getKey()), section, sensitivePaths);
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                flatten(node.get(i), path + "[" + i + "]", section, sensitivePaths);
            }
            return;
        }
        boolean sensitive = sensitivePaths.contains(path);
        section.addField(sensitive
            ? ApprovalTicket.Field.sensitive(path, node.asText())
            : ApprovalTicket.Field.of(path, node.asText()));
    }

    private boolean isFieldLimitReached(ApprovalTicket.Section section) {
        List<ApprovalTicket.Field> fields = section.getFields();
        if (fields == null || fields.size() < MAX_RAW_PARAM_FIELDS) {
            return false;
        }
        if (fields.size() == MAX_RAW_PARAM_FIELDS) {
            section.addField(ApprovalTicket.Field.of(label("rawParams.truncated"),
                labelWithArgs("value.rawParamsTruncated", MAX_RAW_PARAM_FIELDS)));
        }
        return true;
    }

    /**
     * 风险等级只是给审批人的提示强度，不参与放行校验
     */
    private ApprovalRiskLevelEnum resolveRiskLevel(ResolvedSummary summary) {
        if (Boolean.TRUE.equals(summary.getDangerousRuleMatched()) || containsHighRiskAccount(summary)) {
            return ApprovalRiskLevelEnum.HIGH;
        }
        Integer count = summary.getTotalExecuteObjectCount();
        boolean largeScale = count != null
            && count >= ApprovalRiskLevelEnum.MEDIUM_RISK_EXECUTE_OBJECT_COUNT;
        if (largeScale || Boolean.TRUE.equals(summary.getContainsDynamicTarget())) {
            return ApprovalRiskLevelEnum.MEDIUM;
        }
        return ApprovalRiskLevelEnum.LOW;
    }

    private boolean containsHighRiskAccount(ResolvedSummary summary) {
        if (CollectionUtils.isEmpty(summary.getSteps())) {
            return false;
        }
        for (ResolvedSummary.ResolvedStep step : summary.getSteps()) {
            if (Boolean.TRUE.equals(step.getHighRiskAccount())) {
                return true;
            }
        }
        return false;
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

    private String describeExecuteObjects(ResolvedSummary.ResolvedStep step) {
        List<ResolvedSummary.ResolvedExecuteObject> executeObjects = step.getExecuteObjects();
        if (CollectionUtils.isEmpty(executeObjects)) {
            return null;
        }
        int listedCount = Math.min(executeObjects.size(), MAX_LISTED_EXECUTE_OBJECTS);
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < listedCount; i++) {
            joiner.add(StringUtils.defaultString(executeObjects.get(i).getDisplay()));
        }
        StringBuilder value = new StringBuilder(joiner.toString());
        int total = step.getExecuteObjectCount() == null ? executeObjects.size() : step.getExecuteObjectCount();
        if (total > listedCount) {
            value.append(' ').append(labelWithArgs("value.moreExecuteObjects", total - listedCount));
        }
        return value.toString();
    }

    private void addResolvedFields(ApprovalTicket.Section section,
                                   List<ResolvedSummary.ResolvedField> fields,
                                   String labelPrefix) {
        if (CollectionUtils.isEmpty(fields)) {
            return;
        }
        for (ResolvedSummary.ResolvedField field : fields) {
            String fieldLabel = prefixed(labelPrefix, resolvedFieldLabel(field.getLabel()));
            section.addField(field.isHighlight()
                ? ApprovalTicket.Field.highlighted(fieldLabel, field.getValue())
                : ApprovalTicket.Field.of(fieldLabel, field.getValue()));
        }
    }

    /**
     * 下游回传的 label 是稳定的字段名（如 file_target_path），能翻译就翻译，翻不了就原样展示 —— 缺文案
     * 不该让单据取不出来
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
            // 业务信息查不到只影响标题好看程度，不该让整张单据出不来
            log.warn("Get app failed while rendering approval ticket, appId: {}", appId, e);
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

    private String decodeBase64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // 不是合法 BASE64 时原样返回：展示原值好过让审批人看到一个空字段
            return value;
        }
    }

    private String joinPath(String prefix, String segment) {
        return StringUtils.isEmpty(prefix) ? segment : prefix + "." + segment;
    }

    private String prefixed(String prefix, String label) {
        return StringUtils.isEmpty(prefix) ? label : prefix + " " + label;
    }
}
