/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
import CrontabModel from '@model/crontab/crontab';

import CronJobSource from '../source/cron-job';
import TaskPlanSource from '../source/task-plan';

export default {
  create(params) {
    return CronJobSource.create(params)
      .then(({ data }) => data);
  },
  update(params = {}) {
    return CronJobSource.update(params)
      .then(({ data }) => data);
  },
  getDetail(params = {}, payload) {
    return CronJobSource.getDataById(params, payload)
      .then(({ data }) => new CrontabModel(data));
  },
  remove(params = {}) {
    return CronJobSource.deleteDataById(params);
  },
  fetchList(params, payload) {
    return CronJobSource.getAll(params, payload)
      .then(({ data }) => {
        data.data = data.data.map(item => new CrontabModel(item));

        const cronJobList = data.data;
        const planIds = cronJobList.map(item => item.taskPlanId).join(',');

        if (planIds.length > 0) {
          // 获取定时任务的支持方案信息
          TaskPlanSource.getPlansBasicinfo({ planIds })
            .then((plansBasicInfo) => {
              const planMap = {};
              plansBasicInfo.data.forEach((item) => {
                planMap[item.id] = item.name;
              });
              cronJobList.forEach((cronJob) => {
                cronJob.isPlanLoading = false;
                cronJob.taskPlanName = planMap[cronJob.taskPlanId];
              });
              return data;
            });
          // 获取定时任务的执行成功率信息
          CronJobSource.getStatictis({
            cronJobIds: cronJobList.map(item => item.id).join(','),
          }).then((data) => {
            const statictisMap = data.data.reduce((result, item) => {
              result[item.id] = item;
              return result;
            }, {});
            cronJobList.forEach((cronJob) => {
              const {
                lastExecuteStatus,
                failCount,
                totalCount,
                lastFailRecord,
              } = statictisMap[cronJob.id] || {};
              cronJob.lastExecuteStatus = lastExecuteStatus;
              cronJob.isStatictisLoading = false;
              cronJob.failCount = failCount || 0;
              cronJob.totalCount = totalCount || 0;
              cronJob.lastFailRecord = Object.freeze(lastFailRecord || []);
            });
          });
        }

        return data;
      });
  },
  updateStatus(params = {}) {
    return CronJobSource.updateStatus(params);
  },
  checkName(params = {}) {
    return CronJobSource.getCheckResult(params)
      .then(({ data }) => data);
  },
  updatePlanTask(params = {}) {
    return CronJobSource.updateVariableAndEnable(params)
      .then(({ data }) => data);
  },
  fetchTaskOfPlan(params = {}) {
    return CronJobSource.getDataByPlanId(params)
      .then(({ data }) => data.map(item => new CrontabModel(item)));
  },
  fetchTaskOfPlanBatch(params = {}) {
    return CronJobSource.getDataByPlanIds(params)
      .then(({ data }) => data);
  },
  fetchUnlaunchHistory(params) {
    return CronJobSource.getUnlaunchHistory(params)
      .then(({ data }) => data);
  },
};
