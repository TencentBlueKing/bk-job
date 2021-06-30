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

import {
    formatScriptTypeValue,
} from '@utils/assist';
import TagModel from '@model/tag';
import BaseModel from './base';

export default class Script extends BaseModel {
    constructor (payload) {
        super();
        this.appId = payload.appId;
        this.id = payload.id || 0;
        this.name = payload.name;
        this.category = payload.category;
        this.content = payload.content;
        this.createTime = payload.createTime;
        this.creator = payload.creator;
        this.description = payload.description;
        this.typeName = payload.typeName;
        this.lastModifyTime = payload.lastModifyTime;
        this.lastModifyUser = payload.lastModifyUser;
        this.publicScript = Boolean(payload.publicScript);
        this.relatedTaskPlanNum = payload.relatedTaskPlanNum || 0;
        this.relatedTaskTemplateNum = payload.relatedTaskTemplateNum || 0;
        this.scriptVersionId = payload.scriptVersionId;
        this.status = payload.status || 0;
        this.statusDesc = payload.statusDesc;
        this.syncEnabled = Boolean(payload.syncEnabled);
        this.type = payload.type || 0;
        this.typeName = formatScriptTypeValue(this.type);
        this.version = payload.version;
        this.versionDesc = payload.versionDesc;
        this.scriptVersions = this.initScriptVersion(payload.scriptVersions);
        this.tags = this.initTag(payload.tags);

        // 权限
        this.canClone = payload.canClone;
        this.canManage = payload.canManage;
        this.canView = payload.canView;
    }

    get versionText () {
        return this.getDefaultValue(this.version);
    }

    // 已上线
    get isOnline () {
        return this.status === Script.STATUS_ONLINE;
    }

    // 未上线
    get isDraft () {
        return this.status === Script.STATUS_DRAFT;
    }

    // 禁用
    get isDisabled () {
        return this.status === Script.STATUS_DISABLED;
    }

    // 无法执行上线操作
    get isDisabledOnline () {
        return [
            Script.STATUS_ONLINE, Script.STATUS_DISABLED,
        ].includes(this.status);
    }

    // 脚本是否可以被删除
    get isEnableRemove () {
        return this.relatedTaskPlanNum < 1 && this.relatedTaskTemplateNum < 1;
    }

    // 脚本版本是否可以本删除
    get isVersionEnableRemove () {
        return ![
            Script.STATUS_ONLINE, Script.STATUS_OFFLINE,
        ].includes(this.status);
    }

    // 是否可以执行
    get isExecuteDisable () {
        return !this.version;
    }

    get tagsText () {
        return this.getDefaultValue(this.tags.map(tag => tag.name).join('，'));
    }

    get statusHtml () {
        let styles = 'display: inline-block; padding: 0 8px; line-height: 18px; font-size: 12px; border-radius: 2px;';

        switch (this.status) {
            case Script.STATUS_INVALID:
                styles += 'background: #F4E3C7; color: #FF9C01';
                break;
            case Script.STATUS_ONLINE:
                styles += 'background: #E5F6EA; color: #3FC06D';
                break;
            default:
                styles += 'background: #F0F1F5; color: #979BA5';
        }
        
        return `<span style="${styles}">${Script.STATUS_TEXT_MAP[this.status]}</span>`;
    }

    initTag (tags) {
        if (!tags) {
            return [];
        }
        return tags.map(item => new TagModel(item));
    }

    initScriptVersion (versions) {
        if (!versions) {
            return [];
        }
        return versions.map(item => new Script(item));
    }
}
