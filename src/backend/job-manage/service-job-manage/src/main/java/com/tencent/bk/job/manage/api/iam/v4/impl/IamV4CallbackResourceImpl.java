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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.crontab.api.iam.v4.IamV4CronCallbackResource;
import com.tencent.bk.job.file_gateway.api.iam.v4.IamV4FileSourceCallbackResource;
import com.tencent.bk.job.manage.api.iam.v4.IamV4CallbackResource;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4CallbackResp;
import com.tencent.bk.sdk.iamv4.constants.IamV4Header;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 权限中心 v4 资源回调的统一入口实现：按请求体里的资源类型分派。
 *
 * <p>本地资源直接调对应的 {@link BaseIamV4CallbackService} 实现；{@code cron} 与 {@code file_source}
 * 的数据在各自微服务的库里，经内部接口转发到 job-crontab / job-file-gateway，转发时原样带上租户、
 * 认证头与 {@code X-Request-Id}，由对端自行完成回调 token 认证。</p>
 *
 * <p>本地处理器的注册表由 Spring 注入的全部 {@link BaseIamV4CallbackService} 自动构建，新增资源类型
 * 只要实现 {@link BaseIamV4CallbackService#getResourceType()} 即自动接入，无需改动本类。</p>
 *
 * <p>{@code type} 恒为**被查询的资源类型**，多实例视图不改变这一点：列举拓扑节点视图下某集群的模块，
 * 请求是 {@code type=module} + {@code filter.parent={set, x}}；视图首段（业务列表）是 {@code type=biz}
 * 且不带 parent。故按 {@code type} 单键分派即可，parent 只用于定位上级，由各资源实现自行解析。</p>
 *
 * <p>本地优先于转发，这让两种部署模式自然共用同一套代码：标准模式下 {@code cron} / {@code file_source}
 * 落到转发分支；轻量模式（job-assemble）下全部实现都在同一进程，均命中本地分支，不会绕出去做一次自我
 * HTTP 调用。</p>
 */
@Slf4j
@RestController
public class IamV4CallbackResourceImpl implements IamV4CallbackResource {

    /**
     * 资源类型 -> 本服务内的回调实现
     */
    private final Map<String, BaseIamV4CallbackService> localHandlers = new HashMap<>();
    /**
     * 资源类型 -> 需转发到其他微服务的调用
     */
    private final Map<String, BiFunction<CallbackContext, V4CallbackRequestDTO,
        ResponseEntity<V4CallbackResp<?>>>> remoteHandlers = new HashMap<>();

    @Autowired
    public IamV4CallbackResourceImpl(List<BaseIamV4CallbackService> callbackServices,
                                     IamV4CronCallbackResource cronCallbackResource,
                                     IamV4FileSourceCallbackResource fileSourceCallbackResource) {
        for (BaseIamV4CallbackService callbackService : callbackServices) {
            String resourceType = callbackService.getResourceType();
            BaseIamV4CallbackService previous = localHandlers.put(resourceType, callbackService);
            if (previous != null) {
                log.error("Duplicated IAM v4 callback handler for resource type {}|{} and {}",
                    resourceType, previous.getClass().getSimpleName(), callbackService.getClass().getSimpleName());
            }
        }
        remoteHandlers.put(IamV4ResourceTypeId.CRON, (ctx, request) -> cronCallbackResource.callback(
            ctx.tenantId, ctx.authorization, ctx.requestId, request));
        remoteHandlers.put(IamV4ResourceTypeId.FILE_SOURCE, (ctx, request) -> fileSourceCallbackResource.callback(
            ctx.tenantId, ctx.authorization, ctx.requestId, request));
        log.info("IAM v4 callback dispatcher initialized|local={}|remote={}",
            localHandlers.keySet(), remoteHandlers.keySet());
    }

    @Override
    public ResponseEntity<V4CallbackResp<?>> callback(String tenantId,
                                                      String authorization,
                                                      String requestId,
                                                      V4CallbackRequestDTO callbackRequest) {
        String resourceType = callbackRequest == null ? null : callbackRequest.getType();
        if (StringUtils.isBlank(resourceType)) {
            log.warn("Missing resource type in IAM v4 callback|requestId={}", requestId);
            return buildErrorResponse(requestId, "Field 'type' is required");
        }

        BaseIamV4CallbackService localHandler = localHandlers.get(resourceType);
        if (localHandler != null) {
            return localHandler.baseCallback(tenantId, authorization, requestId, callbackRequest);
        }

        BiFunction<CallbackContext, V4CallbackRequestDTO, ResponseEntity<V4CallbackResp<?>>> remoteHandler =
            remoteHandlers.get(resourceType);
        if (remoteHandler != null) {
            return remoteHandler.apply(
                new CallbackContext(tenantId, authorization, requestId), callbackRequest);
        }

        // 未注册的资源类型：不透传给任何实现，直接拒绝
        log.warn("Unknown resource type in IAM v4 callback|type={}|requestId={}", resourceType, requestId);
        return buildErrorResponse(requestId, "Unrecognized resource type");
    }

    private ResponseEntity<V4CallbackResp<?>> buildErrorResponse(String requestId, String message) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.BAD_REQUEST);
        if (StringUtils.isNotBlank(requestId)) {
            builder.header(IamV4Header.X_REQUEST_ID, requestId);
        }
        return builder.body(V4CallbackResp.fail("INVALID_REQUEST", message));
    }

    /**
     * 转发到其他微服务时需要原样带上的请求上下文
     */
    private static class CallbackContext {
        private final String tenantId;
        private final String authorization;
        private final String requestId;

        CallbackContext(String tenantId, String authorization, String requestId) {
            this.tenantId = tenantId;
            this.authorization = authorization;
            this.requestId = requestId;
        }
    }
}
