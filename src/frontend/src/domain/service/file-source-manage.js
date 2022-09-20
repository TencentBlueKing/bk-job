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
import FileSourceModel from '@model/file/file-source';

import FileSourceManageSource from '../source/file-source-manage';

export default {
    availableSourceList (params) {
        return FileSourceManageSource.getAvailableList(params)
            .then(({ data }) => {
                data.data = data.data.map(file => Object.freeze(new FileSourceModel(file)));
                return data;
            });
    },
    fetchAliasCheck (params = {}) {
        return FileSourceManageSource.getAliasCheck(params)
            .then(({ data }) => data);
    },
    fetchFileSourceList (params) {
        return FileSourceManageSource.getWorkTableList(params)
            .then(({ data }) => {
                data.data = data.data.map(file => Object.freeze(new FileSourceModel(file)));
                return data;
            });
    },
    addSource (params) {
        return FileSourceManageSource.addFileSource(params)
            .then(({ data }) => data);
    },
    updateSource (params) {
        return FileSourceManageSource.updateFileSource(params)
            .then(({ data }) => data);
    },
    getSourceInfo (params) {
        return FileSourceManageSource.getFileSourceInfo(params)
            .then(({ data }) => {
                data = Object.freeze(new FileSourceModel(data));
                return data;
            });
    },
    removeSource (params) {
        return FileSourceManageSource.removeFileSource(params)
            .then(({ data }) => data);
    },
    toggleSourceEnable (params) {
        return FileSourceManageSource.toggleEnable(params)
            .then(({ data }) => data);
    },
    fetchParams (params) {
        return FileSourceManageSource.getParams(params)
            .then(({ data }) => data);
    },
};
