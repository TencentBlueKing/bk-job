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

package com.tencent.bk.job.execute.engine.consts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Consts {
    public static final Map<Integer, Integer> GSE_ERROR_CODE_2_STATUS_MAP;
    /**
     * 直连云区域ID
     */
    public static final int DEFAULT_CLOUD_ID = 0;

    static {
        Map<Integer, Integer> gseErrorCode2StatusMap = new HashMap<>();
        // 文件任务超时
        gseErrorCode2StatusMap.put(120, 303);
        // agent异常
        gseErrorCode2StatusMap.put(117, 310);
        // 用户名不存在
        gseErrorCode2StatusMap.put(122, 311);
        // 用户密码错误
        gseErrorCode2StatusMap.put(1326, 312);

        // 文件获取失败
        gseErrorCode2StatusMap.put(104, 320);
        gseErrorCode2StatusMap.put(123, 320);
        gseErrorCode2StatusMap.put(705, 320);
        gseErrorCode2StatusMap.put(706, 320);
        gseErrorCode2StatusMap.put(708, 320);
        gseErrorCode2StatusMap.put(709, 320);
        gseErrorCode2StatusMap.put(127, 320);

        // 文件超出限制
        gseErrorCode2StatusMap.put(119, 321);

        // 文件传输错误
        gseErrorCode2StatusMap.put(701, 329);
        gseErrorCode2StatusMap.put(702, 329);
        gseErrorCode2StatusMap.put(703, 329);
        gseErrorCode2StatusMap.put(704, 329);
        gseErrorCode2StatusMap.put(707, 329);
        gseErrorCode2StatusMap.put(710, 329);

        // 任务执行出错
        gseErrorCode2StatusMap.put(101, 399);
        gseErrorCode2StatusMap.put(102, 399);
        gseErrorCode2StatusMap.put(103, 399);
        gseErrorCode2StatusMap.put(105, 399);
        gseErrorCode2StatusMap.put(106, 399);
        gseErrorCode2StatusMap.put(107, 399);
        gseErrorCode2StatusMap.put(108, 399);
        gseErrorCode2StatusMap.put(109, 399);
        gseErrorCode2StatusMap.put(111, 399);
        gseErrorCode2StatusMap.put(112, 399);
        gseErrorCode2StatusMap.put(114, 399);
        gseErrorCode2StatusMap.put(118, 399);

        // 任务强制终止
        gseErrorCode2StatusMap.put(126, 403);
        GSE_ERROR_CODE_2_STATUS_MAP = Collections.unmodifiableMap(gseErrorCode2StatusMap);
    }

}
