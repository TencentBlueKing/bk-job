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
import TaskModel from '@model/task/task';

import TaskManageSource from '../source/task-manage';

export default {
  taskList(params = {}) {
    return TaskManageSource.getAll(params)
      .then(({ data }) => {
        data.data = data.data.map(item => new TaskModel({
          ...item,
          canCreate: data.canCreate,
        }));
        return data;
      });
  },
  taskDetail(params, config) {
    return TaskManageSource.getDataById(params, config)
      .then(({ data }) => Object.freeze(new TaskModel(data)));
  },
  taskClone(params, config) {
    return TaskManageSource.getDataById(params, config)
      .then(({ data }) => {
        data.name = `${data.name.slice(0, 55)}_copy`;
        return Object.freeze(new TaskModel(data, true));
      });
  },
  create(params) {
    return TaskManageSource.create(params)
      .then(({ data }) => data);
  },
  taskUpdate(params) {
    return TaskManageSource.update(params)
      .then(({ data }) => data);
  },
  taskDelete(params) {
    return TaskManageSource.deleteById(params);
  },
  taskUpdateBasic(params) {
    return TaskManageSource.updateBasic(params)
      .then(({ data }) => data);
  },
  taskUpdateFavorite(params) {
    return TaskManageSource.updateFavorite(params)
      .then(({ data }) => data);
  },
  taskDeleteFavorite(params) {
    return TaskManageSource.deleteFavorite(params)
      .then(({ data }) => data);
  },
  taskCheckName(params) {
    return TaskManageSource.getCheckResult(params)
      .then(({ data }) => data);
  },
  fetchBasic(params) {
    return TaskManageSource.getBasicById(params)
      .then(({ data }) => data.map(item => new TaskModel(item)));
  },
  batchUpdateTag(params = {}) {
    return TaskManageSource.batchUpdateTag(params)
      .then(({ data }) => data);
  },
  fetchTagCount(params = {}) {
    return TaskManageSource.getTagCount(params)
      .then(({ data }) => data);
  },
};
