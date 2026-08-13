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

package com.tencent.bk.job.analysis.approval.channel;

import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批渠道注册表：把 approval_channel 枚举解析为具体的 {@link ApprovalChannel} 实现。
 * <p>
 * <b>枚举之外不存在任何指定渠道的方式</b>：渠道的地址、appCode、密钥全部来自服务端配置
 * （{@link ApprovalProperties}），调用方只能传枚举值，请求 DTO 中不得出现任何 URL / host / token 类字段。
 * <p>
 * <b>无可用实现时的行为是刻意定义的</b>：抛出 APPROVAL_CHANNEL_NOT_SUPPORTED 业务异常，
 * 既不 NPE 也不让应用启动失败。安全机制在依赖未就绪时的正确姿态是不可用，而不是放开。
 */
@Slf4j
@Component
public class ApprovalChannelRegistry {

    private final Map<ApprovalChannelEnum, ApprovalChannel> channelMap =
        new EnumMap<>(ApprovalChannelEnum.class);

    private final ApprovalProperties approvalProperties;

    /**
     * 用 {@link ObjectProvider} 而非 {@code List<ApprovalChannel>} 注入：真实渠道未就绪、Mock 又未开启时
     * 一个实现都没有，直接注入 List 会让应用启动失败。渠道不可用应表现为"该功能不可用"，而不是整个服务起不来
     */
    public ApprovalChannelRegistry(ObjectProvider<ApprovalChannel> channelProvider,
                                   ApprovalProperties approvalProperties) {
        this.approvalProperties = approvalProperties;
        for (ApprovalChannel channel : channelProvider.orderedStream().collect(Collectors.toList())) {
            ApprovalChannel previous = channelMap.put(channel.getChannelType(), channel);
            if (previous != null) {
                // 同一渠道有两个实现同时生效，意味着回查目标不确定，这是必须在启动期暴露的配置错误
                throw new IllegalStateException("Duplicated ApprovalChannel implementation for channel "
                    + channel.getChannelType() + ": " + previous.getClass().getName()
                    + " and " + channel.getClass().getName());
            }
        }
        log.info("Approval channels registered: {}", channelMap.keySet());
    }

    /**
     * 解析渠道实现
     *
     * @throws FailedPreconditionException 渠道无可用实现时抛出，不返回 null
     */
    public ApprovalChannel getChannel(ApprovalChannelEnum channelEnum) {
        if (channelEnum == null) {
            throw new FailedPreconditionException(ErrorCode.APPROVAL_CHANNEL_NOT_SUPPORTED,
                new Object[]{StringUtils.EMPTY});
        }
        ApprovalChannel channel = channelMap.get(channelEnum);
        if (channel == null) {
            log.warn("No available ApprovalChannel implementation for channel {}, registered: {}",
                channelEnum, channelMap.keySet());
            throw new FailedPreconditionException(ErrorCode.APPROVAL_CHANNEL_NOT_SUPPORTED,
                new Object[]{channelEnum.name()});
        }
        return channel;
    }

    /**
     * 解析审批任务实际指派的渠道。
     * <p>
     * 放行时必须走这个方法：回查<b>严格限定在该任务指派的渠道内</b>，不得拿着单号去多个渠道轮询试探
     * —— approval_ticket_id 只是字符串，不同渠道的 ID 空间可能重叠。
     */
    public ApprovalChannel getChannelByName(String channelName) {
        ApprovalChannelEnum channelEnum = ApprovalChannelEnum.valOf(channelName);
        if (channelEnum == null) {
            log.warn("Invalid approval channel name: {}", channelName);
            throw new FailedPreconditionException(ErrorCode.APPROVAL_CHANNEL_NOT_SUPPORTED,
                new Object[]{String.valueOf(channelName)});
        }
        return getChannel(channelEnum);
    }

    /**
     * 未指定渠道时使用的服务端默认渠道
     */
    public ApprovalChannelEnum getDefaultChannel() {
        return approvalProperties.getDefaultChannel();
    }

    /**
     * 取该渠道调用作业平台时使用的内置 appCode，未配置时返回空串。
     * <p>
     * 取内容接口据此判断"调用方正是该任务指派的渠道"。<b>未配置一律视为不匹配</b>，
     * 放开校验会让任何有网关权限的应用读到别人的审批内容（含脚本明文）。
     */
    public String getChannelAppCode(String channelName) {
        ApprovalChannelEnum channelEnum = ApprovalChannelEnum.valOf(channelName);
        if (channelEnum == ApprovalChannelEnum.IMATE) {
            return StringUtils.defaultString(approvalProperties.getChannels().getImate().getAppCode());
        }
        return StringUtils.EMPTY;
    }

    /**
     * TODO: 第二个审批渠道接入时，在此按 (tenantId, appCode, operationType) 收窄可选渠道，
     * 避免调用方总是挑审批要求最松的渠道。当前只有一个渠道，默认返回全集。
     */
    public List<ApprovalChannelEnum> resolveAllowedChannels(String tenantId,
                                                            String appCode,
                                                            ApprovalOperationTypeEnum operationType) {
        return Arrays.asList(ApprovalChannelEnum.values());
    }
}
