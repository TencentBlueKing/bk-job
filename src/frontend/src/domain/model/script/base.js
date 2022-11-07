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

import Model from '@model/model';

import I18n from '@/i18n';

const STATUS_INVALID = -1; // 未保存
const STATUS_DRAFT = 0; // 未上线
const STATUS_ONLINE = 1; // 已上线
const STATUS_OFFLINE = 2; // 已下线
const STATUS_DISABLED = 3; // 已禁用

export default class Base extends Model {
    static STATUS_INVALID = STATUS_INVALID;
    static STATUS_DRAFT = STATUS_DRAFT;
    static STATUS_ONLINE = STATUS_ONLINE;
    static STATUS_OFFLINE = STATUS_OFFLINE;
    static STATUS_DISABLED = STATUS_DISABLED;

    static STATUS_TEXT_MAP = {
        [STATUS_INVALID]: I18n.t('未保存'),
        [STATUS_DRAFT]: I18n.t('未上线'),
        [STATUS_ONLINE]: I18n.t('已上线'),
        [STATUS_OFFLINE]: I18n.t('已下线'),
        [STATUS_DISABLED]: I18n.t('已禁用'),
    };
}
