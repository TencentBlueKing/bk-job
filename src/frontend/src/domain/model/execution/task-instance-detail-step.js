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

import TaskInstanceDetailStepFileModel from '@model/execution/task-instance-detail-step-file';
import TaskInstanceDetailStepScriptModel from '@model/execution/task-instance-detail-step-script';

// 作业执行的步骤实例
export default class TaskInstanceDetailStep {
    constructor (payload) {
        this.id = payload.id || '';
        this.name = payload.name || '';
        this.type = payload.type;
        this.enable = Boolean(payload.enable);
        this.delete = payload.delete;
        this.rollingEnabled = Boolean(payload.rollingEnabled);
        this.rollingConfig = this.initRollingConfig(payload.rollingConfig);
        this.approvalStepInfo = {};
        this.fileStepInfo = this.initFileStepInfo(payload.fileStepInfo);
        this.scriptStepInfo = this.initScriptStepInfo(payload.scriptStepInfo);
    }

    /**
     * @desc 处理作业的分发文件步骤
     * @param { Object } fileStepInfo
     * @returns { Object }
     */
    initFileStepInfo (fileStepInfo) {
        if (!_.isObject(fileStepInfo)) {
            return {};
        }
        return new TaskInstanceDetailStepFileModel(fileStepInfo);
    }

    /**
     * @desc 处理作业的执行脚本步骤
     * @param { Object } scriptStepInfo
     * @returns { Object }
     */
    initScriptStepInfo (scriptStepInfo) {
        if (!_.isObject(scriptStepInfo)) {
            return {};
        }
        return new TaskInstanceDetailStepScriptModel(scriptStepInfo);
    }

    initRollingConfig (rollingConfig) {
        if (!_.isObject(rollingConfig)) {
            return {
                expr: '',
                mode: '',
            };
        }
        const {
            expr = '',
            mode = '',
        } = rollingConfig;
        return {
            expr,
            mode,
        };
    }
}
