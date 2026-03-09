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

package com.tencent.bk.job.common.i18n.zone;

import java.time.ZoneId;

/**
 * 时区常量
 */
public class TimeZoneConstants {

    /**
     * 默认展示时区配置属性key
     */
    public static final String KEY_DISPLAY_TIMEZONE_CONFIG = "display";

    /**
     * 默认运营时区配置属性key
     */
    public static final String KEY_OPERATION_TIMEZONE_CONFIG = "operation";

    /**
     * 默认时区：东八区
     */
    public static final String DEFAULT_ZONE_NAME_CN = "Asia/Shanghai";

    /**
     * 默认时区：东八区 的zoneId
     */
    public static final ZoneId DEFAULT_ZONE_ID_CN = ZoneId.of(DEFAULT_ZONE_NAME_CN);



}
