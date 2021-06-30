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

package com.tencent.bk.job.manage.manager.script.check;

import com.google.common.collect.Maps;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 脚本检查参数
 */
@Setter
@Getter
public class ScriptCheckParam {
    private static final Pattern var = Pattern.compile("\\b(?<var>([_]\\w+)|([a-zA-Z]\\w*))=(?<sp>['\"])?" +
        "(?<value>[^\\\\\\s]+?)\\k<sp>?[;\\s]");
    /**
     * 脚本内容（原始）
     */
    private final String content;
    /**
     * 脚本内容按行来存储
     */
    private final String[] lines;
    /**
     * 每一行最后一个字节所处于脚本内容中的位置
     */
    private final Map<Integer, Integer> linePos;
    /**
     * 变量定义  变量=值
     */
    private final Map<String, String> varMap;
    /**
     * 脚本类型
     */
    private ScriptTypeEnum scriptType;

    public ScriptCheckParam(ScriptTypeEnum scriptType, String content) {
        this.scriptType = scriptType;
        this.content = content;
        String[] lines = StringUtils.splitPreserveAllTokens(content, "\n");
        this.lines = lines;
        this.linePos = Maps.newTreeMap();
        int pos = 0;
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            linePos.put(lineNumber, (pos += lines[lineNumber - 1].length() + 1));
        }

        this.varMap = Maps.newHashMap();
        Matcher matcher = var.matcher(content);
        while (matcher.find()) {
            varMap.put(matcher.group("var"), matcher.group("value"));
        }
    }
}
