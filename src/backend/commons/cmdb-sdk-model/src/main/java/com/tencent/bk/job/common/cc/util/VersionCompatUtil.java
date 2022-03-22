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

package com.tencent.bk.job.common.cc.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class VersionCompatUtil {

    /*
     * 转换maintainers，对接CC3.0之后maintainer用","分隔，CC2.0使用";"分隔；需要兼容
     */
    public static String convertMaintainers(String maintainers) {
        if (StringUtils.isBlank(maintainers)) {
            return "";
        }
        String[] maintainerArray = maintainers.split("[,;]");
        StringBuilder sb = new StringBuilder();
        for (String maintainer : maintainerArray) {
            if (StringUtils.isNotBlank(maintainer)) {
                sb.append(maintainer).append(";");
            } else {
                log.info("ignore an empty maintainer:{}", maintainer);
            }
        }
        String newMaintainers = sb.toString();
        newMaintainers = newMaintainers.substring(0, newMaintainers.length() - 1);
        return newMaintainers;
    }
}
