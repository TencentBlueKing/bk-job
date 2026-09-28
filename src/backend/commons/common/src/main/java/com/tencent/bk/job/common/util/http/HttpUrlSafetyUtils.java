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

package com.tencent.bk.job.common.util.http;

import com.tencent.bk.job.common.util.LogUtil;
import org.apache.commons.lang3.StringUtils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * URL 安全解析与地址判定。
 * <p>
 * 用户可控 URL（制品库 / COS / 回调等）走 {@link #parseHttpUrlHost(String)}、
 * {@link #isResolvedToLocalHostAddress(String, HostResolver)} 等接口，解析结果只拒绝环回与通配地址。
 * 内部服务互调（file-gateway ↔ file-worker）走 {@link #parseSafeInternalHttpUri(String)}，
 * 允许 K8s Service/Pod DNS 与集群/回环 IP，只拒绝链路本地、通配与组播。
 */
public final class HttpUrlSafetyUtils {

    private static final Pattern DNS_1123_HOST = Pattern.compile(
        "(?i)^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)*$"
    );
    private static final Pattern IPV4_LITERAL = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3}");

    private HttpUrlSafetyUtils() {
    }

    @FunctionalInterface
    public interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    public static final HostResolver DEFAULT_HOST_RESOLVER = InetAddress::getAllByName;

    /**
     * 解析 http/https URL 的 host，非法或非 http(s) 时返回 null。
     */
    public static String parseHttpUrlHost(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        URL parsedUrl;
        try {
            parsedUrl = new URL(url.trim());
        } catch (MalformedURLException e) {
            return null;
        }
        String protocol = parsedUrl.getProtocol();
        if (!"http".equalsIgnoreCase(protocol) && !"https".equalsIgnoreCase(protocol)) {
            return null;
        }
        String host = parsedUrl.getHost();
        if (StringUtils.isBlank(host)) {
            return null;
        }
        return host.toLowerCase(Locale.ROOT);
    }

    /**
     * 解析 http/https URL，或无 scheme 的纯 host[:port]。
     */
    public static String parseHttpUrlOrBareHost(String endpoint) {
        if (StringUtils.isBlank(endpoint)) {
            return null;
        }
        String trimmed = endpoint.trim();
        if (trimmed.contains("://")) {
            return parseHttpUrlHost(trimmed);
        }
        if (trimmed.contains("/") || trimmed.contains("?") || trimmed.contains("#") || trimmed.contains(" ")) {
            return null;
        }
        try {
            URI uri = new URI("http://" + trimmed);
            String host = uri.getHost();
            if (StringUtils.isBlank(host)) {
                return null;
            }
            return host.toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * host 是否等于 parentHost，或按 DNS 标签层级判断为 parentHost 的子域。
     */
    public static boolean isHostOrChildHost(String host, String parentHost) {
        if (StringUtils.isBlank(host) || StringUtils.isBlank(parentHost)) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        String normalizedParent = parentHost.toLowerCase(Locale.ROOT);
        if (normalizedHost.equals(normalizedParent)) {
            return true;
        }
        String[] hostLabels = normalizedHost.split("\\.", -1);
        String[] parentLabels = normalizedParent.split("\\.", -1);
        if (hostLabels.length <= parentLabels.length) {
            return false;
        }
        int offset = hostLabels.length - parentLabels.length;
        for (int i = 0; i < parentLabels.length; i++) {
            if (!parentLabels[i].equals(hostLabels[offset + i])) {
                return false;
            }
        }
        for (int i = 0; i < offset; i++) {
            if (hostLabels[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析结果是否包含指向本机的地址：通配地址或环回地址。解析失败、空结果按失败关闭视为命中。
     * <p>
     * 只拦通配与环回，站点本地、链路本地、组播、IPv6 ULA 等地址均放行，
     * 用于当前环境域名匹配、COS 接入点等场景，避免误伤私有化部署与云上内网访问地址。
     */
    public static boolean isResolvedToLocalHostAddress(String host, HostResolver hostResolver) {
        if (StringUtils.isBlank(host)) {
            return true;
        }
        HostResolver resolver = hostResolver == null ? DEFAULT_HOST_RESOLVER : hostResolver;
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException e) {
            return true;
        }
        if (addresses == null || addresses.length == 0) {
            return true;
        }
        for (InetAddress address : addresses) {
            if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 白名单根地址格式：http(s)、必须有 host、禁止 userinfo/query/fragment。
     */
    public static boolean isValidWhitelistHttpBaseUrl(String baseUrl) {
        if (StringUtils.isBlank(baseUrl)
            || !(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
            return false;
        }
        URI uri;
        try {
            uri = new URI(baseUrl.trim());
        } catch (URISyntaxException e) {
            return false;
        }
        if (StringUtils.isBlank(uri.getHost())) {
            return false;
        }
        return uri.getRawUserInfo() == null
            && StringUtils.isEmpty(uri.getRawQuery())
            && StringUtils.isEmpty(uri.getRawFragment());
    }

    /**
     * 打日志用：去掉 userinfo 后截断控制字符，避免凭据泄露和日志注入。
     */
    public static String toLogSafeUrl(String url, int maxLength) {
        if (url == null) {
            return "";
        }
        return LogUtil.sanitizeForLog(stripUserInfo(url), maxLength);
    }

    /**
     * 去掉 URL 或裸 host 形式（如 {@code user:pwd@host:port}）中的 userinfo。
     * <p>
     * 能按标准 URI 解析出 host 时重建 URI，结果与原串仅差 userinfo；
     * 解析失败或无法识别 host 时，丢弃 query/fragment 之前最后一个 {@code @} 及其之前的认证部分，保证不回显凭据。
     */
    public static String stripUserInfo(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }
        try {
            URI uri = new URI(url);
            if (uri.getHost() != null) {
                if (uri.getRawUserInfo() == null) {
                    return url;
                }
                return new URI(
                    uri.getScheme(),
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
                ).toString();
            }
        } catch (URISyntaxException ignored) {
            // 按字符串兜底处理
        }
        return stripUserInfoByAt(url);
    }

    private static String stripUserInfoByAt(String url) {
        int schemeSeparator = url.indexOf("://");
        int authorityStart = schemeSeparator < 0 ? 0 : schemeSeparator + 3;
        int authorityEnd = url.length();
        for (int i = authorityStart; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '?' || c == '#') {
                authorityEnd = i;
                break;
            }
        }
        int at = url.lastIndexOf('@', authorityEnd - 1);
        if (at < authorityStart) {
            return url;
        }
        return url.substring(0, authorityStart) + url.substring(at + 1);
    }

    /**
     * 将内部 HTTP 目标解析为绝对 URI；不合法时返回 null。
     */
    public static URI parseSafeInternalHttpUri(String url) {
        return parseSafeInternalHttpUri(url, DEFAULT_HOST_RESOLVER);
    }

    public static URI parseSafeInternalHttpUri(String url, HostResolver resolver) {
        if (StringUtils.isBlank(url) || resolver == null) {
            return null;
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
        if (!uri.isAbsolute()) {
            return null;
        }
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return null;
        }
        if (StringUtils.isNotEmpty(uri.getRawUserInfo())) {
            return null;
        }
        String host = uri.getHost();
        if (!isAllowedServiceHost(host)) {
            return null;
        }
        int port = uri.getPort();
        if (port == 0 || port < -1 || port > 65535) {
            return null;
        }
        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(host);
        } catch (UnknownHostException e) {
            return null;
        }
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        for (InetAddress address : addresses) {
            if (isBlockedInternalHttpTarget(address)) {
                return null;
            }
        }
        return uri;
    }

    /**
     * 拼进 {@code http://host:port/...} 前校验 host：只允许 IP 或 DNS-1123 主机名，禁止 path/userinfo 注入。
     */
    public static boolean isAllowedServiceHost(String host) {
        if (StringUtils.isBlank(host)) {
            return false;
        }
        String normalized = stripIpv6Brackets(host.trim());
        if (containsForbiddenHostChars(normalized)) {
            return false;
        }
        if (isIpv4Literal(normalized) || isIpv6Literal(normalized)) {
            return true;
        }
        if (normalized.length() > 253) {
            return false;
        }
        return DNS_1123_HOST.matcher(normalized).matches();
    }

    /**
     * 把裸 IPv6 包成 URL host 形态。
     */
    public static String hostForUrl(String host) {
        if (StringUtils.isBlank(host)) {
            return host;
        }
        String trimmed = host.trim();
        if (trimmed.startsWith("[")) {
            return trimmed;
        }
        if (isIpv6Literal(trimmed)) {
            return "[" + trimmed + "]";
        }
        return trimmed;
    }

    /**
     * 拒绝链路本地（含云 metadata）、通配与组播。回环与站点本地对内部 Worker 调用是合法目标。
     */
    public static boolean isBlockedInternalHttpTarget(InetAddress address) {
        if (address == null) {
            return true;
        }
        return address.isAnyLocalAddress()
            || address.isLinkLocalAddress()
            || address.isMulticastAddress();
    }

    private static String stripIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static boolean containsForbiddenHostChars(String host) {
        return host.indexOf('/') >= 0
            || host.indexOf('?') >= 0
            || host.indexOf('#') >= 0
            || host.indexOf('@') >= 0
            || host.indexOf(' ') >= 0
            || host.indexOf('\\') >= 0;
    }

    private static boolean isIpv4Literal(String host) {
        if (!IPV4_LITERAL.matcher(host).matches()) {
            return false;
        }
        try {
            return InetAddress.getByName(host) instanceof Inet4Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isIpv6Literal(String host) {
        if (host.indexOf(':') < 0) {
            return false;
        }
        try {
            return InetAddress.getByName(host) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
