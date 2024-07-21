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

package com.tencent.bk.job.common.validation;

/**
 * 联合校验分组
 */
public interface ValidationGroups {
    /**
     * 脚步校验分组
     */
    interface Script {
        interface ScriptVersionId {
        }

        interface ScriptContent {
        }

        interface ScriptId {
        }

        interface ScriptType {
        }

        interface ScriptDesc {
        }

        interface ScriptName {
        }

        interface ScriptTags {
        }

    }

    /**
     * 账号校验分组
     */
    interface Account {
        interface AccountId {
        }

        interface AccountAlias {
        }
    }

    /**
     * esb主机校验分组v3
     */
    interface EsbServerV3 {
        interface IP {
        }

        interface HostId {
        }

        interface DynamicGroup {
        }

        interface TopoNode {
        }
    }

    /**
     * 文件分发文件源校验分组
     */
    interface FileSource {
        interface ServerFile {
        }

        interface FileSourceFile {
        }

        interface LocalFile {
        }

        interface FileSourceId {
        }

        interface FileSourceCode {
        }
    }

    /**
     * 任务执行目标校验分组
     */
    interface TaskTarget {
        interface Variable {
        }

        interface HostNode {
        }

        interface ExecuteObject {
        }
    }

    /**
     * 任务步骤校验分组
     */
    interface TaskStep {
        interface ScriptStep {
        }

        interface FileStep {
        }

        interface ApprovalStep {
        }
    }

    /**
     * 全局变量校验分组
     */
    interface GrobalVar {
        interface Id {
        }

        interface Name {
        }
    }
}
