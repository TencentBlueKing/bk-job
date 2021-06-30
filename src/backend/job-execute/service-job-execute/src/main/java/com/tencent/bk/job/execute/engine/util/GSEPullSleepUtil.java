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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * GSE拉取日志超时时间机制
 */
public final class GSEPullSleepUtil {

    private static final Logger log = LoggerFactory.getLogger(GSEPullSleepUtil.class);

    /**
     * GSE拉取日志睡眠时间, 格式如：“10,20|5,10,60”，竖线左边是引起睡眠时间切换的拉取次数；竖线右边是睡眠时间，单位秒。如果竖线两边的数字个数不映衬，那么多余的数字会被忽略。
     */
    private final static String gsePullSleep = "100,200,320,830|1,2,5,10,60";


    /**
     * 拉取次数间隔的默认值
     */
    private static int[] pullArr = new int[]{20, 50};

    /**
     * 睡眠时间间隔的默认值
     */
    private static int[] sleepArr = new int[]{3, 5, 60};

    /**
     * 初始化
     */
    static {
        try {
            String[] gsePullSleepArr = gsePullSleep.split("\\|");
            String[] pullStrArr = gsePullSleepArr[0].split(",");
            String[] sleepStrArr = gsePullSleepArr[1].split(",");

            pullArr = strArrToIntArr(pullStrArr);
            sleepArr = strArrToIntArr(sleepStrArr);

            Arrays.sort(pullArr);
            Arrays.sort(sleepArr);

        } catch (Exception e) {
            log.warn("gse.pull.sleep config error", e);
        }

    }

    /**
     * 获取拉取日志的超时时间，单位秒
     *
     * @param allPullTimes 已拉取次数
     * @return 拉取日志的超时时间，单位秒
     */
    public static int getTime(int allPullTimes) {

        for (int i = 0; i < pullArr.length; i++) {
            if (allPullTimes <= pullArr[i]) {
                if (i < sleepArr.length) {
                    return sleepArr[i];
                } else {
                    return sleepArr[sleepArr.length - 1];
                }
            }
        }

        // 最大的拉取时间
        if (pullArr.length < sleepArr.length) {
            return sleepArr[pullArr.length];
        } else {
            return sleepArr[sleepArr.length - 1];
        }
    }

    /**
     * 字符串数组转整形数组
     *
     * @param strArr 字符串数组
     * @return 整形数组
     */
    private static int[] strArrToIntArr(String[] strArr) {
        int[] intArr = new int[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            intArr[i] = Integer.parseInt(strArr[i]);
        }
        return intArr;
    }
}
