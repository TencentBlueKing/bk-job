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
 * @desc 作业模版
*/

import _ from 'lodash';
import I18n from '@/i18n';
import TagModel from '@model/tag';
import TaskStepModel from '@model/task/task-step';
import GlobalVariableModel from '@model/task/global-variable';

const STATUS_SCRIPT_NEED_UPDATE = 1;
const STATUS_SCRIPT_DISABLED = 2;
const STATUS_SCRIPT_NEED_UPDATE_AND_DISABLE = 3;

export default class Task {
    static STATUS_SCRIPT_NEED_UPDATE = STATUS_SCRIPT_NEED_UPDATE
    static STATUS_SCRIPT_DISABLED = STATUS_SCRIPT_DISABLED
    static STATUS_SCRIPT_NEED_UPDATE_AND_DISABLE = STATUS_SCRIPT_NEED_UPDATE_AND_DISABLE
    
    constructor (payload, missId = false) {
        this.id = missId ? 0 : payload.id || 0;
        this.name = payload.name;
        this.description = payload.description || '';
        this.favored = Boolean(payload.favored);
        this.scriptStatus = payload.scriptStatus;
        this.status = payload.status || 0;
        this.version = payload.version;
        this.createTime = payload.createTime;
        this.creator = payload.creator;
        this.lastModifyTime = payload.lastModifyTime;
        this.lastModifyUser = payload.lastModifyUser;
        
        this.stepList = this.initStepList(payload.stepList, missId);
        this.tags = this.initTag(payload.tags);
        this.variables = this.initVariable(payload.variableList, missId);

        // 作业模版权限
        this.canDebug = payload.canDebug;
        this.canDelete = payload.canDelete;
        this.canEdit = payload.canEdit;
        this.canView = payload.canView;
        this.canCreate = payload.canCreate;
    }

    get tagText () {
        if (this.tags.length < 1) {
            return '--';
        }
        return this.tags.map(tag => tag.name).join('，');
    }

    get scriptStatusHtml () {
        const stack = [];
        if ([
            STATUS_SCRIPT_NEED_UPDATE, STATUS_SCRIPT_NEED_UPDATE_AND_DISABLE,
        ].includes(this.scriptStatus)) {
        // eslint-disable-next-line max-len
            stack.push(`<span tippy-tips="${I18n.t('引用脚本待更新')}"><i class="job-icon job-icon-script-update"></i></span>`);
        }
        if ([
            STATUS_SCRIPT_DISABLED, STATUS_SCRIPT_NEED_UPDATE_AND_DISABLE,
        ].includes(this.scriptStatus)) {
        // eslint-disable-next-line max-len
            stack.push(`<span tippy-tips="${I18n.t('引用脚本被禁用')}"><i class="job-icon job-icon-script-disable"></i></span>`);
        }
        return `<span style="color: #EA3636">${stack.join('')}</span>`;
    }

    get statusText () {
        return I18n.t('已上线');
    }

    toggleFavored () {
        this.favored = !this.favored;
    }

    initStepList (payload, missId = false) {
        if (!_.isArray(payload)) {
            return [];
        }
        return payload.map(item => new TaskStepModel(item, missId));
    }

    initTag (payload) {
        if (!_.isArray(payload)) {
            return [];
        }
        return payload.map(item => Object.freeze(new TagModel(item)));
    }

    initVariable (payload, missId = false) {
        if (!_.isArray(payload)) {
            return [];
        }
        return payload.map(item => new GlobalVariableModel(item, missId));
    }
}
