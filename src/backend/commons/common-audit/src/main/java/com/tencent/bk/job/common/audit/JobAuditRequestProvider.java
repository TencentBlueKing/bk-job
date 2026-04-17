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

package com.tencent.bk.job.common.audit;

import com.tencent.bk.audit.AuditRequestProvider;
import com.tencent.bk.audit.constants.AccessTypeEnum;
import com.tencent.bk.audit.constants.UserIdentifyTypeEnum;
import com.tencent.bk.audit.exception.AuditException;
import com.tencent.bk.audit.model.AuditHttpRequest;
import com.tencent.bk.job.common.util.JobContextUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class JobAuditRequestProvider implements AuditRequestProvider {

    public AuditHttpRequest getRequest() {
        HttpServletRequest httpServletRequest = this.getHttpServletRequest();
        return new AuditHttpRequest(httpServletRequest);
    }

    private HttpServletRequest getHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            log.error("Could not get RequestAttributes from RequestContext!");
            throw new AuditException("Parse http request error");
        } else {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
    }

    @Override
    public String getUsername() {
        return JobContextUtil.getUserDisplayName();
    }

    @Override
    public UserIdentifyTypeEnum getUserIdentifyType() {
        // 当前只支持个人账户
        return UserIdentifyTypeEnum.PERSONAL;
    }

    @Override
    public String getUserIdentifyTenantId() {
        return JobContextUtil.getTenantId();
    }

    @Override
    public String getRequestId() {
        return JobContextUtil.getRequestId();
    }

    @Override
    public AccessTypeEnum getAccessType() {
        AuditHttpRequest request = getRequest();
        String uri = request.getUri();
        if (uri.startsWith("/web/")) {
            return AccessTypeEnum.WEB;
        } else if (uri.startsWith("/esb/")) {
            return AccessTypeEnum.API;
        } else {
            return AccessTypeEnum.OTHER;
        }
    }

    /**
     * 获取客户端IP。
     * <p>
     * 注意：X-Forwarded-For 头可被客户端伪造，此方法仅适用于服务部署在可信反向代理/网关后面的场景。
     * 若需要更严格的IP解析，请自行实现 {@link AuditRequestProvider} 接口，
     * 结合受信任代理列表或使用 Spring 的 ForwardedHeaderFilter。
     */
    @Override
    public String getClientIp() {
        HttpServletRequest request = getHttpServletRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isEmpty()) {
            return request.getRemoteAddr();
        }
        // 取第一个IP并去除空格
        String clientIp = xff.contains(",") ? xff.split(",")[0].trim() : xff.trim();
        return clientIp.isEmpty() ? request.getRemoteAddr() : clientIp;
    }

    @Override
    public String getUserAgent() {
        HttpServletRequest request = getHttpServletRequest();
        return request.getHeader("User-Agent");
    }

}
