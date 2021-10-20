/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
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

import {
    transformTimeFriendly,
} from '@utils/assist';

const TASK_TYPE_TASK = 0;
const TASK_TYPE_SCRIPT = 1;
const TASK_TYPE_FILE = 2;

// 执行状态
const STATUS_PENDING = 1;
const STATUS_DOING = 2;
const STATUS_SUCCESS = 3;
const STATUS_FAIL = 4;
const STATUS_PASS = 5;
const STATUS_INGORE_ERROR = 6;
const STATUS_MANUAL_CONFIRM = 7;
const STATIS_MANUAL_END = 8;
const STATUS_STATE_EXCEPTION = 9;
const STATUS_FORCEDING = 10;
const STATUS_FORCED_SUCCESS = 11;
const STATUS_FORCED_FAIL = 12;
const STATUS_CONFIRM_FORCED = 13;

const calcStatusGroup = (status) => {
    if ([
        STATUS_SUCCESS,
        STATUS_PASS,
        STATUS_INGORE_ERROR,
    ].includes(status)) {
        return 'success';
    }
    if ([
        STATUS_FAIL,
        STATUS_STATE_EXCEPTION,
        STATIS_MANUAL_END,
        STATUS_FORCED_FAIL,
    ].includes(status)) {
        return 'fail';
    }
    if ([
        STATUS_FORCED_SUCCESS,
    ].includes(status)) {
        return 'forced';
    }
    if ([
        STATUS_DOING,
        STATUS_FORCEDING,
    ].includes(status)) {
        return 'loading';
    }
    if ([
        STATUS_MANUAL_CONFIRM,
    ].includes(status)) {
        return 'confirm';
    }
    if ([
        STATUS_CONFIRM_FORCED,
    ].includes(status)) {
        return 'confirmForced';
    }
    if ([
        STATUS_PENDING,
    ].includes(status)) {
        return 'disabled';
    }
    return 'disabled';
};

// 执行历史列表
// 任务实例信息
export default class TaskInstance {
    static STATUS_ICON_TYPE = {

    }

    constructor (payload) {
        this.id = payload.id;
        this.taskId = payload.taskId;
        this.cronTaskId = payload.cronTaskId;
        this.templateId = payload.templateId;
        this.debugTask = payload.debugTask;
        this.appId = payload.appId;
        this.name = payload.name;
        this.operator = payload.operator;
        this.startupMode = payload.startupMode;
        this.startupModeDesc = payload.startupModeDesc;
        this.currentStepId = payload.currentStepId;
        this.status = payload.status;
        this.statusDesc = payload.statusDesc;
        this.startTime = payload.startTime;
        this.endTime = payload.endTime;
        this.totalTime = payload.totalTime;
        this.createTime = payload.createTime;
        this.type = payload.type;
        this.typeDesc = payload.typeDesc;
        this.canExecute = payload.canExecute;
        this.canView = payload.canView;
    }

    /**
     * @desc 任务状态的 icon
     * @returns { String }
     */
    get statusIconType () {
        const iconMap = {
            fail: 'sync-failed',
            success: 'sync-success',
            forced: 'sync-success',
            confirm: 'waiting',
            confirmForced: 'sync-failed',
            loading: 'sync-pending',
            disabled: 'sync-default',
        };
        return iconMap[calcStatusGroup(this.status)];
    }

    /**
     * @desc 任务正在执行
     * @returns { Boolean }
     */
    get isDoing () {
        return [
            STATUS_DOING,
            STATUS_FORCEDING,
        ].includes(this.status);
    }

    /**
     * @desc 表示任务状态 css 的 class
     * @returns { String }
     */
    get statusClass () {
        return calcStatusGroup(this.status);
    }

    /**
     * @desc 表示任务状态文本描述
     * @returns { String }
     */
    get statusDescHtml () {
        const statusColorMap = {
            fail: '#EA3636',
            success: '#2DCB8D',
            forced: '#2DCB8D',
            confirm: '#FF9C01',
            confirmForced: '#EA3636',
            loading: '#3A84FF',
            disabled: '#C4C6CC',
        };
        return `<span style="color: ${statusColorMap[calcStatusGroup(this.status)]}">${this.statusDesc}</span>`;
    }

    /**
     * @desc 任务执行总耗时
     * @returns { String }
     */
    get totalTimeText () {
        return transformTimeFriendly(this.totalTime);
    }

    /**
     * @desc 任务类型为作业执行
     * @returns { Boolean }
     */
    get isTask () {
        return this.type === TASK_TYPE_TASK;
    }

    /**
     * @desc 任务类型为快速执行脚本
     * @returns { Boolean }
     */
    get isScript () {
        return this.type === TASK_TYPE_SCRIPT;
    }

    /**
     * @desc 任务类型为快速分发文件
     * @returns { Boolean }
     */
    get isFile () {
        return this.type === TASK_TYPE_FILE;
    }
}
