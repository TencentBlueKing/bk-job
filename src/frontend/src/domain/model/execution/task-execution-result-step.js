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

import I18n from '@/i18n';

// 步骤类型
const TYPE_SCRIPT = 1;
const TYPE_FILE = 2;
const TYPE_APPROVAL = 3;

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

const checkStatus = (status) => {
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
    if ([
        STATUS_FORCED_SUCCESS,
    ].includes(status)) {
        return 'forced';
    }
    if ([
        STATUS_FORCEDING,
    ].includes(status)) {
        return 'forceding';
    }
    if ([
        STATUS_DOING,
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

// 作业执行详情页
// ——步骤执行详情信息
export default class TaskExecutionResultStep {
    static typeIconMap = {
        [TYPE_SCRIPT]: 'script-5',
        [TYPE_FILE]: 'file',
        [TYPE_APPROVAL]: 'approval',
    };

    static processMap = {
        success: 'step-next',
        pending: 'step-pending',
    };
    
    constructor (payload) {
        this.stepInstanceId = payload.stepInstanceId;
        this.retryCount = payload.retryCount;
        this.name = payload.name;
        this.type = payload.type;
        this.confirmMessage = payload.confirmMessage;
        this.confirmReason = payload.confirmReason;
        this.notifyChannelNameList = payload.notifyChannelNameList || [];
        this.userList = payload.userList || [];
        this.roleNameList = payload.roleNameList || [];
        this.operator = payload.operator || '--';
        this.totalTime = payload.totalTime;
        this.status = payload.status;
        this.statusDesc = payload.statusDesc;
        this.endTime = payload.endTime;
        this.startTime = payload.startTime;
        this.currentStepRunning = payload.currentStepRunning || false;
        this.isLastStep = payload.isLastStep;
    }

    /**
     * @desc 总耗时
     * @returns { String }
     */
    get totalTimeText () {
        return transformTimeFriendly(this.totalTime);
    }
    
    /**
     * @desc 步骤类型 icon
     * @returns { String }
     */
    get icon () {
        return TaskExecutionResultStep.typeIconMap[this.type];
    }
    
    /**
     * @desc 人工审核类型的步骤
     * @returns { Boolean }
     */
    get isApproval () {
        return this.type === TYPE_APPROVAL;
    }

    /**
     * @desc 人工审核类型的步骤——待审核
     * @returns { Boolean }
     */
    get isApprovaling () {
        return [
            STATUS_MANUAL_CONFIRM,
        ].includes(this.status);
    }

    /**
     * @desc 步骤执行进度icon
     * @returns { String }
     */
    get lastStepIcon () {
        if (this.status === STATUS_PENDING) {
            return 'step-pending';
        }
        return 'step-next';
    }

    /**
     * @desc 步骤类型文本描述
     * @param { Boolean } name
     * @returns { Boolean }
     */
    get typeDesc () {
        const typeMap = {
            [TYPE_SCRIPT]: I18n.t('执行脚本'),
            [TYPE_FILE]: I18n.t('分发文件'),
            [TYPE_APPROVAL]: I18n.t('人工确认'),
        };
        return typeMap[this.type];
    }

    /**
     * @desc 步骤正在执行中
     * @returns { Boolean }
     */
    get isDoing () {
        return [
            STATUS_DOING,
            STATUS_FORCEDING,
        ].includes(this.status);
    }

    /**
     * @desc 步骤可以被强制终止
     * @returns { Boolean }
     */
    get isForcedEnable () {
        return [
            STATUS_DOING,
            STATUS_MANUAL_CONFIRM,
        ].includes(this.status);
    }

    /**
     * @desc 步骤还没开始执行
     * @returns { Boolean }
     */
    get isNotStart () {
        return this.status === STATUS_PENDING;
    }

    /**
     * @desc 展示样式风格
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
     * @desc 人工审核类型的步骤——人工确认信息
     * @returns { String }
     */
    get confirmReasonHtml () {
        if (this.confirmReason) {
            return `<span>${this.confirmReason}</span>`;
        }
        return `<span style="color: #bcbec5">${I18n.t('（未发表确认信息）')}</span>`;
    }

    /**
     * @desc 该步骤可以进行的操作
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
            forceding: [
                'forcedSkip',
            ],
            fail: [
                'failIpRetry',
                'allRetry',
                'skip',
            ],
            loading: [],
            confirm: [
                'confirm',
                'confirmForced',
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
}
