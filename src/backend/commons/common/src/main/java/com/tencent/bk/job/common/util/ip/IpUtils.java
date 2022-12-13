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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidIpv6Exception;
import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.Inet4Address;
import java.net.Inet6Address;
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
    public static final String PROTOCOL_IP_V4 = "v4";
    public static final String PROTOCOL_IP_V6 = "v6";
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
     * 从网卡获取首个机器IP，优先获取IPv4地址
     * 若获取到的值为v6协议的IP，默认提供完整无压缩的IPv6地址
     *
     * @return 首个机器IP地址
     */
    public static String getFirstMachineIP() {
        return getFirstMachineIpPreferV4();
    }

    private static String getFirstMachineIpPreferV4() {
        Map<String, String> ipv4Map = getMachineIPv4Map();
        if (!ipv4Map.isEmpty()) {
            return ipv4Map.values().iterator().next();
        }
        Map<String, String> ipv6Map = getMachineIPv6Map();
        if (!ipv6Map.isEmpty()) {
            String ipv6 = ipv6Map.values().iterator().next();
            /*
             * 此处处理原因详情可见Inet6Address.getHostAddress()方法说明。
             * Because link-local and site-local addresses are non-global, it is possible that
             * different hosts may have the same destination address and may be reachable through
             * different interfaces on the same originating system. In this case, the originating
             * system is said to be connected to multiple zones of the same scope. In order to
             * disambiguate which is the intended destination zone, it is possible to append a
             * zone identifier (or scope_id) to an IPv6 address.
             */
            if (ipv6.contains("%")) {
                ipv6 = ipv6.substring(0, ipv6.indexOf("%"));
            }
            return getFullIpv6ByCompressedOne(ipv6);
        }
        log.error("no available ip, plz check net interface");
        return null;
    }

    interface IpExtracter {
        String extractIpFromInetAddress(InetAddress inetAddress);
    }

    /**
     * 获取当前服务器IPv4地址
     *
     * @return Map<网卡名称 ， IP>
     */
    private static Map<String, String> getMachineIPv4Map() {
        return getMachineIP(inetAddress -> {
            if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                return inetAddress.getHostAddress();
            }
            return null;
        });
    }

    /**
     * 获取当前服务器IPv6地址
     *
     * @return Map<网卡名称 ， IP>
     */
    private static Map<String, String> getMachineIPv6Map() {
        return getMachineIP(inetAddress -> {
            if (inetAddress instanceof Inet6Address && !inetAddress.isLoopbackAddress()) {
                return inetAddress.getHostAddress();
            }
            return null;
        });
    }

    private static Map<String, String> getMachineIP(IpExtracter ipExtracter) {
        log.info("#####################Start getMachineIP");
        Map<String, String> allIp = new HashMap<>();

        try {
            // 获取服务器的所有网卡
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            if (null == allNetInterfaces) {
                log.error("#####################getMachineIP Can not get NetworkInterfaces");
                return allIp;
            }
            // 循环网卡获取网卡的IP地址
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = allNetInterfaces.nextElement();
                String netInterfaceName = netInterface.getName();
                // 过滤掉127.0.0.1的IP
                if (StringUtils.isBlank(netInterfaceName) || "lo".equalsIgnoreCase(netInterfaceName)) {
                    log.info("loopback address or net interface name is blank");
                    continue;
                }
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress inetAddress = addresses.nextElement();
                    String ip = ipExtracter.extractIpFromInetAddress(inetAddress);
                    if (StringUtils.isBlank(ip)) {
                        continue;
                    }
                    log.info("NetInterfaceName={}, The Machine IP={}", netInterfaceName, ip);
                    allIp.put(netInterfaceName, ip);
                }
            }
        } catch (Exception e) {
            log.error("Fail to get network interfaces", e);
        }
        return allIp;
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

    /**
     * 将纯IP与含云区域的IP分离开
     *
     * @param ipOrCloudIpList ip/cloudIp列表
     * @return <纯IP列表，含云区域IP列表>
     */
    public static Pair<List<String>, List<String>> separateIpAndCloudIps(List<String> ipOrCloudIpList) {
        List<String> ipList = new ArrayList<>();
        List<String> cloudIpList = new ArrayList<>();
        for (String ipOrCloudIp : ipOrCloudIpList) {
            if (ipOrCloudIp.contains(":")) {
                cloudIpList.add(ipOrCloudIp);
            } else {
                ipList.add(ipOrCloudIp);
            }
        }
        return Pair.of(ipList, cloudIpList);
    }

    /**
     * 根据IP推断IP协议
     *
     * @param ip ip地址
     * @return 协议号，常量值：PROTOCOL_IP_V6/PROTOCOL_IP_V4
     */
    public static String inferProtocolByIp(String ip) {
        if (IpUtils.checkIpv6(ip)) {
            return PROTOCOL_IP_V6;
        }
        return PROTOCOL_IP_V4;
    }

    /**
     * 有压缩的IPv6地址转换成完整无压缩的IPv6
     *
     * @param compressedIpv6 有压缩的IPv6地址
     * @return 完整无压缩的IPv6地址
     */
    public static String getFullIpv6ByCompressedOne(String compressedIpv6) {
        if (!checkIpv6(compressedIpv6)) {
            throw new InvalidIpv6Exception(ErrorCode.INVALID_IPV6_ADDRESS, new String[]{compressedIpv6});
        }
        String[] finalSeqArr = new String[]{"0000", "0000", "0000", "0000", "0000", "0000", "0000", "0000"};
        // 连续0标识符
        String continueZeroToken = "::";
        // 段之间分隔符
        String seqSeparator = ":";
        // 一个IPv6地址最多有8段
        int maxSeqNum = 8;
        // ::开头的IP首位补0便于后续统一分割处理
        if (compressedIpv6.startsWith(continueZeroToken)) {
            compressedIpv6 = "0" + compressedIpv6;
        }
        // ::结尾的IP末位补0便于后续统一分割处理
        if (compressedIpv6.endsWith(continueZeroToken)) {
            compressedIpv6 = compressedIpv6 + "0";
        }
        // 统一分割、解析
        if (compressedIpv6.contains(continueZeroToken)) {
            String[] seqArr = compressedIpv6.split(continueZeroToken);
            String[] leftSeqArr = seqArr[0].split(seqSeparator);
            for (int i = 0; i < leftSeqArr.length && i < maxSeqNum; i++) {
                finalSeqArr[i] = getStandardIpv6Seq(leftSeqArr[i]);
            }
            String[] rightSeqArr = seqArr[1].split(seqSeparator);
            for (int i = 0; i < rightSeqArr.length && i < maxSeqNum; i++) {
                finalSeqArr[i + maxSeqNum - rightSeqArr.length] = getStandardIpv6Seq(rightSeqArr[i]);
            }
        } else {
            String[] seqArr = compressedIpv6.split(seqSeparator);
            for (int i = 0; i < seqArr.length && i < maxSeqNum; i++) {
                finalSeqArr[i] = getStandardIpv6Seq(seqArr[i]);
            }
        }
        return StringUtils.join(finalSeqArr, ":");
    }

    /**
     * 获取含有4个字符的Ipv6标准段，不足4字符则添加前缀0
     *
     * @param ipv6Seq ipv6地址的一段
     * @return 4个字符的标准段
     */
    private static String getStandardIpv6Seq(String ipv6Seq) {
        // 兼容IPv4地址的最后一个地址段
        if (ipv6Seq.contains(".")) {
            return ipv6Seq;
        }
        String template = "0000";
        return template.substring(0, template.length() - ipv6Seq.length()) + ipv6Seq;
    }
}
