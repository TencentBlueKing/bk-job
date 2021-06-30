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

import I18n from '@/i18n';
import TaskHostNodeModel from '@model/task-host-node';

const TYPE_STRING = 1;
const TYPE_NAMESPACE = 2;
const TYPE_HOST = 3;
const TYPE_PASSWORD = 4;
const TYPE_RELATE_ARRAY = 5;
const TYPE_INDEX_ARRAY = 6;

export default class GlobalVariable {
    static TYPE_STRING = TYPE_STRING
    static TYPE_NAMESPACE = TYPE_NAMESPACE
    static TYPE_HOST = TYPE_HOST
    static TYPE_PASSWORD = TYPE_PASSWORD
    static TYPE_RELATE_ARRAY = TYPE_RELATE_ARRAY
    static TYPE_INDEX_ARRAY = TYPE_INDEX_ARRAY
    static iconMap = {
        [TYPE_STRING]: 'string',
        [TYPE_NAMESPACE]: 'namespace',
        [TYPE_HOST]: 'host',
        [TYPE_PASSWORD]: 'password',
        [TYPE_RELATE_ARRAY]: 'array',
        [TYPE_INDEX_ARRAY]: 'array',
    }

    static typeTextMap = {
        [TYPE_STRING]: I18n.t('字符串'),
        [TYPE_NAMESPACE]: I18n.t('命名空间'),
        [TYPE_HOST]: I18n.t('主机列表'),
        [TYPE_PASSWORD]: I18n.t('密文'),
        [TYPE_RELATE_ARRAY]: I18n.t('数组'),
        [TYPE_INDEX_ARRAY]: I18n.t('数组'),
    }
    
    constructor (payload, missId = false) {
        this.id = missId ? 0 : payload.id || 0;
        this.name = payload.name || '';
        this.type = payload.type || TYPE_STRING;
        this.defaultValue = payload.defaultValue || '';
        this.defaultTargetValue = new TaskHostNodeModel(payload.defaultTargetValue || {});
        this.description = payload.description || '';
        this.changeable = payload.changeable || 0;
        this.required = payload.required || 0;
        this.delete = payload.delete || 0;
        this.value = payload.value || '';
        this.targetValue = new TaskHostNodeModel(payload.targetValue || {});
    }

    // 主机变量
    get isHost () {
        return this.type === TYPE_HOST;
    }

    // 密文变量
    get isPassword () {
        return this.type === TYPE_PASSWORD;
    }

    // 变量值是否为空
    get isEmpty () {
        if (this.type === TYPE_HOST) {
            return this.defaultTargetValue.isEmpty;
        }
        return !this.defaultValue;
    }

    // 变量值是否必填
    get isRequired () {
        return !!this.required;
    }

    // 变量的icon
    get icon () {
        return GlobalVariable.iconMap[this.type];
    }

    // 类型的文本展示
    get typeText () {
        return GlobalVariable.typeTextMap[this.type];
    }

    // 类型的分类描述
    get typeDescription () {
        const descriptionMap = {
            [TYPE_STRING]: 'string',
            [TYPE_NAMESPACE]: 'namespace',
            [TYPE_HOST]: 'host',
            [TYPE_PASSWORD]: 'password',
            [TYPE_RELATE_ARRAY]: 'array',
            [TYPE_INDEX_ARRAY]: 'array',
        };
        return descriptionMap[this.type];
    }
    
    // 可变的文本描述
    get changeableText () {
        return this.changeable ? I18n.t('是') : I18n.t('否');
    }

    // 必填的问题描述
    get requiredText () {
        return this.required ? I18n.t('是') : I18n.t('否');
    }

    // 展示的值
    get valueText () {
        if ([
            TYPE_HOST,
        ].includes(this.type)) {
            return this.defaultTargetValue.text;
        }
        if ([
            TYPE_PASSWORD,
        ].includes(this.type)) {
            return '******';
        }
        return this.defaultValue || '--';
    }

    // 鼠标hover的title展示
    get title () {
        if (this.type === TYPE_PASSWORD) {
            return '';
        }
        if (this.value === '--') {
            return '';
        }
        return this.value;
    }
}
