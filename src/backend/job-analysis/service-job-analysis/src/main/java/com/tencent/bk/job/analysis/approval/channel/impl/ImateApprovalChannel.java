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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
 * <b>日志</b>：IMate 响应里的 approvalContent 含脚本明文，本类只打印任务 ID、状态与审批人，
 * 绝不打印响应体。注意底层 HttpHelper 在 DEBUG 级别会打印完整响应，生产环境不应对其开启 DEBUG。
 */
@Slf4j
@Component
@ConditionalOnExpression(
    // 与 Mock 渠道严格互斥：同一渠道注册两个实现会让回查目标不确定，Registry 会直接启动失败。
    // 这里用 equalsIgnoreCase 对齐 @ConditionalOnProperty 的 havingValue 语义，避免 "True" 这类写法两边都命中。
    // url 未配置即视为渠道未就绪，不注册本 Bean，带审批的接口会明确返回"渠道不可用"而不是运行期才失败
    "!'${job.analysis.approval.channels.imate.mock.enabled:false}'.equalsIgnoreCase('true') "
        + "&& !'${job.analysis.approval.channels.imate.url:}'.trim().isEmpty()"
)
public class ImateApprovalChannel implements ApprovalChannel {

    private static final String APPROVAL_DETAIL_URI = "/open/approval/detail";
    private static final String HEADER_APP_ID = "x-app-id";
    private static final String HEADER_SECRET = "x-secret";

    private static final String IMATE_STATUS_PENDING = "PENDING";
    private static final String IMATE_STATUS_APPROVED = "APPROVED";
    private static final String IMATE_STATUS_REJECTED = "REJECTED";
    private static final String IMATE_STATUS_EXPIRED = "EXPIRED";
    private static final String IMATE_STATUS_CANCELED = "CANCELED";

    private final ApprovalProperties approvalProperties;
    private final HttpHelper httpHelper;

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
        HttpResponse httpResponse = httpHelper.requestForSuccessResp(
            HttpRequest.builder(HttpMethodEnum.GET, buildDetailUrl(imateConfig.getUrl(), approvalTaskId))
                .setHeaders(headers)
                .build());
        ImateOpenApiResponse<ImateApprovalDetail> response = JsonUtils.fromJson(httpResponse.getEntity(),
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
