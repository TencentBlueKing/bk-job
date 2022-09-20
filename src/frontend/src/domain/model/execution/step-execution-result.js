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

import _ from 'lodash';

import ResultGroup from '@model/execution/step-execution-result-group';

import {
    transformTimeFriendly,
} from '@utils/assist';

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

// 是否滚动执行步骤
const MODE_ONCE = 1;
const MODE_ROLLING_ALL = 2;
const MODE_ROLLING_BATCH = 3;

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
    static MODE_ONCE = MODE_ONCE;
    static MODE_ROLLING_ALL = MODE_ROLLING_ALL;
    static MODE_ROLLING_BATCH = MODE_ROLLING_BATCH;
    
    constructor (payload) {
        this.gseTaskId = payload.gseTaskId;
        this.finished = payload.finished;
        this.isLastStep = payload.isLastStep;
        this.name = payload.name;
        this.retryCount = payload.retryCount;
        this.stepInstanceId = payload.stepInstanceId;
        this.startTime = payload.startTime;
        this.endTime = payload.endTime;
        this.totalTime = payload.totalTime;
        this.type = payload.type;
        this.status = payload.status;
        this.statusDesc = payload.statusDesc;
        this.runMode = payload.runMode || MODE_ONCE;
        
        this.resultGroups = this.initResultGroup(payload.resultGroups);
        this.rollingTasks = this.initRollingTasks(payload.rollingTasks);
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

    get isRollingTask () {
        return [
            MODE_ROLLING_ALL,
            MODE_ROLLING_BATCH,
        ].includes(this.runMode);
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
        // 非人工确认类型的步骤，在需要人工确认时（人工确认批次）没有对步骤的确认操作
        if (!this.isApproval) {
            actionMap.confirm = [];
        }
        
        return actionMap[checkStatus(this.status)];
    }

    /**
     * @desc 正在执行批次的索引顺序
     * @returns { Number }
     *
     * - 非滚动执行返回 Null
     * - 滚动执行返回批次排序位置
     */
    get runningBatchOrder () {
        const index = _.findIndex(this.rollingTasks, ({ latestBatch }) => latestBatch);
        return index < 0 ? undefined : index + 1;
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

    /**
     * @desc 初始化执行分批信息
     * @param { Array } rollingTasks
     * @returns { Array }
     */
    initRollingTasks (rollingTasks) {
        if (!Array.isArray(rollingTasks)) {
            return [];
        }
        return rollingTasks.map(item => Object.freeze(item));
    }
}
