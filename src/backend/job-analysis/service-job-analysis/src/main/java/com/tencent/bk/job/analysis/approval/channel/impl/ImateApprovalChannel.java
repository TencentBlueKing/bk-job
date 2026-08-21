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

package com.tencent.bk.job.analysis.approval.channel.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.approval.channel.ApprovalChannel;
import com.tencent.bk.job.analysis.approval.channel.impl.model.ImateApprovalDetail;
import com.tencent.bk.job.analysis.approval.channel.impl.model.ImateOpenApiResponse;
import com.tencent.bk.job.analysis.approval.channel.model.ApprovalResult;
import com.tencent.bk.job.analysis.approval.consts.ApprovalChannelEnum;
import com.tencent.bk.job.analysis.approval.consts.ApprovalResultStatusEnum;
import com.tencent.bk.job.analysis.config.ApprovalProperties;
import com.tencent.bk.job.analysis.config.condition.ConditionalOnImateApprovalUrlConfigured;
import com.tencent.bk.job.analysis.config.condition.ConditionalOnMockImateApprovalDisabled;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.HttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpRequest;
import com.tencent.bk.job.common.util.http.HttpResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IMate 审批渠道：回查 IMate 侧的审批结论。
 * <p>
 * IMate 以 {@code (x-app-id, taskId)} 唯一定位一张审批单，其中 {@code taskId} 就是作业平台的
 * <b>审批任务 ID</b>——IMate 建单时用它取审批内容，也用它落库。因此回查直接以 approvalTaskId 发起，
 * 响应回带的 {@code taskId} 即 SPI 要求的<b>绑定证明</b>。IMate 没有独立于 taskId 的单据 ID，
 * 所以 {@code approvalTicketId} 参数在本实现中不参与请求构造，接口文档已约定调用方对本渠道
 * 传与 approval_task_id 相同的值。
 * <p>
 * <b>凭证边界</b>：{@code x-app-id} / {@code x-secret} 是 IMate 颁发给作业平台的凭证，
 * 与蓝鲸的 app_code / app_secret 无关，也与"IMate 来取审批内容时校验的蓝鲸 appCode"不是一回事。
 * <p>
 * <b>日志边界</b>：回查是跨系统调用，出问题时必须能对账，所以请求（含请求头）、响应体与耗时都要打印，
 * 但有两条硬约束：
 * <ul>
 *     <li>响应里的 {@code approvalContent} 是作业平台自己渲染的审批正文，<b>含脚本明文</b>，
 *     打印前替换成长度摘要（{@code <masked,length=N>}），只保留对账真正需要的
 *     taskId / status / approver / approveTime；</li>
 *     <li>{@code x-secret} 是 IMate 颁发的凭证，<b>只打字段名不打取值</b>；{@code x-app-id} 不敏感，原样打印。</li>
 * </ul>
 * 注意底层 HttpHelper 在 DEBUG 级别会打印完整响应（不经本类脱敏），生产环境不应对其开启 DEBUG。
 */
@Slf4j
@Component
// 类上叠加的多个条件注解是 AND 关系：Mock 未开启（与 Mock 渠道严格互斥）且渠道地址已配置，才注册真实渠道
@ConditionalOnMockImateApprovalDisabled
@ConditionalOnImateApprovalUrlConfigured
public class ImateApprovalChannel implements ApprovalChannel {

    private static final String APPROVAL_DETAIL_URI = "/open/approval/detail";
    private static final String HEADER_APP_ID = "x-app-id";
    private static final String HEADER_SECRET = "x-secret";

    private static final String FIELD_APPROVAL_CONTENT = "approvalContent";
    private static final String MASKED_VALUE = "<masked>";

    /**
     * 匹配响应体里的 approvalContent 取值（含转义字符的字符串，或 null），用于打印前替换成长度摘要
     */
    private static final Pattern APPROVAL_CONTENT_PATTERN =
        Pattern.compile("\"" + FIELD_APPROVAL_CONTENT + "\"\\s*:\\s*(?:\"((?:\\\\.|[^\"\\\\])*)\"|null)");

    private static final String IMATE_STATUS_PENDING = "PENDING";
    private static final String IMATE_STATUS_APPROVED = "APPROVED";
    private static final String IMATE_STATUS_REJECTED = "REJECTED";
    private static final String IMATE_STATUS_EXPIRED = "EXPIRED";
    private static final String IMATE_STATUS_CANCELED = "CANCELED";

    private final ApprovalProperties approvalProperties;
    private final HttpHelper httpHelper;

    /**
     * 注解不可省略：本类还有一个供单测注入 HttpHelper 的构造器，多构造器且都无注解时
     * Spring 会放弃构造器注入、回退去找无参构造器，导致启动期实例化失败
     */
    @Autowired
    public ImateApprovalChannel(ApprovalProperties approvalProperties) {
        this(approvalProperties, HttpHelperFactory.getDefaultHttpHelper());
    }

    ImateApprovalChannel(ApprovalProperties approvalProperties, HttpHelper httpHelper) {
        this.approvalProperties = approvalProperties;
        this.httpHelper = httpHelper;
        log.info("Approval channel IMATE is enabled, url: {}",
            approvalProperties.getChannels().getImate().getUrl());
    }

    @Override
    public ApprovalChannelEnum getChannelType() {
        return ApprovalChannelEnum.IMATE;
    }

    /**
     * @param approvalTicketId IMate 无独立单据 ID，该参数不参与请求；绑定关系由响应回带的 taskId 证明
     */
    @Override
    public ApprovalResult queryResult(ApprovalTaskDTO task, String approvalTicketId) {
        String approvalTaskId = task.getApprovalTaskId();
        ImateApprovalDetail detail = requestApprovalDetail(approvalTaskId);
        ApprovalResult result = new ApprovalResult();
        result.setStatus(mapStatus(detail.getStatus(), approvalTaskId));
        result.setApprovalTaskId(detail.getTaskId());
        result.setApprover(detail.getApprover());
        result.setApprovedAt(parseApproveTime(detail.getApproveTime()));
        result.setComment(buildComment(detail));
        log.info("Query IMate approval result, approvalTaskId: {}, imateStatus: {}, approver: {}",
            approvalTaskId, detail.getStatus(), detail.getApprover());
        return result;
    }

    /**
     * 发起回查。<b>任何异常都直接向上抛</b>，由调用方 fail-closed 处理，这里不做任何"查不到即通过"的兜底。
     * <p>
     * 请求、响应与耗时都记日志：这是跨系统调用，对端超时或改了返回结构时，日志是唯一的对账依据。
     * 敏感内容的处理见类注释的"日志边界"
     */
    private ImateApprovalDetail requestApprovalDetail(String approvalTaskId) {
        ApprovalProperties.ImateConfig imateConfig = approvalProperties.getChannels().getImate();
        ApprovalProperties.OpenApiConfig openApi = imateConfig.getOpenApi();
        if (StringUtils.isBlank(openApi.getAppId()) || StringUtils.isBlank(openApi.getSecret())) {
            throw new InternalException("IMate open api credential is not configured", ErrorCode.INTERNAL_ERROR);
        }
        Header[] headers = new Header[]{
            new BasicHeader(HEADER_APP_ID, openApi.getAppId()),
            new BasicHeader(HEADER_SECRET, openApi.getSecret()),
            new BasicHeader("Accept", "application/json")
        };
        String url = buildDetailUrl(imateConfig.getUrl(), approvalTaskId);
        log.info("[ImateApprovalChannel] Request|method={}|url={}|headers={}",
            HttpMethodEnum.GET.name(), url, describeHeaders(headers));
        long startTime = System.currentTimeMillis();
        String respStr = null;
        try {
            HttpResponse httpResponse = httpHelper.requestForSuccessResp(
                HttpRequest.builder(HttpMethodEnum.GET, url)
                    .setHeaders(headers)
                    .build());
            respStr = httpResponse.getEntity();
            ImateApprovalDetail detail = parseDetail(respStr, approvalTaskId);
            log.info("[ImateApprovalChannel] Response|method={}|url={}|success=true|costTime={}|resp={}",
                HttpMethodEnum.GET.name(), url, System.currentTimeMillis() - startTime,
                maskApprovalContent(respStr));
            return detail;
        } catch (Exception e) {
            // 超时与对端报错是最需要耗时数据的场景：响应体此时通常为空，耗时是唯一能判断"卡在哪"的线索
            log.warn("[ImateApprovalChannel] Response|method={}|url={}|success=false|costTime={}|resp={}",
                HttpMethodEnum.GET.name(), url, System.currentTimeMillis() - startTime,
                maskApprovalContent(respStr), e);
            throw e;
        }
    }

    private ImateApprovalDetail parseDetail(String respStr, String approvalTaskId) {
        ImateOpenApiResponse<ImateApprovalDetail> response = JsonUtils.fromJson(respStr,
            new TypeReference<ImateOpenApiResponse<ImateApprovalDetail>>() {
            });
        if (response == null || !response.isSuccess()) {
            // message 由 IMate 生成，只描述错误原因，不含审批正文
            throw new InternalException("Query IMate approval detail failed, approvalTaskId: " + approvalTaskId
                + ", status: " + (response == null ? null : response.getStatus())
                + ", message: " + (response == null ? null : response.getMessage()), ErrorCode.INTERNAL_ERROR);
        }
        if (response.getData() == null) {
            throw new InternalException("IMate approval detail is empty, approvalTaskId: " + approvalTaskId,
                ErrorCode.INTERNAL_ERROR);
        }
        return response.getData();
    }

    /**
     * 请求头照打，唯独 {@code x-secret} 只打字段名：凭证一旦落盘，日志的流转范围就是它的泄露范围
     */
    private String describeHeaders(Header[] headers) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Header header : headers) {
            boolean secret = HEADER_SECRET.equalsIgnoreCase(header.getName());
            joiner.add(header.getName() + "=" + (secret ? MASKED_VALUE : header.getValue()));
        }
        return joiner.toString();
    }

    /**
     * 把响应体里的 approvalContent 换成长度摘要：审批正文含脚本明文，不能落盘，但其余字段
     * （taskId / status / approver / approveTime）都是对账必需的，整体不打等于没日志。
     * <p>
     * <b>匹配不上就整体不打</b>：响应体里出现了 approvalContent 这个字段名却取不出取值，说明返回结构
     * 与预期不符，此时宁可只留一个长度也不能赌"里面没有脚本"
     */
    private String maskApprovalContent(String respStr) {
        if (StringUtils.isBlank(respStr) || !respStr.contains(FIELD_APPROVAL_CONTENT)) {
            return respStr;
        }
        Matcher matcher = APPROVAL_CONTENT_PATTERN.matcher(respStr);
        StringBuffer masked = new StringBuffer(respStr.length());
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            String content = matcher.group(1);
            String replacement = "\"" + FIELD_APPROVAL_CONTENT + "\":"
                + (content == null ? "null" : "\"" + maskedLength(content) + "\"");
            matcher.appendReplacement(masked, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(masked);
        return matched ? masked.toString() : maskedLength(respStr);
    }

    private String maskedLength(String value) {
        return "<masked,length=" + value.length() + ">";
    }

    private String buildDetailUrl(String rootUrl, String approvalTaskId) {
        String base = StringUtils.removeEnd(StringUtils.trim(rootUrl), "/");
        return base + APPROVAL_DETAIL_URI + "?taskId="
            + URLEncoder.encode(approvalTaskId, StandardCharsets.UTF_8);
    }

    /**
     * EXPIRED / CANCELED 与 REJECTED 一样是"不会再通过"的终态，统一落成 REJECTED，
     * 让审批任务尽快进入终态而不是一直挂到自身 TTL 过期；原始状态写进 comment 备查。
     * <p>
     * <b>未知状态一律抛异常</b>：无法判定就不能放行，更不能默默当成待审批。
     */
    private ApprovalResultStatusEnum mapStatus(String imateStatus, String approvalTaskId) {
        String status = StringUtils.upperCase(StringUtils.trim(imateStatus));
        if (IMATE_STATUS_APPROVED.equals(status)) {
            return ApprovalResultStatusEnum.APPROVED;
        }
        if (IMATE_STATUS_PENDING.equals(status)) {
            return ApprovalResultStatusEnum.PENDING;
        }
        if (IMATE_STATUS_REJECTED.equals(status)
            || IMATE_STATUS_EXPIRED.equals(status)
            || IMATE_STATUS_CANCELED.equals(status)) {
            return ApprovalResultStatusEnum.REJECTED;
        }
        throw new InternalException("Unknown IMate approval status: " + imateStatus
            + ", approvalTaskId: " + approvalTaskId, ErrorCode.INTERNAL_ERROR);
    }

    /**
     * IMate 给的是不带时区的本地时间，按当前时区还原。
     * 时间只用于展示与审计，解析失败不影响审批结论，返回 null 由上层兜底
     */
    private Long parseApproveTime(String approveTime) {
        if (StringUtils.isBlank(approveTime)) {
            return null;
        }
        try {
            return LocalDateTime.parse(approveTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        } catch (Exception e) {
            log.warn("Parse IMate approveTime failed: {}", approveTime, e);
            return null;
        }
    }

    private String buildComment(ImateApprovalDetail detail) {
        String status = StringUtils.upperCase(StringUtils.trim(detail.getStatus()));
        boolean mappedFromOtherStatus = IMATE_STATUS_EXPIRED.equals(status) || IMATE_STATUS_CANCELED.equals(status);
        if (!mappedFromOtherStatus) {
            return detail.getApproveComment();
        }
        String note = "IMate approval status: " + status;
        return StringUtils.isBlank(detail.getApproveComment())
            ? note : note + "; " + detail.getApproveComment();
    }
}
