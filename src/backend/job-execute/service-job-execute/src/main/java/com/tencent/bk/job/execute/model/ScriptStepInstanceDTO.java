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

package com.tencent.bk.job.execute.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScriptStepInstanceDTO {
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 脚本ID
     */
    private String scriptId;

    /**
     * 脚本版本ID
     */
    private Long scriptVersionId;

    /**
     * 执行脚本的内容
     */
    private String scriptContent;

    /**
     * 脚本来源：1-本地脚本 2-引用业务脚本 3-引用公共脚本
     */
    private Integer scriptSource;

    /**
     * 执行脚本的类型:1(shell脚本)、2(bat脚本)、3(perl脚本)、4(python脚本)、5(powershell脚本)
     *
     * @see com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum
     */
    private Integer scriptType;
    /**
     * 执行脚本的执行参数
     */
    private String scriptParam;
    /**
     * 变量解析之后的执行脚本的执行参数
     */
    private String resolvedScriptParam;

    /**
     * 任务超时时间
     */
    private Integer timeout;
    /**
     * 系统账号ID
     */
    private Long accountId;
    /**
     * 目标机器的执行账户名
     */
    private String account;
    /**
     * 步骤实例增加db SQL执行的账号,密码,端口,SQL内容,参数
     */
    private String dbAccount;
    private String dbPass;
    private Integer dbPort;
    private Integer dbType;
    private Long dbAccountId;

    /**
     * 是否敏感参数
     */
    private boolean secureParam;
}
