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

package com.tencent.bk.job.manage.common.consts.account;

import com.tencent.bk.job.manage.common.consts.globalsetting.OSTypeEnum;

/**
 * 账号类型
 *
 * @date 2019/09/19
 */
public enum AccountTypeEnum {
    LINUX(1, "Linux"),
    WINDOW(2, "Windows"),
    MYSQL(9, "MySQL"),
    ORACLE(10, "Oracle"),
    DB2(11, "DB2");

    private final Integer type;
    private final String name;

    AccountTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public static AccountTypeEnum valueOf(Integer type) {
        if (type == null) return null;
        for (AccountTypeEnum typeEnum : values()) {
            if (typeEnum.getType().equals(type)) {
                return typeEnum;
            }
        }
        return null;
    }

    public static boolean isValidDbType(Integer type) {
        if (type == null) {
            return false;
        }
        if (!MYSQL.getType().equals(type) && !ORACLE.getType().equals(type) && !DB2.getType().equals(type)) {
            return false;
        }
        return true;
    }

    public static boolean isValidSystemType(Integer type) {
        if (type == null) {
            return false;
        }
        if (!LINUX.getType().equals(type) && !WINDOW.getType().equals(type)) {
            return false;
        }
        return true;
    }

    public OSTypeEnum getOsType() {
        switch (this) {
            case LINUX:
                return OSTypeEnum.LINUX;
            case WINDOW:
                return OSTypeEnum.WINDOWS;
            default:
                return OSTypeEnum.DATABASE;
        }
    }

    public Integer getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
