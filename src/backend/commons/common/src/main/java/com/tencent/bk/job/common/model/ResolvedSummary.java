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

package com.tencent.bk.job.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * dryRun 预检解析出的操作概要。
 * <p>
 * <b>这是审批人唯一能看清"到底要在哪些机器上执行什么"的信息来源</b>：审批单据的概要区完全由它渲染，
 * 内容不足会直接导致盲签。因此它记录的是<b>解析后的实际影响面</b>（实际目标机、账号别名、脚本版本、
 * 高危命中），而不是用户原始入参里的动态分组 ID 之类的间接引用。
 * <p>
 * 由各下游服务在 dryRun 返回点之后填充并随响应回传，由 job-analysis 一次性序列化进
 * approval_task.resolved_summary，取单时直接读库渲染，不必每次取单都重跑执行对象解析。
 * <p>
 * 它既随 EsbV4Response#dryRunSummary 返回给开放接口调用方，又原样落库做快照，
 * 因此所有字段必须按 v4 开放接口约定序列化为下划线命名，新增字段一律逐个标注 {@link JsonProperty}。
 */
@Data
public class ResolvedSummary {

    /**
     * 执行对象列表的最大保留条数。超出部分不再逐条带回，只保留总数与截断标记：
     * 单据展示不需要逐台列出上千台主机，而快照要落库，无节制会把 resolved_summary 撑爆
     */
    public static final int MAX_EXECUTE_OBJECT_COUNT = 100;

    /**
     * 一个概要条目里多条内容之间的分隔符（如一个步骤的多个文件源）。
     * <p>
     * 下游按它拼接，单据渲染侧按它拆开逐条截断，因此是两侧共用的格式约定，不能各写各的
     */
    public static final String ITEM_SEPARATOR = "; ";

    /**
     * 主机类全局变量逐台列出的台数上限，超出只保留总数。
     * <p>
     * 少量主机逐台列出，审批人才看得出这次到底动哪几台；上百台逐台列出只会把单据铺成一堵墙，
     * 反而让人直接划到底放行，此时台数本身已足够判断影响面
     */
    public static final int MAX_GLOBAL_VAR_HOST_COUNT = 10;

    /**
     * 全局变量取值在概要里保留的最大字符数，超出截断。
     * <p>
     * 关联数组、索引数组类变量的取值可能很长，而概要要整份落库进 approval_task.resolved_summary
     */
    public static final int MAX_GLOBAL_VAR_VALUE_LENGTH = 512;

    private static final String TRUNCATED_SUFFIX = "…";

    /**
     * 操作类型，取值见 job-analysis 的 ApprovalOperationTypeEnum
     */
    @JsonProperty("operation_type")
    private String operationType;

    /**
     * 操作对象名称，如作业名、执行方案名、定时任务名，供单据标题使用
     */
    @JsonProperty("name")
    private String name;

    /**
     * 操作级别的概要条目，如执行方案 ID、定时任务表达式等
     */
    @JsonProperty("fields")
    private List<ResolvedField> fields;

    /**
     * 分步骤解析结果。快速执行脚本/分发文件只有一个步骤；创建执行方案与定时任务操作不涉及步骤，此处为空
     */
    @JsonProperty("steps")
    private List<ResolvedStep> steps;

    /**
     * 全部步骤去重后的执行对象总数，即"将在多少台主机/容器上执行"
     */
    @JsonProperty("total_execute_object_count")
    private Integer totalExecuteObjectCount;

    /**
     * 是否存在动态分组 / 拓扑节点目标。为 true 时单据必须提示"实际执行台数在放行时重新解析确定"
     */
    @JsonProperty("contains_dynamic_target")
    private Boolean containsDynamicTarget;

    /**
     * 是否命中高危脚本规则。为 true 时单据须显著标注
     */
    @JsonProperty("dangerous_rule_matched")
    private Boolean dangerousRuleMatched;

    /**
     * 未显式指定而按默认生效的参数，逐项说明后果（如文件分发默认强制模式会覆盖同名文件）
     */
    @JsonProperty("defaults_applied")
    private List<ResolvedField> defaultsApplied;

    /**
     * 本次操作生效的全局变量，含未在请求中指定、沿用执行方案/作业模板默认值的那些。
     * <p>
     * <b>全部变量都要列出</b>：只列请求里传的那几个，审批人无法判断这个方案实际会拿什么参数去跑。
     * 主机类变量的台数计入风险等级判定，因此即便台数超过 {@link #MAX_GLOBAL_VAR_HOST_COUNT}
     * 不再逐台列出，{@link ResolvedGlobalVar#hostCount} 也必须如实带回
     */
    @JsonProperty("global_vars")
    private List<ResolvedGlobalVar> globalVars;

    public void addField(String label, String value) {
        if (value == null) {
            return;
        }
        if (fields == null) {
            fields = new ArrayList<>();
        }
        fields.add(new ResolvedField(label, value));
    }

    public void addStep(ResolvedStep step) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(step);
    }

    public void addDefaultApplied(String label, String value) {
        if (defaultsApplied == null) {
            defaultsApplied = new ArrayList<>();
        }
        defaultsApplied.add(new ResolvedField(label, value));
    }

    public void addGlobalVar(ResolvedGlobalVar globalVar) {
        if (globalVar == null) {
            return;
        }
        if (globalVars == null) {
            globalVars = new ArrayList<>();
        }
        globalVars.add(globalVar);
    }

    /**
     * 全部主机类变量的主机台数合计，供风险等级判定使用。动态分组与拓扑节点算不出台数，不计入
     */
    public int totalGlobalVarHostCount() {
        if (globalVars == null) {
            return 0;
        }
        int total = 0;
        for (ResolvedGlobalVar globalVar : globalVars) {
            if (globalVar.getHostCount() != null) {
                total += globalVar.getHostCount();
            }
        }
        return total;
    }

    /**
     * 概要条目
     */
    @Data
    public static class ResolvedField {

        /**
         * 展示名
         */
        @JsonProperty("label")
        private String label;

        /**
         * 展示值
         */
        @JsonProperty("value")
        private String value;

        /**
         * 是否高危项，需在单据中显著标注
         */
        @JsonProperty("highlight")
        private boolean highlight;

        public ResolvedField() {
        }

        public ResolvedField(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public ResolvedField(String label, String value, boolean highlight) {
            this.label = label;
            this.value = value;
            this.highlight = highlight;
        }
    }

    /**
     * 一个生效的全局变量。
     * <p>
     * <b>密文类型变量的取值一律不带</b>：本概要以明文落库，取值只在参数快照里加密保存，渲染侧对密文
     * 变量出占位符即可。主机类变量的取值走 {@link #addHost}，其余类型走 {@link #setValue}。
     */
    @Data
    public static class ResolvedGlobalVar {

        /**
         * 变量名
         */
        @JsonProperty("name")
        private String name;

        /**
         * 变量类型，取值见 TaskVariableTypeEnum 的枚举名
         */
        @JsonProperty("type")
        private String type;

        /**
         * 非主机类变量的取值。密文类型不带，超长按 {@link #MAX_GLOBAL_VAR_VALUE_LENGTH} 截断
         */
        @JsonProperty("value")
        private String value;

        /**
         * 本次请求是否显式指定了该变量的取值。
         * <p>
         * 为 false 表示取值沿用现有配置（作业模板默认值、执行方案默认值或定时任务原有取值），
         * 单据要把这件事标出来：同一个取值是"本次改成这样"还是"一直就是这样"，审批结论可能完全不同
         */
        @JsonProperty("assigned")
        private Boolean assigned;

        /**
         * 主机类变量解析出的主机，形如 云区域ID:IP；台数超过 {@link #MAX_GLOBAL_VAR_HOST_COUNT} 时为空
         */
        @JsonProperty("hosts")
        private List<String> hosts;

        /**
         * 主机类变量的静态主机台数，无论是否逐台列出都如实带回
         */
        @JsonProperty("host_count")
        private Integer hostCount;

        /**
         * 主机类变量里动态分组的个数。实际主机在放行时才解析，台数算不出来
         */
        @JsonProperty("dynamic_group_count")
        private Integer dynamicGroupCount;

        /**
         * 主机类变量里拓扑节点的个数。实际主机在放行时才解析，台数算不出来
         */
        @JsonProperty("topo_node_count")
        private Integer topoNodeCount;

        /**
         * 主机类变量里容器的个数
         */
        @JsonProperty("container_count")
        private Integer containerCount;

        /**
         * 手写 setter 让截断成为绕不过去的不变式：调用方各写一遍必然有人漏掉，
         * 而单个变量取值就能把整份概要撑到落库失败
         */
        public void setValue(String value) {
            if (value != null && value.length() > MAX_GLOBAL_VAR_VALUE_LENGTH) {
                this.value = value.substring(0, MAX_GLOBAL_VAR_VALUE_LENGTH) + TRUNCATED_SUFFIX;
                return;
            }
            this.value = value;
        }

        /**
         * 逐台累计主机。台数超过上限后清掉已收集的清单、只留总数，
         * <b>上限判断收在这里</b>，免得各下游各写一份判断而写出不一致的单据
         *
         * @param hostId  主机 ID，cloudIp 为空时用它兜底展示
         * @param cloudIp 云区域ID:IP，审批人真正看得懂的那个值
         */
        public void addHost(Long hostId, String cloudIp) {
            String display = hostDisplay(hostId, cloudIp);
            if (display == null) {
                // 既无 IP 又无主机 ID，展示不了也数不清，不计入台数
                return;
            }
            hostCount = hostCount == null ? 1 : hostCount + 1;
            if (hostCount > MAX_GLOBAL_VAR_HOST_COUNT) {
                hosts = null;
                return;
            }
            if (hosts == null) {
                hosts = new ArrayList<>();
            }
            hosts.add(display);
        }

        private String hostDisplay(Long hostId, String cloudIp) {
            if (cloudIp != null && !cloudIp.trim().isEmpty()) {
                return cloudIp.trim();
            }
            return hostId == null ? null : "host_id:" + hostId;
        }
    }

    /**
     * 步骤级解析结果
     */
    @Data
    public static class ResolvedStep {

        @JsonProperty("name")
        private String name;

        /**
         * 步骤类型，如 EXECUTE_SCRIPT / SEND_FILE
         */
        @JsonProperty("execute_type")
        private String executeType;

        /**
         * 解析出的执行账号别名。不是用户传的 accountId，而是审批人能看懂的别名
         */
        @JsonProperty("account_alias")
        private String accountAlias;

        /**
         * 是否 root 等高危账号
         */
        @JsonProperty("high_risk_account")
        private Boolean highRiskAccount;

        @JsonProperty("script_name")
        private String scriptName;

        /**
         * 脚本版本 ID。引用已有脚本时有值，手工录入脚本内容时为空
         */
        @JsonProperty("script_version_id")
        private Long scriptVersionId;

        /**
         * 脚本来源，取值见 ScriptSourceEnum：1-手工录入 2-引用业务脚本 3-引用公共脚本
         */
        @JsonProperty("script_source")
        private Integer scriptSource;

        /**
         * 高危脚本规则命中概要。有值即须在单据中显著标注
         */
        @JsonProperty("dangerous_check_summary")
        private String dangerousCheckSummary;

        /**
         * 本步骤解析出的执行对象总数
         */
        @JsonProperty("execute_object_count")
        private Integer executeObjectCount;

        /**
         * 本步骤解析出的执行对象，最多 {@link #MAX_EXECUTE_OBJECT_COUNT} 条
         */
        @JsonProperty("execute_objects")
        private List<ResolvedExecuteObject> executeObjects;

        /**
         * 执行对象列表是否因超过上限被截断
         */
        @JsonProperty("execute_object_truncated")
        private Boolean executeObjectTruncated;

        /**
         * 目标是否为动态分组 / 拓扑节点 / 容器过滤器
         */
        @JsonProperty("contains_dynamic_target")
        private Boolean containsDynamicTarget;

        /**
         * 步骤级补充条目，如文件源、目标路径
         */
        @JsonProperty("fields")
        private List<ResolvedField> fields;

        public void addField(String label, String value) {
            if (value == null) {
                return;
            }
            if (fields == null) {
                fields = new ArrayList<>();
            }
            fields.add(new ResolvedField(label, value));
        }
    }

    /**
     * 解析出的单个执行对象
     */
    @Data
    public static class ResolvedExecuteObject {

        /**
         * 执行对象类型：HOST / CONTAINER
         */
        @JsonProperty("type")
        private String type;

        @JsonProperty("id")
        private Long id;

        /**
         * 展示值，主机为 云区域ID:IP，容器为容器名
         */
        @JsonProperty("display")
        private String display;

        public ResolvedExecuteObject() {
        }

        public ResolvedExecuteObject(String type, Long id, String display) {
            this.type = type;
            this.id = id;
            this.display = display;
        }
    }
}
