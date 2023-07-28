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

package com.tencent.bk.job.common.crypto;

/**
 * 加密场景枚举值
 */
public enum CryptoScenarioEnum {
    // 脚本敏感参数
    SCRIPT_SENSITIVE_PARAM((byte) 0, "scriptSensitiveParam"),
    // 密文变量
    CIPHER_VARIABLE((byte) 0, "cipherVariable"),
    // DB账号的密码
    DATABASE_PASSWORD((byte) 0, "databasePassword"),
    // 凭证信息
    CREDENTIAL((byte) 0, "credential"),
    // 导出作业的密码
    EXPORT_JOB_PASSWORD((byte) 0, "exportJobPassword"),
    // 导出作业的备份文件
    BACKUP_FILE((byte) 0, "backupFile");

    // 加密类型：0为对称加密，1为非对称加密
    private final byte type;
    // 场景标识
    private final String value;

    CryptoScenarioEnum(byte type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public byte getType() {
        return type;
    }
}
