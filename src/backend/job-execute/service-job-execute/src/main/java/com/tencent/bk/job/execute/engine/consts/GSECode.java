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

package com.tencent.bk.job.execute.engine.consts;


/**
 * GSE的错误码
 */
public interface GSECode {
    /**
     * Agent异常
     */
    public static final Integer AGENT_DOWN = 117;

    /**
     * 运行脚本执行状态
     */
    enum Status {

        /**
         * 0：原子任务已派发；
         */
        UNSTART(0),

        /**
         * 1：原子任务执行中；
         */
        RUNNING(1),

        /**
         * 2：原子任务执行成功；
         */
        SUCCESS(2),

        /**
         * 3：原子任务执行超时；
         */
        TIMEOUT(3),

        /**
         * 4：原子任务被放弃执行；
         */
        DISCARD(4),

        /**
         * 5：原子任务执行失败；
         */
        FAIL(5),

        /**
         * 其他错误类型
         */
        ERROR(-1);

        final int value;

        Status(int value) {
            this.value = value;
        }

        /**
         * 通过值获取枚举
         *
         * @param value
         * @return
         */
        public static Status getStatus(int value) {
            for (Status status : Status.values()) {
                if (status.getValue() == value) {
                    return status;
                }
            }
            return ERROR;
        }

        public int getValue() {
            return this.value;
        }
    }

    /**
     * 内部GSE原子执行状态
     */
    enum AtomicErrorCode {

        /**
         * 0：已完成；
         */
        FINISHED(0),

        /**
         * 115: 执行中；
         */
        RUNNING(115),
        /**
         * 126: 任务强制终止
         */
        TERMINATE(126),
        /**
         * -1: 其他错误类型
         */
        ERROR(-1);

        final int value;

        AtomicErrorCode(int value) {
            this.value = value;
        }

        /**
         * 通过值获取枚举
         *
         * @param value
         * @return
         */
        public static AtomicErrorCode getErrorCode(int value) {
            for (AtomicErrorCode errorCode : AtomicErrorCode.values()) {
                if (errorCode.getValue() == value) {
                    return errorCode;
                }
            }
            return ERROR;
        }

        public int getValue() {
            return this.value;
        }
    }

    /**
     * 外部GSE统一错误码执行状态
     */
    enum ErrorCode {

        /**
         * 0：已完成；
         */
        FINISHED(0),

        /**
         * 1000115: 执行中；
         */
        RUNNING(1000115),

        /**
         * 1000126: 任务强制终止
         */
        TERMINATE(1000126),

        /**
         * 1000101: 其他错误类型
         */
        ERROR(1000101);

        final int value;

        ErrorCode(int value) {
            this.value = value;
        }

        /**
         * 通过值获取枚举
         *
         * @param value
         * @return
         */
        public static ErrorCode getErrorCode(int value) {
            for (ErrorCode errorCode : ErrorCode.values()) {
                if (errorCode.getValue() == value) {
                    return errorCode;
                }
            }
            return ERROR;
        }

        public int getValue() {
            return this.value;
        }
    }
}
