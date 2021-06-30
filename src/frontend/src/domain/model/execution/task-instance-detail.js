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
import TaskInstanceModel from '@model/execution/task-instance';
import TaskInstanceStepMode from '@model/execution/task-instance-step';

// 作业任务的实例信息
export default class TaskInstanceDetail {
    constructor (payload) {
        this.taskInstance = new TaskInstanceModel(payload.taskInstance || {});
        this.steps = this.initSteps(payload.steps);
        this.variables = this.initVariables(payload.variables);
    }

    get stepInfo () {
        if (this.steps.length < 1) {
            return {};
        }
        return this.steps[0];
    }

    initSteps (steps) {
        if (!_.isArray(steps)) {
            return [];
        }
        return steps.map(_ => new TaskInstanceStepMode(_));
    }

    initVariables (payload) {
        if (!_.isArray(payload)) {
            return [];
        }
        return payload;
    }
}
