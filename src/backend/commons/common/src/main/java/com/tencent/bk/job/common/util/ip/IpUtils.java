/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.util.ip;

import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作业平台服务器 IP 工具类
 */
@Slf4j
public class IpUtils {
    public static final String COLON = ":";

    /**
     * 直连云区域
     */
    public static final long DEFAULT_CLOUD_ID = 0;
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.(" +
            "(?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");
    private static final Pattern pattern = Pattern.compile(
        "\\b((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.(" +
            "(?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\.((?!\\d\\d\\d)\\d+|1\\d\\d|2[0-4]\\d|25[0-5])\\b");

    /**
     * 校验云区域:服务器IP
     *
     * @param cloudIp 云区域ID:服务器IP
     */
    public static boolean checkCloudIp(String cloudIp) {
        if (StringUtils.isEmpty(cloudIp)) {
            return false;
        }
        String[] ipProps = cloudIp.split(":");
        if (ipProps.length != 2) {
            log.warn("Both cloudAreaId and ip is required, ip={}", cloudIp);
            return false;
        }
        try {
            Integer.parseInt(ipProps[0]);
        } catch (NumberFormatException e) {
            log.warn("CloudAreaId is illegal for ip:{}", cloudIp);
            return false;
        }
        String ip = ipProps[1];
        Matcher matcher = IP_PATTERN.matcher(ip.trim());
        return matcher.matches();
    }

    /**
     * 验证ip格式（不包含云区域ID)
     *
     * @param ipStr ip
     */
    public static boolean checkIp(String ipStr) {
        if (StringUtils.isEmpty(ipStr.trim())) {
            return false;
        }
        Matcher matcher = IP_PATTERN.matcher(ipStr);
        return matcher.matches();
    }

    /**
     * 验证ipv6格式
     *
     * @param ipv6Str ipv6字符串
     */
    public static boolean checkIpv6(String ipv6Str) {
        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet6Address(ipv6Str);
    }

    /**
     * 验证ipv4格式
     *
     * @param ipv4Str ipv4字符串
     */
    public static boolean checkIpv4(String ipv4Str) {
        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet4Address(ipv4Str);
    }

    /**
     * 转换到IpDTO
     *
     * @param cloudIp 云区域+IP
     */
    public static HostDTO transform(String cloudIp) {
        HostDTO hostDTO = null;
        if (cloudIp != null) {
            String[] split = cloudIp.split(COLON);
            if (split.length == 2) {
                long cloudAreaId = optLong(split[0].trim(), DEFAULT_CLOUD_ID);
                if (cloudAreaId == 1)
                    cloudAreaId = DEFAULT_CLOUD_ID;
                hostDTO = new HostDTO(cloudAreaId, split[1].trim());
            } else {
                hostDTO = new HostDTO(DEFAULT_CLOUD_ID, cloudIp.trim());
            }
        }
        return hostDTO;
    }

    private static long optLong(String str, long defaultValue) {
        if (NumberUtils.isDigits(str))
            try {
                return Long.parseLong(str);
            } catch (Exception e) {
                return defaultValue;
            }
        return defaultValue;
    }

    /**
     * 字符串ip转换为long
     *
     * @return ip
     */
    public static long getStringIpToLong(String ip) {
        long num = 0;
        if (ip != null && compileIP(ip)) {
            String[] ips = ip.split("\\.");
            long[] vals = new long[4];
            for (int i = 0; i < ips.length; i++) {
                vals[i] = Long.parseLong(ips[i]);
            }
            num = (vals[0] << 24) + (vals[1] << 16) + (vals[2] << 8) + vals[3];
        }
        return num;
    }

    /**
     * 将数值转换为ip
     *
     * @return ip
     */
    public static String revertIpFromLong(long ipLong) {
        long y = ipLong % 256;
        long m = (ipLong - y) / (256 * 256 * 256);
        long n = (ipLong - 256 * 256 * 256 * m - y) / (256 * 256);
        long x = (ipLong - 256 * 256 * 256 * m - 256 * 256 * n - y) / 256;
        return m + "." + n + "." + x + "." + y;
    }

    /**
     * 将数值转换为ip
     *
     * @return ip
     */
    public static String revertIpFromNumericalStr(String numericalStr) {
        try {
            InetAddress addr = InetAddress.getByName(numericalStr);
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            return "";
        }
    }


    public static boolean compileIP(String ip) {
        Matcher matcher = pattern.matcher(ip);
        return matcher.matches();
    }

    /**
     * 获取当前服务器IP地址
     *
     * @return 返回当前服务器的网卡对应IP的MAP。
     */
    public static Map<String, String> getMachineIP() {
        log.info("#####################Start getMachineIP");
        Map<String, String> allIp = new HashMap<>();

        try {
            // 获取服务器的所有网卡
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            if (null == allNetInterfaces) {
                log.error("#####################getMachineIP Can not get NetworkInterfaces");
            } else {
                // 循环网卡获取网卡的IP地址
                while (allNetInterfaces.hasMoreElements()) {
                    NetworkInterface netInterface = allNetInterfaces.nextElement();
                    String netInterfaceName = netInterface.getName();
                    // 过滤掉127.0.0.1的IP
                    if (StringUtils.isBlank(netInterfaceName) || "lo".equalsIgnoreCase(netInterfaceName)) {
                        log.info("loopback地址或网卡名称为空");
                    } else {
                        Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress ip = addresses.nextElement();
                            if (ip instanceof Inet4Address && !ip.isLoopbackAddress()) {
                                String machineIp = ip.getHostAddress();
                                log.info("###############" + "netInterfaceName=" + netInterfaceName
                                    + " The Macheine IP=" + machineIp);
                                allIp.put(netInterfaceName, machineIp);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取网卡失败", e);
        }

        return allIp;
    }

    public static String getFirstMachineIP() {
        String ip = getMachineIP().values().iterator().next();
        if (ip == null) {
            ip = "no available ip";
        }
        return ip;
    }

    /**
     * 通过含多个IP的字符串与云区域ID构造多个cloudIp
     *
     * @param cloudId    云区域ID
     * @param multiIpStr 含多个IP的字符串
     * @return cloudIp列表
     */
    public static List<String> buildCloudIpListByMultiIp(Long cloudId, String multiIpStr) {
        if (StringUtils.isBlank(multiIpStr)) {
            return Collections.emptyList();
        }
        if (!multiIpStr.contains(",") && !multiIpStr.contains(":")) {
            return Collections.singletonList(cloudId + ":" + multiIpStr.trim());
        }
        String[] ipArr = multiIpStr.split("[,;]");
        List<String> cloudIpList = new ArrayList<>(ipArr.length);
        for (String ip : ipArr) {
            cloudIpList.add(cloudId + ":" + ip.trim());
        }
        return cloudIpList;
    }

    /**
     * 移除ip中的云区域ID
     *
     * @param ip bkCloudId:ip
     * @return 移除云区域ID后的ip
     */
    public static String removeBkCloudId(String ip) {
        if (ip == null) {
            return null;
        }
        if (ip.contains(":")) {
            return ip.substring(ip.indexOf(":") + 1);
        } else {
            return ip;
        }
    }
}
