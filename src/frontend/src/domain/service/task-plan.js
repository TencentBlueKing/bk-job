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
import VariableModel from '@model/task/global-variable';
import PlanModel from '@model/task/plan';
import TaskModel from '@model/task/task';

import TaskPlanSource from '../source/task-plan';

export default {
    fetchAllPlan (params) {
        return TaskPlanSource.getAllPlan(params)
            .then(({ data }) => {
                data.data = data.data.map(item => new PlanModel(item));
                return data;
            });
    },
    fetchBatchPlan (params) {
        return TaskPlanSource.getPlansBasicInfo(params)
            .then(({ data }) => data.map(item => new PlanModel(item)));
    },
    fetchTaskPlan (params, config) {
        return TaskPlanSource.getAllPlanOfTemplate(params, config)
            .then(({ data }) => Object.freeze(data.map(item => new PlanModel(item))));
    },
    fetchBatchTaskPlan (params) {
        return TaskPlanSource.getAllTemplatePlan(params)
            .then(({ data }) => Object.freeze(data.map(item => new PlanModel(item))));
    },
    fetchPlanEditInfo (params) {
        return TaskPlanSource.getDetail(params)
            .then(({ data }) => new PlanModel(data));
    },
    fetchPlanDetailInfo (params, config) {
        return TaskPlanSource.getDetail(params, config)
            .then(({ data }) => new PlanModel(data));
    },
    fetchPlanVariable (params, payload) {
        return TaskPlanSource.getDetail(params, payload)
            .then(({ data }) => data.variableList.map(variable => new VariableModel(variable)));
    },
    fetchDebugPlanVariable (params, payload) {
        return TaskPlanSource.getDebugInfo(params, payload)
            .then(({ data }) => data.variableList.map(variable => new VariableModel(variable)));
    },
    fetchSyncInfo (params, payload) {
        return TaskPlanSource.getSyncDataById(params, payload)
            .then(({ data }) => {
                data.templateInfo = new TaskModel(data.templateInfo);
                data.planInfo = new PlanModel(data.planInfo);
                return data;
            });
    },
    planSyncInfo (params) {
        return TaskPlanSource.updateSyncInfo(params);
    },
    planUpdate (params) {
        return TaskPlanSource.update(params).then(({ data }) => data);
    },
    planDelete (params) {
        return TaskPlanSource.delete(params);
    },
    fetchDebugInfo (params, config) {
        return TaskPlanSource.getDebugInfo(params, config)
            .then(({ data }) => Object.freeze(new PlanModel(data)));
    },
    planCheckName (params) {
        return TaskPlanSource.getCheckResult(params)
            .then(({ data }) => data);
    },
    updateFavorite (params) {
        return TaskPlanSource.updateFavorite(params);
    },
    deleteFavorite (params) {
        return TaskPlanSource.deleteFavorite(params);
    },
    fetchPlanData (params) {
        return TaskPlanSource.getDataById(params)
            .then(({ data }) => data);
    },
    batchUpdateVariable (params) {
        return TaskPlanSource.batchUpdateVariable(params)
            .then(({ data }) => data);
    },
};
