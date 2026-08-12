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

package com.tencent.bk.job.analysis.approval;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 参数快照中的敏感字段登记表。
 * <p>
 * <b>加密落库与单据脱敏共用这一份登记</b>：两处各自维护一张表早晚会漂移成"库里加密了、单据里明文展示"
 * 或"单据里打了码、库里存的是明文"，而这两种漂移都不会有编译或运行期报错。因此本类同时给出
 * JSON 路径与单据呈现方式，{@link ApprovalParamsCryptoService} 与单据渲染都从这里取。
 * <p>
 * <b>新增任何密码/密钥/脚本类字段时必须同步登记</b>，评审时按 6 个 v4 Request DTO 的字段清单逐条核对。
 * <p>
 * 几处与方案 §8.1 字段表的出入，以实际 DTO 为准：{@code host_password_list} 只存在于
 * FAST_EXECUTE_SCRIPT 的 v4 请求体上；FAST_TRANSFER_FILE 与 UPDATE_CRON_STATUS 的 v4 请求体
 * 不含任何密码类字段（账号只以 account_id / account_alias 引用，密码不随请求传输）。
 */
public final class ApprovalSensitiveFields {

    /**
     * 数组通配段：匹配数组中的每一个元素
     */
    public static final String ARRAY_WILDCARD = "*";

    private static final Map<ApprovalOperationTypeEnum, List<SensitiveField>> FIELDS =
        new EnumMap<>(ApprovalOperationTypeEnum.class);

    static {
        FIELDS.put(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, Arrays.asList(
            // 脚本内容是审批人要审的对象本身，不展示等于盲签，因此加密落库但单据里原样展示
            SensitiveField.base64PlainText("script_content"),
            // 脚本参数只在用户声明为敏感参数时才打码，否则同样是判断风险的必要信息
            SensitiveField.base64MaskedIf("script_param", "param_sensitive"),
            SensitiveField.passwordProvided("host_password_list", ARRAY_WILDCARD, "encrypted_password")
        ));
        FIELDS.put(ApprovalOperationTypeEnum.FAST_TRANSFER_FILE, Collections.emptyList());
        // v4 请求体里没有变量类型字段，无法在此判断是否为 CIPHER 变量，
        // 宁可全部加密并打码，也不能漏掉密码类变量
        FIELDS.put(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, Collections.singletonList(
            SensitiveField.masked("global_var_list", ARRAY_WILDCARD, "value")
        ));
        FIELDS.put(ApprovalOperationTypeEnum.CREATE_JOB_PLAN, Collections.singletonList(
            SensitiveField.masked("variables", ARRAY_WILDCARD, "value")
        ));
        FIELDS.put(ApprovalOperationTypeEnum.SAVE_CRON, Collections.singletonList(
            SensitiveField.masked("global_var_list", ARRAY_WILDCARD, "value")
        ));
        FIELDS.put(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, Collections.emptyList());
    }

    private ApprovalSensitiveFields() {
    }

    /**
     * 该操作类型的敏感字段登记，永不返回 null
     */
    public static List<SensitiveField> of(ApprovalOperationTypeEnum operationType) {
        List<SensitiveField> fields = operationType == null ? null : FIELDS.get(operationType);
        return fields == null ? Collections.emptyList() : fields;
    }

    /**
     * 敏感字段在审批单据中的呈现方式
     */
    public enum TicketDisplay {
        /**
         * 原样展示。仅用于"不展示就无法判断风险"的字段，目前只有脚本内容
         */
        PLAIN_TEXT,
        /**
         * 固定掩码，只让审批人知道"这里有个值"
         */
        MASKED,
        /**
         * 只披露"提供了自定义密码"这一事实，连长度都不暴露
         */
        PASSWORD_PROVIDED
    }

    /**
     * 一条敏感字段登记。无论呈现方式如何，<b>落库时一律加密</b>
     */
    @Getter
    public static class SensitiveField {

        /**
         * JSON 路径，段名与 DTO 的 {@code @JsonProperty} 一致，{@link #ARRAY_WILDCARD} 表示数组通配
         */
        private final List<String> path;

        private final TicketDisplay ticketDisplay;

        /**
         * 值是否为 BASE64 编码。原样展示时需先解码，否则审批人看到的是一串看不出风险的乱码
         */
        private final boolean base64Encoded;

        /**
         * 脱敏条件字段的路径（与本字段同级）。为空表示无条件按 {@link #ticketDisplay} 呈现；
         * 非空时仅当该布尔字段为 true 才脱敏，否则原样展示
         */
        private final List<String> maskConditionPath;

        private SensitiveField(List<String> path,
                               TicketDisplay ticketDisplay,
                               boolean base64Encoded,
                               List<String> maskConditionPath) {
            this.path = path;
            this.ticketDisplay = ticketDisplay;
            this.base64Encoded = base64Encoded;
            this.maskConditionPath = maskConditionPath;
        }

        private static SensitiveField masked(String... path) {
            return new SensitiveField(Arrays.asList(path), TicketDisplay.MASKED, false, null);
        }

        private static SensitiveField passwordProvided(String... path) {
            return new SensitiveField(Arrays.asList(path), TicketDisplay.PASSWORD_PROVIDED, false, null);
        }

        private static SensitiveField base64PlainText(String... path) {
            return new SensitiveField(Arrays.asList(path), TicketDisplay.PLAIN_TEXT, true, null);
        }

        private static SensitiveField base64MaskedIf(String path, String conditionPath) {
            return new SensitiveField(Collections.singletonList(path), TicketDisplay.MASKED, true,
                Collections.singletonList(conditionPath));
        }
    }
}
