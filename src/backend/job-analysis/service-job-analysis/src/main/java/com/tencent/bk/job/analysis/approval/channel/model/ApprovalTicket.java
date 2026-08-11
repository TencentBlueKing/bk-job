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

package com.tencent.bk.job.analysis.approval.channel.model;

import com.tencent.bk.job.common.model.dto.ResourceScope;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批单据。
 * <p>
 * <b>这是审批人做判断的唯一信息来源</b>：概要区看不清"要在哪些机器上执行什么"，就等于逼审批人盲签，
 * 本 Issue 的安全价值也随之归零。因此单据必须自包含，关键信息不依赖 AI 对话上下文。
 * <p>
 * 字段化而非渲染好的富文本，便于不同渠道按自身展示能力各自渲染。{@link Section#collapsed}
 * 只是渲染建议，渠道不支持折叠时全部展开也不影响正确性。
 * <p>
 * <b>本期一任务一单据，不做批次合并展示</b>：模型中刻意不引入批次 ID 与聚合渲染概念。
 */
@Data
public class ApprovalTicket {

    /**
     * 操作概要区的 section key。默认展开，是审批人判断风险的主依据
     */
    public static final String SECTION_SUMMARY = "summary";

    /**
     * 原始参数区的 section key。默认折叠，避免淹没关键信息、加重盲签
     */
    public static final String SECTION_RAW_PARAMS = "raw_params";

    private String approvalTaskId;

    /**
     * 单据标题，形如「快速执行脚本 - 某业务 - 37台主机」
     */
    private String title;

    /**
     * 风险等级，取值见 ApprovalRiskLevelEnum
     */
    private String riskLevel;

    private String operationType;

    private ResourceScope scope;

    /**
     * 发起人。审批人必须为发起人本人，渠道据此校验
     */
    private String creator;

    /**
     * 过期时刻（毫秒）。过期后不可再放行，渠道应据此提示审批人
     */
    private Long expireAt;

    private List<Section> sections;

    public Section addSection(String key, String title, boolean collapsed) {
        Section section = new Section();
        section.setKey(key);
        section.setTitle(title);
        section.setCollapsed(collapsed);
        if (sections == null) {
            sections = new ArrayList<>();
        }
        sections.add(section);
        return section;
    }

    /**
     * 单据区块
     */
    @Data
    public static class Section {

        private String key;

        private String title;

        /**
         * 渲染建议：true 表示建议默认折叠
         */
        private boolean collapsed;

        private List<Field> fields;

        public Section addField(Field field) {
            if (field == null) {
                return this;
            }
            if (fields == null) {
                fields = new ArrayList<>();
            }
            fields.add(field);
            return this;
        }
    }

    /**
     * 单据字段
     */
    @Data
    public static class Field {

        private String label;

        /**
         * 字段值。{@link #sensitive} 为 true 时<b>只允许是占位符</b>，绝不能是明文或密文
         */
        private String value;

        /**
         * 是否为敏感字段，true 表示服务端已将其替换为占位符
         */
        private boolean sensitive;

        /**
         * 是否为需要显著标注的高危项，如 root 账号、命中高危脚本规则、动态分组目标
         */
        private boolean highlight;

        public static Field of(String label, String value) {
            return build(label, value, false, false);
        }

        public static Field highlighted(String label, String value) {
            return build(label, value, false, true);
        }

        /**
         * 敏感字段。传入的必须已经是占位符
         */
        public static Field sensitive(String label, String placeholder) {
            return build(label, placeholder, true, false);
        }

        private static Field build(String label, String value, boolean sensitive, boolean highlight) {
            Field field = new Field();
            field.setLabel(label);
            field.setValue(value);
            field.setSensitive(sensitive);
            field.setHighlight(highlight);
            return field;
        }
    }
}
