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

/* eslint-disable no-param-reassign */
import _ from 'lodash';

import CrontabVariableModel from '@model/crontab/variable';

import {
    prettyDateTimeFormat,
} from '@utils/assist';
import Translate from '@utils/cron/translate';

import I18n from '@/i18n';

const STATUS_NOTSTARTED = 0;
const STATUS_SUCCESS = 1;
const STATUS_FAILURE = 2;

export default class Crontab {
    static STATUS_MAP = {
        [STATUS_NOTSTARTED]: I18n.t('未开始'),
        [STATUS_SUCCESS]: I18n.t('成功'),
        [STATUS_FAILURE]: I18n.t('失败'),
        
    };

    static STATUS_ICON_TYPE = {
        [STATUS_NOTSTARTED]: 'sync-default',
        [STATUS_SUCCESS]: 'sync-success',
        [STATUS_FAILURE]: 'sync-failed',
        
    };

    constructor (payload) {
        this.scopeType = payload.scopeType;
        this.scopeId = payload.scopeId;
        this.creator = payload.creator;
        this.createTime = payload.createTime;
        this.cronExpression = payload.cronExpression;
        this.executeTime = payload.executeTime || '';
        this.id = payload.id;
        this.enable = payload.enable;
        this.endTime = prettyDateTimeFormat(parseInt(payload.endTime, 10) * 1000);
        this.lastExecuteStatus = payload.lastExecuteStatus;
        this.lastModifyTime = payload.lastModifyTime;
        this.lastModifyUser = payload.lastModifyUser;
        this.name = payload.name;
        this.notifyChannel = payload.notifyChannel || [];
        this.notifyOffset = payload.notifyOffset;
        this.scriptId = payload.scriptId || 0;
        this.scriptVersionId = payload.scriptVersionId || 0;
        this.taskPlanId = payload.taskPlanId || 0;
        this.taskPlanName = payload.taskPlanName;
        this.taskTemplateId = payload.taskTemplateId;
        this.failCount = payload.failCount || 0;
        this.totalCount = payload.totalCount || 0;
        this.lastFailRecord = payload.lastFailRecord || [];

        this.variableValue = this.initVariableValue(payload.variableValue);
        this.notifyUser = this.initNotifyUser(payload.notifyUser);
        
        // 权限
        this.canManage = payload.canManage;
        // 异步数据获取状态
        this.isPlanLoading = true;
        this.isStatictisLoading = true;
    }

    /**
     * @desc 执行策略类型（单次执行，周期执行）
     * @returns { String }
     */
    get executeStrategy () {
        if (this.cronExpression) {
            return 'period';
        }
        return 'once';
    }

    /**
     * @desc 执行策略类型显示文本
     * @returns { String }
     */
    get executeStrategyText () {
        if (this.cronExpression) {
            return I18n.t('周期执行');
        }
        return I18n.t('单次执行');
    }

    /**
     * @desc 执行事件tips
     * @returns { String }
     */
    get executeTimeTips () {
        if (this.cronExpression) {
            const [
                month,
                dayOfMonth,
                dayOfWeek,
                hour,
                minute,
            ] = Translate(this.cronExpression);
            let text = month;
            
            if (dayOfMonth) {
                text += dayOfMonth;
            }
            if (dayOfWeek) {
                if (month && dayOfWeek) {
                    if (dayOfMonth) {
                        text += '以及当月';
                    } else {
                        text += '的';
                    }
                }

                text += dayOfWeek;
            }
            
            if (hour) {
                if (dayOfMonth || dayOfWeek) {
                    text += '的';
                }
                text += hour;
            }
            text += minute;
            return text;
        }
        return this.executeTime;
    }

    /**
     * @desc 执行状态的标记 ICON
     * @returns { String }
     */
    get statusIconType () {
        return Crontab.STATUS_ICON_TYPE[this.lastExecuteStatus];
    }

    /**
     * @desc 执行状态展示文本
     * @returns { String }
     */
    get statusText () {
        return Crontab.STATUS_MAP[this.lastExecuteStatus];
    }

    /**
     * @desc 定时任务的执行策略
     * @returns { String }
     */
    get policeText () {
        if (this.cronExpression) {
            return this.cronExpression;
        }
        return this.executeTime.slice(0, 19);
    }

    /**
     * @desc 周期执行成功率显示文本
     * @returns { String }
     */
    get successRateText () {
        if (!this.failCount && !this.totalCount) {
            return '--';
        }
        const getStyle = (value) => {
            const stack = [
                'padding: 0 6px',
                'border-radius: 2px',
            ];
            if (value === 0) {
                stack.push('color: #EA3636');
                stack.push('background-color: #FFEBEB');
            } else if (value < 40) {
                stack.push('color: #FF8801');
                stack.push('background-color: #FFEFD6');
            } else if (value < 80) {
                stack.push('color: #EBB626');
                stack.push('background-color: #FFF6D1');
            } else {
                stack.push('color: #63656e');
                stack.push('background-color: #F0F1F5');
            }
            return stack.join(';');
        };
        const rate = Math.floor(((this.totalCount - this.failCount) / this.totalCount) * 100);
        return `<span style="${getStyle(rate)}">${rate}%</span>`;
    }

    /**
     * @desc 周期执行成功率 tips
     * @returns { Array }
     */
    get successRateTips () {
        const tips = `
            <p>
                ${I18n.t('最近')}<span style="padding: 0 0.2em;">${this.totalCount}</span>${I18n.t('次')}
                ${I18n.t('有')}<span style="padding: 0 0.2em;">${this.failCount}</span>${I18n.t('次失败')}:
            </p>
        `;
        return this.lastFailRecord.slice(0, 5).reduce((result, item) => {
            result = `${result}<p>${item}</p>`;
            return result;
        }, tips);
    }

    /**
     * @desc 周期执行成功率数据为空
     * @returns { Boolean }
     */
    get isRateEmpty () {
        return !this.failCount || !this.totalCount;
    }

    /**
     * @desc 最新执行失败记录
     * @returns { Array }
     */
    get lastFailRecordList () {
        const len = this.lastFailRecord.length;
        const MAX_LEN = 5;
        let lastFailRecords = this.lastFailRecord;
        if (len > MAX_LEN) {
            lastFailRecords = lastFailRecords.slice(0, MAX_LEN);
        }
        return lastFailRecords.map(item => prettyDateTimeFormat(item));
    }

    /**
     * @desc 更多失败记录
     * @returns { Boolean }
     */
    get showMoreFailAcion () {
        return this.lastFailRecord.length > 0;
    }

    /**
     * @desc 定时任务全局变量
     * @param { Array } variableValue
     * @returns { Array }
     */
    initVariableValue (variableValue) {
        if (!_.isArray(variableValue)) {
            return [];
        }
        return variableValue.map(_ => new CrontabVariableModel(_));
    }

    /**
     * @desc 定时任务消息通知人
     * @param { Object } payload
     * @returns { Object }
     */
    initNotifyUser (payload) {
        if (!_.isObject(payload)) {
            return {
                roleList: [],
                userList: [],
            };
        }
        return {
            roleList: payload.roleList || [],
            userList: payload.userList || [],
        };
    }
}
