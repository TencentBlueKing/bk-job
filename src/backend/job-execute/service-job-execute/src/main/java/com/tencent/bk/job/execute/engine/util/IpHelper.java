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

package com.tencent.bk.job.execute.engine.util;

import com.tencent.bk.job.common.model.dto.HostDTO;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 说明：
 * 子网id原1是直连的，现在0是直连的，1逐渐作废掉转为0,未来禁止使用
 * 非0和1的都不是直连区域。
 */
public class IpHelper {

    private static final String SI_SPLIT = ":";
    private static final long DEFAULT_CLOUD_ID = 0; // 子网id原1是直连的，现在0是直连的，1逐渐作废掉转为0,未来禁止使用

    /**
     * 将 原子网id为1的修正为0
     *
     * @param sourceAndIp 可以为 1:127.0.0.1 或127.0.0.1 会转换成 0:127.0.0.1
     * @return 返回修正后的 云区域id:IP
     */
    public static String fix1To0(String sourceAndIp) {
        return compose(transform(sourceAndIp));
    }

    public static HostDTO transform(String sourceAndIp) {
        HostDTO hostDTO = null;
        if (sourceAndIp != null) {
            String[] split = sourceAndIp.split(SI_SPLIT);
            if (split.length == 2) {
                long source = optLong(split[0].trim(), DEFAULT_CLOUD_ID);
                if (source == 1) source = DEFAULT_CLOUD_ID;
                hostDTO = new HostDTO(source, split[1].trim());
            } else {
                hostDTO = new HostDTO(DEFAULT_CLOUD_ID, sourceAndIp.trim());
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

    public static String compose(HostDTO hostDTO) {
        if (hostDTO == null) {
            return null;
        }
        return compose(hostDTO.getBkCloudId(), hostDTO.getIp());
    }

    public static String compose(Long cloudId, String ip) {
        if (ip == null) {
            return null;
        }
        // 子网id原1是直连的，现在0是直连的，1逐渐作废掉转为0,未来禁止使用
        if (cloudId == null || cloudId <= 1) {
            cloudId = DEFAULT_CLOUD_ID;
        }
        return cloudId + SI_SPLIT + ip.trim();
    }
}
