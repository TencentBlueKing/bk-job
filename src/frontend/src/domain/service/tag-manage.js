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
import TagModel from '@model/tag';

import TagManageSource from '../source/tag-manage';
import TagNumSource from '../source/tag-num';

export default {
    /**
     * @desc 列表接口，支持分页
     * @param { Object } params 筛选参数
     * @returns { Promise }
     */
    fetchTagList (params = {}) {
        return TagManageSource.getAll(params)
            .then(({ data }) => {
                data.data = data.data.map(tag => new TagModel(tag));
                return data;
            });
    },
    /**
     * @desc tag 基础信息列表（全量，不支持分页）
     * @param { Object } params 筛选参数
     * @returns { Promise }
     */
    fetchWholeList (params = {}) {
        return TagManageSource.getAllWithBasic(params)
            .then(({ data }) => data.map(item => new TagModel(item)));
    },
    /**
     * @desc 批量流转 tag
     * @param { Object } params 筛选参数
     * @returns { Promise }
     */
    batchUpdate (params = {}) {
        return TagManageSource.batchUpdate(params)
            .then(({ data }) => data);
    },
    fetchTagOfSearch (name) {
        return TagManageSource.getAllWithBasic({
            name,
        }).then(({ data }) => data.map(tag => new TagModel(tag)));
    },
    fetchTagTemplateNum (params = {}) {
        return TagNumSource.getNum(params)
            .then(({ data }) => data);
    },
    remove (params = {}) {
        return TagManageSource.remove(params)
            .then(({ data }) => data);
    },
    updateTag (params = {}) {
        return TagManageSource.update(params)
            .then(({ data }) => data);
    },
    createTag (params) {
        return TagManageSource.create(params)
            .then(({ data }) => data);
    },
    checkName (params = {}) {
        return TagManageSource.checkName(params)
            .then(({ data }) => data);
    },
};
