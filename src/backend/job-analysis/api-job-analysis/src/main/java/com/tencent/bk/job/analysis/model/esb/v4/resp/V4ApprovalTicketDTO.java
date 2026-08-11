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

package com.tencent.bk.job.analysis.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 应用态取单接口的返回体。审批渠道据此建单。
 * <p>
 * 字段化而非渲染好的富文本，便于不同渠道按自身展示能力各自渲染；
 * {@code sensitive=true} 的字段值<b>只会是占位符</b>，不含真实敏感值。
 */
@Data
public class V4ApprovalTicketDTO {

    @JsonProperty("approval_task_id")
    private String approvalTaskId;

    /**
     * 单据标题，形如「快速执行脚本 - 某业务 - 37个执行对象」
     */
    @JsonProperty("title")
    private String title;

    /**
     * 风险等级，可选值：HIGH / MEDIUM / LOW
     */
    @JsonProperty("risk_level")
    private String riskLevel;

    @JsonProperty("operation_type")
    private String operationType;

    /**
     * 资源范围类型，可选值：biz / biz_set
     */
    @JsonProperty("bk_scope_type")
    private String scopeType;

    @JsonProperty("bk_scope_id")
    private String scopeId;

    /**
     * 发起人。审批人必须为发起人本人，渠道据此校验
     */
    @JsonProperty("creator")
    private String creator;

    /**
     * 过期时刻，Unix 时间戳，单位毫秒；过期后不可再放行
     */
    @JsonProperty("expire_at")
    private Long expireAt;

    @JsonProperty("sections")
    private List<Section> sections;

    /**
     * 单据区块
     */
    @Data
    public static class Section {

        /**
         * 区块标识，可选值：summary / raw_params
         */
        @JsonProperty("key")
        private String key;

        @JsonProperty("title")
        private String title;

        /**
         * 渲染建议，true 表示建议默认折叠；渠道不支持折叠时全部展开也不影响正确性
         */
        @JsonProperty("collapsed")
        private Boolean collapsed;

        @JsonProperty("fields")
        private List<Field> fields;
    }

    /**
     * 单据字段
     */
    @Data
    public static class Field {

        @JsonProperty("label")
        private String label;

        /**
         * 字段值。sensitive 为 true 时此处仅为占位符，不含真实值
         */
        @JsonProperty("value")
        private String value;

        @JsonProperty("sensitive")
        private Boolean sensitive;

        /**
         * 是否为需要显著标注的高危项，如 root 账号、命中高危脚本规则、动态分组目标
         */
        @JsonProperty("highlight")
        private Boolean highlight;
    }
}
