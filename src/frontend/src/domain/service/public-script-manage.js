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
import ScriptSource from '../source/public-script-manage';
import ScriptModel from '@model/script/script';
import ScriptErrorModel from '@model/script/script-error';
import ScriptSyncModel from '@model/script/script-sync';
import ScriptRelatedModel from '@model/script/script-related';

export default {
    scriptList (params) {
        return ScriptSource.getAll(params)
            .then(({ data }) => {
                data.data = data.data.map(script => Object.freeze(new ScriptModel(script)));
                return data;
            });
    },
    scriptDetail (params) {
        return ScriptSource.getDataByScriptId(params)
            .then(({ data }) => Object.freeze(new ScriptModel(data)));
    },
    scriptName (params) {
        return ScriptSource.getName(params)
            .then(({ data }) => data.map(item => ({
                id: item,
                name: item,
            })));
    },
    scriptVersionList (params) {
        return ScriptSource.getAllVersion(params)
            .then(({ data }) => data.map(script => Object.freeze(new ScriptModel(script))));
    },
    scriptUpdate (params) {
        return ScriptSource.update(params)
            .then(({ data }) => data);
    },
    scriptUpdateMeta (params) {
        return ScriptSource.updateMeta(params)
            .then(({ data }) => data);
    },
    scriptDelete (params) {
        return ScriptSource.deleteById(params)
            .then(({ data }) => data);
    },
    versionDetail (params) {
        return ScriptSource.getDataByVersionId(params)
            .then(({ data }) => new ScriptModel(data));
    },
    scriptVersionOnline (params) {
        return ScriptSource.updateVersionStatusOnline(params);
    },
    scriptVersionOffline (params) {
        return ScriptSource.updateVersionStatusOffline(params);
    },
    scriptVersionRemove (params) {
        return ScriptSource.deleteVersionByVersionId(params);
    },
    scriptTypeList () {
        return Promise.resolve(Object.freeze([
            { id: 1, name: 'Shell' },
            { id: 2, name: 'Bat' },
            { id: 3, name: 'Perl' },
            { id: 4, name: 'Python' },
            { id: 5, name: 'Powershell' },
            { id: 6, name: 'SQL' },
        ]));
    },
    getScriptValidation (params) {
        return ScriptSource.getValidation(params)
            .then(({ data }) => data.map(item => new ScriptErrorModel(item)));
    },
    getUploadContent (params) {
        return ScriptSource.uploadGetContent(params)
            .then(({ data }) => Object.freeze(data));
    },
    getOnlineScriptList (params = {}) {
        return ScriptSource.getAllOnline(params)
            .then(({ data }) => data);
    },
    getOneOnlineScript (params = {}) {
        return ScriptSource.getOneOnlineByScriptId(params)
            .then(({ data }) => {
                if (data) {
                    return new ScriptModel(data);
                }
                return '';
            });
    },
    scriptRefTemplateSteps (params = {}) {
        return ScriptSource.getRefTemplateSteps(params)
            .then(({ data }) => data.map(script => Object.freeze(new ScriptSyncModel(script))));
    },
    scriptVersionSync (params = {}) {
        return ScriptSource.syncScriptVersion(params)
            .then(({ data }) => data.map(script => Object.freeze(new ScriptSyncModel(script))));
    },
    citeInfo (params) {
        return ScriptSource.getCiteInfo(params)
            .then(({ data }) => ({
                citedTaskPlanList: data.citedTaskPlanList.map(_ => new ScriptRelatedModel(_)),
                citedTemplateList: data.citedTemplateList.map(_ => new ScriptRelatedModel(_)),
            }));
    },
    fetchBasicInfo (params) {
        return ScriptSource.getBasiceInfoById(params)
            .then(({ data }) => new ScriptModel(data));
    },
};
