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

import java.util.HashSet;
import java.util.Set;

public class WindowsHelper {
    private static final Set<String> WINDOWS_DISK_PARTITION_NAMES = new HashSet<>();

    static {
        WINDOWS_DISK_PARTITION_NAMES.add("C");
        WINDOWS_DISK_PARTITION_NAMES.add("c");
        WINDOWS_DISK_PARTITION_NAMES.add("D");
        WINDOWS_DISK_PARTITION_NAMES.add("d");
        WINDOWS_DISK_PARTITION_NAMES.add("E");
        WINDOWS_DISK_PARTITION_NAMES.add("e");
        WINDOWS_DISK_PARTITION_NAMES.add("F");
        WINDOWS_DISK_PARTITION_NAMES.add("f");
    }

    public static boolean isWindowsDiskPartition(String diskPartitionName) {
        return WINDOWS_DISK_PARTITION_NAMES.contains(diskPartitionName);
    }
}
