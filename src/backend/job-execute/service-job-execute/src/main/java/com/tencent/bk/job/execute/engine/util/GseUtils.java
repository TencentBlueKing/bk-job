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

import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.consts.Consts;

/**
 * 处理 GSE 工具类
 */
public class GseUtils {
    /**
     * 字节转换的进制
     */
    private static final int SIZE_UNIT = 1024;

    /**
     * 将字节转换为 human readable format
     *
     * @param size 大小(单位Byte)
     * @return 便于阅读的大小表示
     */
    public static String tranByteReadable(double size) {
        if (size < SIZE_UNIT) {
            return size + " Bytes";
        }

        size = changeUnit(size);
        if (size < SIZE_UNIT) {
            return size + " KB";
        }

        size = changeUnit(size);
        if (size < SIZE_UNIT) {
            return size + " MB";
        }

        return changeUnit(size) + " GB";
    }

    /**
     * 转换单位
     */
    private static double changeUnit(double size) {
        return (double) Math.round(size / SIZE_UNIT * 1000) / 1000;
    }

    /**
     * 根据gse ErrorCode返回 Status
     */
    public static AgentTaskStatusEnum getStatusByGseErrorCode(int gseErrorCode) {
        AgentTaskStatusEnum status = Consts.GSE_ERROR_CODE_2_STATUS_MAP.get(gseErrorCode);
        if (status == null) {
            if (gseErrorCode > 0) {
                status = AgentTaskStatusEnum.UNKNOWN_ERROR;
            } else {
                status = AgentTaskStatusEnum.SUCCESS;
            }
        }
        return status;
    }
}
