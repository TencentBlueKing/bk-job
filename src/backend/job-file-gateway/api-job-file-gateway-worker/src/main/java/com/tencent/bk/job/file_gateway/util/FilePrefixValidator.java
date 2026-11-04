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

package com.tencent.bk.job.file_gateway.util;

import java.util.regex.Pattern;

/**
 * 文件源 filePrefix 字段安全校验工具
 */
public final class FilePrefixValidator {

    /**
     * 后台自动生成 UUID 的占位符
     */
    public static final String FILE_PREFIX_UUID = "${UUID}";

    /**
     * 自定义前缀最大长度
     */
    public static final int MAX_CUSTOM_PREFIX_LENGTH = 64;

    /**
     * 自定义前缀白名单：仅允许字母、数字、下划线、连字符，长度 1~64
     */
    private static final Pattern SAFE_CUSTOM_PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private FilePrefixValidator() {
    }

    /**
     * 判断文件前缀是否合法：
     * <ul>
     *     <li>允许为 null 或空字符串 ""（表示不加前缀）</li>
     *     <li>允许为固定占位符 "${UUID}"（下发任务时替换为随机 UUID）</li>
     *     <li>自定义前缀仅允许字母、数字、下划线（_）与连字符（-），且长度不超过 64，拒绝路径分隔符、..、空白及空字节等字符</li>
     * </ul>
     *
     * @param filePrefix 文件前缀
     * @return 是否合法
     */
    public static boolean isValid(String filePrefix) {
        return getInvalidReason(filePrefix) == null;
    }

    /**
     * 获取文件前缀不合法的具体原因，用于日志排查；合法时返回 null。
     *
     * @param filePrefix 文件前缀
     * @return 不合法原因描述；合法时返回 null
     */
    public static String getInvalidReason(String filePrefix) {
        if (filePrefix == null || filePrefix.isEmpty()) {
            return null;
        }
        if (FILE_PREFIX_UUID.equals(filePrefix)) {
            return null;
        }
        if (filePrefix.length() > MAX_CUSTOM_PREFIX_LENGTH) {
            return "length exceeds max limit " + MAX_CUSTOM_PREFIX_LENGTH + " (actual=" + filePrefix.length() + ")";
        }
        if (filePrefix.indexOf('\0') >= 0) {
            return "contains null byte";
        }
        if (filePrefix.contains("..")) {
            return "contains path traversal sequence '..'";
        }
        if (filePrefix.indexOf('/') >= 0 || filePrefix.indexOf('\\') >= 0) {
            return "contains path separator '/' or '\\'";
        }
        if (!SAFE_CUSTOM_PREFIX_PATTERN.matcher(filePrefix).matches()) {
            return "contains illegal characters (only [A-Za-z0-9_-] or ${UUID} allowed)";
        }
        return null;
    }
}
