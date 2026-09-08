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

package com.tencent.bk.job.manage.model.esb.v4.req.validator;

/**
 * 作业模板写接口的分支校验分组。由各自的 GroupSequenceProvider 按请求内容动态启用。
 */
public interface V4JobTemplateValidationGroups {

    /**
     * 按步骤类型启用，决定哪个步骤详情对象必填。
     */
    interface StepType {
        interface Script {
        }

        interface File {
        }

        interface Approval {
        }
    }

    /**
     * 按脚本来源启用。本地脚本要求脚本内容，引用脚本/公共脚本要求脚本 ID 与版本 ID。
     */
    interface ScriptSource {
        interface Local {
        }

        interface Cited {
        }
    }

    /**
     * 按源文件类型启用。服务器文件要求执行目标与账号，文件源文件要求文件源 ID。
     */
    interface FileType {
        interface Server {
        }

        interface FileSource {
        }
    }
}
