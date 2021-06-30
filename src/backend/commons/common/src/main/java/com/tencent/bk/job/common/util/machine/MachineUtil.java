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

package com.tencent.bk.job.common.util.machine;

import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MachineUtil {
    private static final OperatingSystemMXBean osmxb =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final List<Float> systemCPULoadList = new ArrayList<>(100);
    private static final List<Float> processCPULoadList = new ArrayList<>(100);

    static {
        new CPUWatchThread().start();
    }

    /**
     * 获取某个目录下可用磁盘空间
     *
     * @param path
     * @return 字节数
     * @throws FileNotFoundException
     */
    public static long getDiskFreeSpace(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path + " not found");
        }
        return file.getFreeSpace();
    }

    /**
     * 获取某个目录下可用磁盘空间
     *
     * @param path
     * @return 字节数
     * @throws FileNotFoundException
     */
    public static long getDiskUsableSpace(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path + " not found");
        }
        return file.getUsableSpace();
    }

    /**
     * 获取某个目录下磁盘总空间
     *
     * @param path
     * @return 字节数
     * @throws FileNotFoundException
     */
    public static long getDiskTotalSpace(String path) throws FileNotFoundException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(path + " not found");
        }
        return file.getTotalSpace();
    }

    /**
     * 获取磁盘负载
     *
     * @param path
     * @return 0~1, double
     * @throws FileNotFoundException
     */
    public static float getDiskLoad(String path) throws FileNotFoundException {
        long totalSpace = getDiskTotalSpace(path);
        long freeSpace = getDiskFreeSpace(path);
        return (totalSpace - freeSpace) / (float) totalSpace;
    }

    /**
     * 系统CPU负载
     *
     * @return 0~1 double
     */
    public static float systemCPULoad() {
        if (systemCPULoadList.isEmpty()) {
            return (float) osmxb.getSystemCpuLoad();
        }
        return averageCPULoad(systemCPULoadList);
    }

    /**
     * 进程CPU负载
     *
     * @return 0~1 double
     */
    public static float processCPULoad() {
        if (processCPULoadList.isEmpty()) {
            return (float) osmxb.getProcessCpuLoad();
        }
        return averageCPULoad(processCPULoadList);
    }

    private static float averageCPULoad(List<Float> cpuLoadList) {
        float cpuLoad = 0;
        for (float load : cpuLoadList) {
            cpuLoad += load;
        }
        return cpuLoad / cpuLoadList.size();
    }

    /**
     * 物理内存总量，字节数
     *
     * @return
     */
    public static long totalPhysicalMemorySize() {
        return osmxb.getTotalPhysicalMemorySize();
    }

    /**
     * 空闲内存总量，字节数
     *
     * @return
     */
    public static long freePhysicalMemorySize() {
        return osmxb.getFreePhysicalMemorySize();
    }

    /**
     * 内存使用率
     *
     * @return 0~1, double
     */
    public static float memoryLoad() {
        long totalPhysicalMemorySize = totalPhysicalMemorySize();
        long freePhysicalMemorySize = freePhysicalMemorySize();
        return (totalPhysicalMemorySize - freePhysicalMemorySize) / (float) totalPhysicalMemorySize;
    }

    /**
     * 每秒统计100次系统与进程CPU负载，计算均值
     */
    static class CPUWatchThread extends Thread {
        @Override
        public void run() {
            while (true) {
                // 检测系统CPU占用
                double systemCpuLoad = osmxb.getSystemCpuLoad();
                if (systemCPULoadList.size() > 99) {
                    systemCPULoadList.remove(0);
                }
                systemCPULoadList.add((float) systemCpuLoad);
                // 检测进程CPU占用
                double processCpuLoad = osmxb.getProcessCpuLoad();
                if (processCPULoadList.size() > 99) {
                    processCPULoadList.remove(0);
                }
                processCPULoadList.add((float) processCpuLoad);
                // 每秒统计100次
                try {
                    sleep(10);
                } catch (InterruptedException e) {
                    log.error("sleep interrupted", e);
                }
            }
        }
    }

}
