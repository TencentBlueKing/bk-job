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

package com.tencent.bk.job.manage.manager.script.check.consts;

/**
 * 脚本检查项定义
 */
public class ScriptCheckItemCode {
    /**
     * 逻辑不严谨：脚本首行没有定义脚本类型，例如： #!/bin/bash
     */
    public static final String LOGIC_NEED_SHEBANG = "103701001";
    /**
     * 逻辑不严谨：请用'cd ... || exit' 或者 'cd ... || return' 来避免因目录不存在导致未知后果
     */
    public static final String LOGIC_NEED_CD_DIR_CHECK = "103701002";
    /**
     * 危险：删除高危目录，可能会影响到系统的稳定!
     */
    public static final String DANGER_RM_FORCE_ALL = "103702002";

    /**
     * 禁止不加条件扫描根目录
     */
    public static final String DANGER_FOUND_ALL = "103702101";
    /**
     * 警告:有条件扫描根目录，建议指定具体目录，不建议直接扫描根目录
     */
    public static final String DANGER_FOUND_BASE = "103702102";
    /**
     * 警告 建议加上扫描条件
     */
    public static final String DANGER_FOUND_WITH_NO_CONDITION = "103702103";
    /**
     * 重写向数据到块设备 /dev/sd?
     */
    public static final String DANGER_TEE_DEV_SD_STDIN = "103702111";
    /**
     * 指定分区格式化为ext3格式。 mkfs.ext3 /dev/sda
     */
    public static final String DANGER_MKFS_EXT3 = "103702112";
    /**
     * 不建议使用格式化命令。 fdisk
     */
    public static final String DANGER_FDISK = "103702113";
    /**
     * 拷贝文件。 dd
     */
    public static final String DANGER_DD = "103702113";

}
