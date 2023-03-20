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
 * Agent 任务状态
 */
public enum AgentTaskStatusEnum {
    /**
     * 未知错误
     */
    UNKNOWN_ERROR(0),
    /**
     * Agent异常
     */
    AGENT_ERROR(1),
    /**
     * 无效主机
     */
    HOST_NOT_EXIST(2),
    /**
     * 上次已成功
     */
    LAST_SUCCESS(3),
    /**
     * 等待执行
     */
    WAITING(5),
    /**
     * 正在执行
     */
    RUNNING(7),
    /**
     * 执行成功
     */
    SUCCESS(9),
    /**
     * 执行失败
     */
    FAILED(11),
    /**
     * 任务下发失败
     */
    SUBMIT_FAILED(12),
    /**
     * 任务超时
     */
    TASK_TIMEOUT(13),
    /**
     * 任务日志错误
     */
    LOG_ERROR(15),
    /**
     * GSE脚本日志超时
     */
    GSE_SCRIPT_TIMEOUT(16),
    /**
     * GSE文件日志超时
     */
    GSE_FILE_TIMEOUT(17),
    /**
     * Agent未安装
     */
    AGENT_NOT_INSTALLED(18),
    /**
     * 脚本执行失败
     */
    SCRIPT_FAILED(101),
    /**
     * 脚本执行超时
     */
    SCRIPT_TIMEOUT(102),
    /**
     * 脚本执行被终止
     */
    SCRIPT_TERMINATE(103),
    /**
     * 脚本返回码非零
     */
    SCRIPT_NOT_ZERO_EXIT_CODE(104),
    /**
     * 文件传输失败
     */
    COPYFILE_FAILED(202),
    /**
     * 源文件不存在
     */
    COPYFILE_SOURCE_FILE_NOT_EXIST(203),
    /**
     * 文件任务系统错误-未分类的
     */
    FILE_ERROR_UNCLASSIFIED(301),
    /**
     * 文件任务超时
     */
    GSE_TIMEOUT(303),
    /**
     * Agent异常
     */
    GSE_AGENT_ERROR(310),
    /**
     * 用户名不存在
     */
    GSE_USER_ERROR(311),
    /**
     * 用户密码错误
     */
    GSE_USER_PWD_ERROR(312),
    /**
     * 文件获取失败
     */
    GSE_FILE_ERROR(320),
    /**
     * 文件超出限制
     */
    GSE_FILE_SIZE_EXCEED(321),
    /**
     * 文件传输错误
     */
    GSE_FILE_TASK_ERROR(329),
    /**
     * 任务执行出错
     */
    GSE_TASK_ERROR(399),
    /**
     * GSE 任务强制终止成功
     */
    GSE_TASK_TERMINATE_SUCCESS(403),
    /**
     * GSE 任务强制终止失败
     */
    GSE_TASK_TERMINATE_FAILED(404),
    /**
     * 未知状态
     */
    UNKNOWN(500);


    private final int status;

    AgentTaskStatusEnum(int status) {
        this.status = status;
    }

    public static AgentTaskStatusEnum valueOf(Integer status) {
        if (status == null) {
            return null;
        }
        for (AgentTaskStatusEnum agentTaskStatus : values()) {
            if (agentTaskStatus.status == status) {
                return agentTaskStatus;
            }
        }
        return UNKNOWN_ERROR;
    }

    public final int getValue() {
        return status;
    }

    public String getI18nKey() {
        return "agent.task.status." + this.name().toLowerCase();
    }

    public static boolean isSuccess(AgentTaskStatusEnum status) {
        if (status == null) {
            return false;
        }
        return status == SUCCESS || status == LAST_SUCCESS;
    }

    public static boolean isSuccess(Integer status) {
        if (status == null) {
            return false;
        }
        return status.equals(SUCCESS.getValue()) || status.equals(LAST_SUCCESS.getValue());
    }

    /**
     * 是否完成状态
     */
    public boolean isFinished() {
        return this != AgentTaskStatusEnum.RUNNING && this != AgentTaskStatusEnum.WAITING;
    }

}
