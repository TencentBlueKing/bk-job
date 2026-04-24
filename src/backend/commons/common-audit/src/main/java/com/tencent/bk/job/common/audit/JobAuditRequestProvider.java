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
import com.tencent.bk.job.common.util.LogUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class JobAuditRequestProvider implements AuditRequestProvider {

    /**
     * User-Agent 最大长度，超出部分截断。
     * 常见浏览器 UA 通常在 100~300 字符；512 足以覆盖绝大多数合法场景。
     */
    private static final int MAX_USER_AGENT_LENGTH = 512;

    /**
     * 日志中打印不可信外部输入时的最大长度
     */
    private static final int MAX_LOG_VALUE_LENGTH = 64;

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
     * 获取客户端 IP。
     * <p>
     * 信任模型：后端微服务始终部署在 CLB → Ingress → job-gateway 的可信代理链之后，
     * 真实客户端 IP 只能由第一跳可信网关（CLB）通过 TCP 连接获取并写入 X-Forwarded-For，
     * 后续各层代理追加自身地址。因此取 X-Forwarded-For 第一个 IP 即为客户端真实 IP。
     * <p>
     * 防伪造措施：对提取出的 IP 进行格式校验（IPv4/IPv6），若不合法则回退到 remoteAddr，
     * 防止客户端通过构造恶意 X-Forwarded-For 值注入非 IP 内容。
     */
    @Override
    public String getClientIp() {
        HttpServletRequest request = getHttpServletRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isEmpty()) {
            return request.getRemoteAddr();
        }
        String candidate = xff.contains(",") ? xff.substring(0, xff.indexOf(',')).trim() : xff.trim();
        if (candidate.isEmpty() || !IpUtils.isValidIpAddress(candidate)) {
            log.warn("Invalid client IP from X-Forwarded-For: {}, fallback to remoteAddr",
                LogUtil.sanitizeForLog(candidate, MAX_LOG_VALUE_LENGTH));
            return request.getRemoteAddr();
        }
        return candidate;
    }

    /**
     * 获取 User-Agent，做长度截断与控制字符清理，防止恶意超长值或日志注入。
     */
    @Override
    public String getUserAgent() {
        HttpServletRequest request = getHttpServletRequest();
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.isEmpty()) {
            return "";
        }
        if (ua.length() > MAX_USER_AGENT_LENGTH) {
            ua = ua.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return LogUtil.stripControlChars(ua);
    }

}
