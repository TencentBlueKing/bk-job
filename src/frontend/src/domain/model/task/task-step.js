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

/**
 * @desc 作业模板步骤模型
 *
*/
import _ from 'lodash';

import Model from '@model/model';
import TaskApprovalStep from '@model/task/task-approval-step';
import TaskFileStep from '@model/task/task-file-step';
import TaskScriptStep from '@model/task/task-script-step';

import I18n from '@/i18n';

const TYPE_SCRIPT = 1;
const TYPE_FILE = 2;
const TYPE_APPROVAL = 3;

export default class TaskStep extends Model {
    static TYPE_SCRIPT = TYPE_SCRIPT;
    static TYPE_FILE = TYPE_FILE;
    static TYPE_APPROVAL = TYPE_APPROVAL;

    static fileStep = TaskFileStep;
    static scriptStep = TaskScriptStep;
    static approvalStep = TaskApprovalStep;

    static typeTextMap = {
        [TYPE_SCRIPT]: I18n.t('执行脚本'),
        [TYPE_FILE]: I18n.t('分发文件'),
        [TYPE_APPROVAL]: I18n.t('人工审核'),
    };

    static iconMap = {
        [TYPE_SCRIPT]: 'script-5',
        [TYPE_FILE]: 'file',
        [TYPE_APPROVAL]: 'approval',
    };

    constructor (payload, isClone = false) {
        super();
        this.id = isClone ? -payload.id : payload.id;
        this.name = payload.name;
        this.type = payload.type || 0;
        this.delete = payload.delete || 0;
        this.enable = payload.enable || 0;
        this.templateStepId = payload.templateStepId || 0;
        this.refVariables = payload.refVariables || [];
        
        this.approvalStepInfo = this.initApprovalStepInfo(payload.approvalStepInfo);
        this.fileStepInfo = this.initFileStepInfo(payload.fileStepInfo);
        this.scriptStepInfo = this.initScriptStepInfo(payload.scriptStepInfo);
    }

    /**
     * @desc 步骤类型 ICON
     * @returns { String }
     */
    get icon () {
        return TaskStep.iconMap[this.type];
    }

    /**
     * @desc 步骤类型文本描述
     * @returns { String }
     */
    get typeText () {
        return TaskStep.typeTextMap[this.type];
    }

    /**
     * @desc 步骤状态 html
     * @returns { String }
     */
    get scriptStatusHtml () {
        return this.scriptStepInfo.scriptStatusHtml;
    }

    /**
     * @desc 执行脚本步骤——引用脚本需要更新
     * @returns { Boolean }
     */
    get isScriptNeedUpdate () {
        if (this.type !== TYPE_SCRIPT) {
            return false;
        }
        return this.scriptStepInfo.isNeedUpdate;
    }

    /**
     * @desc 执行脚本步骤——引用脚本本禁用
     * @returns { Boolean }
     */
    get isScriptDisabled () {
        if (this.type !== TYPE_SCRIPT) {
            return false;
        }
        return this.scriptStepInfo.isDisabled;
    }

    /**
     * @desc 脚本类型步骤
     * @returns { Boolean }
     */
    get isScript () {
        return this.type === TYPE_SCRIPT;
    }

    /**
     * @desc 分发文件类型步骤
     * @returns { Boolean }
     */
    get isFile () {
        return this.type === TYPE_FILE;
    }

    /**
     * @desc 人工审核类型步骤
     * @returns { Boolean }
     */
    get isApproval () {
        return this.type === TYPE_APPROVAL;
    }

    /**
     * @desc 初始化人工审核类型步骤
     * @param { Object } approvalStepInfo
     * @returns { Object }
     */
    initApprovalStepInfo (approvalStepInfo) {
        if (!_.isObject(approvalStepInfo)) {
            return {};
        }
        return new TaskApprovalStep(approvalStepInfo);
    }

    /**
     * @desc 初始化分发文件类型步骤
     * @param { Object } fileStepInfo
     * @returns { Object }
     */
    initFileStepInfo (fileStepInfo) {
        if (!_.isObject(fileStepInfo)) {
            return {};
        }
        return new TaskFileStep(fileStepInfo);
    }

    /**
     * @desc 初始化执行脚本类型步骤
     * @param { Object } scriptStepInfo
     * @returns { Object }
     */
    initScriptStepInfo (scriptStepInfo) {
        if (!_.isObject(scriptStepInfo)) {
            return {};
        }
        return new TaskScriptStep(scriptStepInfo);
    }
}
