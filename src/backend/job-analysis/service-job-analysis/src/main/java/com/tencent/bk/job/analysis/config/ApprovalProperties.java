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

package com.tencent.bk.job.analysis.config;

import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 审批域配置。
 * <p>
 * <b>这里只有渠道的内置配置，没有任何"由调用方指定渠道地址"的入口</b>：渠道的地址、appCode、密钥
 * 一律由服务端配置决定，调用方只能传 {@link ApprovalChannelEnum} 枚举值。
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "job.analysis.approval")
public class ApprovalProperties {

    /**
     * 审批任务 TTL，单位小时。TTL 越长，参数快照在库中驻留越久、动态目标漂移窗口越大
     */
    private Integer ttlHours = 8;

    /**
     * 未指定渠道时的默认渠道
     */
    private ApprovalChannelEnum defaultChannel = ApprovalChannelEnum.IMATE;

    /**
     * 审批任务记录的保留天数。独立配置而非复用 AI 的保留期：审批记录是安全审计凭据，
     * 且参数快照含加密后的敏感数据，保留期越长数据面越大
     */
    private Integer maxKeepDays = 30;

    /**
     * "疑似秒批"代理指标的阈值：approved_at - create_time 小于该值即计数。
     * 自动审批的典型特征就是近乎瞬时通过
     */
    private Long fastApproveThresholdMillis = 5000L;

    private ChannelsConfig channels = new ChannelsConfig();

    @Getter
    @Setter
    @ToString
    public static class ChannelsConfig {

        private ImateConfig imate = new ImateConfig();
    }

    @Getter
    @Setter
    @ToString
    public static class ImateConfig {

        /**
         * 渠道服务地址
         */
        private String url;

        /**
         * 该渠道取单时使用的 appCode。取单接口据此校验"调用方正是该任务指派的渠道"
         */
        private String appCode;

        private MockConfig mock = new MockConfig();
    }

    /**
     * Mock 渠道配置。
     * <p>
     * <b>这里刻意没有"默认返回 APPROVED"之类的开关</b>：Mock 的默认结论永远是 PENDING，
     * 放行只能靠 {@link #approvedIds} 里显式登记的 ID 命中。
     */
    @Getter
    @Setter
    @ToString
    public static class MockConfig {

        /**
         * 仅自测环境开启；生产 values 模板中不出现该节点。
         * <p>
         * 该开关决定 Mock 渠道 Bean 是否注册（{@code @ConditionalOnProperty}），因此只能在启动前就位，
         * 改动必须重启 job-analysis
         */
        private boolean enabled = false;

        /**
         * 视为"审批通过"的 ID 列表，与审批任务 ID 或审批单据 ID 任一相等即算通过，其余一律视为未通过。
         * <p>
         * 放在可热刷新的配置中：自测时改桩数据无需重启 job-analysis
         */
        private List<String> approvedIds = new ArrayList<>();
    }
}
