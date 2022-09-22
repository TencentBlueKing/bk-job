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

package com.tencent.bk.job.manage.common.consts.script;

import lombok.Getter;

/**
 * @date 2019/09/19
 */
@Getter
public enum ScriptTypeEnum {
    /**
     * 脚本类型枚举类
     */
    SHELL(1, "shell", ".sh"),
    BAT(2, "bat", ".bat"),
    PERL(3, "perl", ".pl"),
    PYTHON(4, "python", ".py"),
    POWERSHELL(5, "powershell", ".ps1"),
    SQL(6, "sql", ".sql");

    private final Integer value;
    private final String name;
    private final String ext;

    ScriptTypeEnum(Integer type, String name, String ext) {
        this.value = type;
        this.name = name;
        this.ext = ext;
    }

    public static String getName(Integer type) {
        for (ScriptTypeEnum scriptTypeEnum : values()) {
            if (scriptTypeEnum.value.equals(type)) {
                return scriptTypeEnum.getName();
            }
        }
        return "";
    }

    public static ScriptTypeEnum valueOf(Integer type) {
        for (ScriptTypeEnum scriptTypeEnum : values()) {
            if (scriptTypeEnum.value.equals(type)) {
                return scriptTypeEnum;
            }
        }
        return null;
    }

    public static boolean isValid(Integer type) {
        if (type == null) {
            return false;
        }
        return valueOf(type) != null;
    }

    public static ScriptTypeEnum getTypeByExt(String ext) {
        if (SHELL.ext.equals(ext)) {
            return SHELL;
        } else if (BAT.ext.equals(ext)) {
            return BAT;
        } else if (PERL.ext.equals(ext)) {
            return PERL;
        } else if (PYTHON.ext.equals(ext)) {
            return PYTHON;
        } else if (POWERSHELL.ext.equals(ext)) {
            return POWERSHELL;
        } else if (SQL.ext.equals(ext)) {
            return SQL;
        } else {
            return null;
        }
    }

    public static String getExtByValue(int value) {
        ScriptTypeEnum scriptType = valueOf(value);
        if (scriptType != null) {
            return scriptType.getExt();
        } else {
            return "";
        }
    }

    /**
     * 获取脚本类型的字典名称的排序
     *
     * @return 排序
     */
    public static ScriptTypeEnum[] getScriptTypeNameAscSort() {
        // 按照脚本语言名称字典顺序排序.Bat(2)->Perl(3)->Powershell(5)->Python(4)->Shell(1)->SQL(6)
        return new ScriptTypeEnum[]{
            BAT, PERL, POWERSHELL, PYTHON, SHELL, SQL
        };
    }
}
