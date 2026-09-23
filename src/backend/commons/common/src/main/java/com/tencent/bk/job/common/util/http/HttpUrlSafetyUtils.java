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

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * 用户可控 URL 的安全解析与地址判定，用于 SSRF 防护。
 */
public final class HttpUrlSafetyUtils {

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
     * 环回、内网、链路本地、通配、组播、IPv6 ULA。
     */
    public static boolean isInternalAddress(InetAddress address) {
        if (address == null) {
            return true;
        }
        if (isDangerousAddress(address) || address.isSiteLocalAddress()) {
            return true;
        }
        return isIpv6UniqueLocalAddress(address);
    }

    /**
     * 几乎不可能作为合法第三方服务入口的地址：环回、链路本地、通配、组播、IPv6 ULA。
     * 不含站点本地地址，避免误伤私有化部署。
     */
    public static boolean isDangerousAddress(InetAddress address) {
        if (address == null) {
            return true;
        }
        if (address.isAnyLocalAddress()
            || address.isLoopbackAddress()
            || address.isLinkLocalAddress()
            || address.isMulticastAddress()) {
            return true;
        }
        return isIpv6UniqueLocalAddress(address);
    }

    public static boolean isResolvedToInternalAddress(String host, HostResolver hostResolver) {
        return isResolvedTo(host, hostResolver, true);
    }

    public static boolean isResolvedToDangerousAddress(String host, HostResolver hostResolver) {
        return isResolvedTo(host, hostResolver, false);
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

    private static boolean isResolvedTo(String host, HostResolver hostResolver, boolean includeSiteLocal) {
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
            if (includeSiteLocal) {
                if (isInternalAddress(address)) {
                    return true;
                }
            } else if (isDangerousAddress(address)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIpv6UniqueLocalAddress(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes != null && bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
    }
}
