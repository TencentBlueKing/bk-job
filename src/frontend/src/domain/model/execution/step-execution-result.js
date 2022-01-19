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
import ResultGroup from '@model/execution/step-execution-result-group';

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
const STATUS_EVICTED = 14;

// 步骤类型
// const TYPE_SCRIPT = 1
const TYPE_FILE = 2;
// const TYPE_APPROVAL = 3

const checkStatus = (status) => {
    // 执行成功
    if ([
        STATUS_SUCCESS,
        STATUS_PASS,
    ].includes(status)) {
        return 'success';
    }
    if ([
        STATUS_INGORE_ERROR,
    ].includes(status)) {
        return 'ingore';
    }
    // 执行失败
    if ([
        STATUS_FAIL,
        STATUS_STATE_EXCEPTION,
        STATIS_MANUAL_END,
        STATUS_FORCED_FAIL,
    ].includes(status)) {
        return 'fail';
    }
    if ([
        STATUS_EVICTED,
    ].includes(status)) {
        return 'evicted';
    }
    // 终止成功
    if ([
        STATUS_FORCED_SUCCESS,
    ].includes(status)) {
        return 'forced';
    }
    // 执行中
    if ([
        STATUS_DOING,
    ].includes(status)) {
        return 'loading';
    }
    if ([
        STATUS_FORCEDING,
    ].includes(status)) {
        return 'forceding';
    }
    // 人工确认
    if ([
        STATUS_MANUAL_CONFIRM,
    ].includes(status)) {
        return 'confirm';
    }
    // 确认终止
    if ([
        STATUS_CONFIRM_FORCED,
    ].includes(status)) {
        return 'confirmForced';
    }
    // 等待执行
    if ([
        STATUS_PENDING,
    ].includes(status)) {
        return 'disabled';
    }
    return 'disabled';
};

// 使用场景：步骤执行详情页，步骤执行结果的详细信息
// —— resultGroups 步骤执行结果分组信息
export default class StepExecutionResult {
    constructor (payload) {
        this.stepInstanceId = payload.stepInstanceId;
        this.retryCount = payload.retryCount;
        this.finished = payload.finished;
        this.name = payload.name;
        this.startTime = payload.startTime;
        this.endTime = payload.endTime;
        this.totalTime = payload.totalTime;
        this.type = payload.type;
        this.status = payload.status;
        this.statusDesc = payload.statusDesc;
        this.isLastStep = payload.isLastStep;
        this.resultGroups = this.initResultGroup(payload.resultGroups);
    }

    /**
     * @desc 步骤执行总耗时
     * @returns { String }
     */
    get totalTimeText () {
        return transformTimeFriendly(this.totalTime);
    }

    /**
     * @desc 分发文件类型的步骤
     * @returns { Boolean }
     */
    get isFile () {
        return this.type === TYPE_FILE;
    }

    /**
     * @desc 步骤执行状态展示css对应的class
     * @returns { String }
     */
    get displayStyle () {
        const styleMap = {
            success: 'success',
            ingore: 'ingore',
            fail: 'fail',
            forced: 'forced',
            forceding: 'loading',
            loading: 'loading',
            confirm: 'confirm',
            confirmForced: 'confirm-forced',
            disabled: 'disabled',
            evicted: 'fail',
        };
        return styleMap[checkStatus(this.status)];
    }

    /**
     * @desc 步骤详情可以被强制终止(步骤执行详情页面通过步骤的状态来判断作业是否可以强制终止)
     * @returns { Boolean }
     */
    get isForcedEnable () {
        return [
            STATUS_DOING,
            STATUS_MANUAL_CONFIRM,
        ].includes(this.status);
    }

    /**
     * @desc 步骤当前状态支持的操作
     * @returns { Array }
     */
    get actions () {
        const actionMap = {
            success: [],
            ingore: [],
            disabled: [],
            forced: [
                'forcedRetry',
                'next',
            ],
            fail: [
                'failIpRetry', 'allRetry', 'skip',
            ],
            forceding: [
                'forcedSkip',
            ],
            loading: [],
            confirm: [
                'confirmForced', 'confirm',
            ],
            confirmForced: [
                'confirmRetry',
            ],
            evicted: [],
        };
        // 步骤是最后一步 强制终止操作 没有进入下一步的操作
        if (this.isLastStep) {
            actionMap.forced = [
                'forcedRetry',
            ];
        }
        
        return actionMap[checkStatus(this.status)];
    }

    /**
     * @desc 初始化步骤执行结果的分组数据
     * @param { Array } resultGroups
     * @returns { Array }
     */
    initResultGroup (resultGroups) {
        if (!Array.isArray(resultGroups)) {
            return [];
        }
        return resultGroups.map(item => Object.freeze(new ResultGroup(item)));
    }
}
