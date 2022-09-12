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

import _ from 'lodash';

import TaskHostNodeModel from '@model/task-host-node';

import { bytePretty } from '@utils/assist';

import I18n from '@/i18n';

const AGENT_STATUS_NORMAL = 1; // 正常
const AGENT_STATUS_ABNORMAL = 0; // 异常
const AGENT_STATUS_UNINSTALLED = 2; // 未安装

// 文件类型
const FILE_TYPE_SERVER = 1;
const FILE_TYPE_LOCAL = 2;
const FILE_TYPE_SOURCE = 3;

export default class SourceFile {
    static statusAgentMap = {
        [AGENT_STATUS_NORMAL]: I18n.t('正常'),
        [AGENT_STATUS_ABNORMAL]: I18n.t('异常'),
        [AGENT_STATUS_UNINSTALLED]: I18n.t('未安装'),
    };

    static typeServer = FILE_TYPE_SERVER;
    static typeLocal = FILE_TYPE_LOCAL;
    static typeSource = FILE_TYPE_SOURCE;

    constructor (payload) {
        this.id = payload.id || 0;
        // 文件类型, 1：服务器文件， 2：本地文件，3：文件源文件
        this.fileType = payload.fileType || '';
        // 文件名(路径)
        this.fileLocation = payload.fileLocation || [];
        // 执行目标信息（全局变量和服务器主机二选一）—— 服务器文件
        this.host = new TaskHostNodeModel(payload.host || {});
        // 服务器账号——服务器文件
        this.account = payload.account;
        // 文件源ID——文件源文件
        this.fileSourceId = payload.fileSourceId;
        // 文件大小——本地文件
        this.fileSize = payload.fileSize || 0;
        // 文件 Hash 值——本地文件
        this.fileHash = payload.fileHash || '';

        // 服务器文件操作状态
        //  -编辑账号
        this.isEditAccount = false;
        //  -编辑变量
        this.isEditVariable = false;

        // 本地文件操作状态
        //  -上传进度
        this.loaded = 0;
        //  -文件总大小
        this.loadTotal = 1;
        //  -上传状态
        this.uploadStatus = 'primary'; // 失败：danger, 成功：success, 上传中：primary
    }

    /**
     * @desc 服务器文件的服务器是否是全局变量
     * @returns {Boolean}
     *
     * 服务列表为空则使用的是全局变量
    */
    get isVar () {
        return TaskHostNodeModel.isHostNodeInfoEmpty(this.host.hostNodeInfo);
    }

    /**
     * @desc 服务器文件
     * @returns {Boolean}
    */
    get isServerFile () {
        return this.fileType === FILE_TYPE_SERVER;
    }

    /**
     * @desc 本地文件
     * @returns {Boolean}
    */
    get isLocalFile () {
        return this.fileType === FILE_TYPE_LOCAL;
    }

    /**
     * @desc 文件源文件
     * @returns {Boolean}
    */
    get isSourceFile () {
        return this.fileType === FILE_TYPE_SOURCE;
    }

    /**
     * @desc 本地文件——文件大小
     * @returns {String}
    */
    get fileSizeText () {
        return bytePretty(this.fileSize);
    }

    /**
     * @desc 本地文件——上传进度
     * @returns {Number}
    */
    get uploadProgress () {
        return this.loaded / this.loadTotal;
    }

    /**
     * @desc 服务器文件——主机为空
     * @returns {Boolean}
    */
    get isHostEmpty () {
        return TaskHostNodeModel.isHostNodeInfoEmpty(this.host.hostNodeInfo);
    }

    /**
     * @desc 文件来源的唯一标记（服务器文件可能出现重复值）
     * @returns {String}
     */
    get realId () {
        if (this.fileType === FILE_TYPE_SERVER) {
            return `${this.fileLocation}____${this.account}`;
        }
        return this.fileLocationText;
    }

    /**
     * @desc 文件路径显示文本
     * @returns { String }
     *
     * 本地文件（路径）只显示文件名
     * 服务器文件显示完成字符串
     */
    get fileLocationText () {
        if (this.fileType === FILE_TYPE_LOCAL) {
            return this.fileLocation.map(fileItem => _.last(fileItem.split('/'))).join('\n');
        }
        return this.fileLocation.join('\n');
    }

    /**
     * @desc 新建时是否可以保存
     * @returns { Boolean }
     */
    get isDisableSave () {
        // 文件路径为空
        if (!this.fileLocation || this.fileLocation.length < 1) {
            return true;
        }
        // 账号为空
        if (!this.account) {
            return true;
        }
        // 服务器列表为空
        if (TaskHostNodeModel.isHostNodeInfoEmpty(this.host.hostNodeInfo)
            && this.host.variable === '') {
            return true;
        }
        return false;
    }

    /**
     * @desc 获取服务器文件使用的全局变量
     * @returns { String }
     */
    get variable () {
        if (this.host.variable) {
            return this.host.variable;
        }
        return '';
    }

    /**
     * @desc 服务器列表统计文本
     * @returns { String }
     */
    get serverDesc () {
        if (this.host.variable) {
            return this.host.variable;
        }
        const textArr = [];
        const { dynamicGroupList, hostList, nodeList } = this.host.hostNodeInfo;
        // eslint-disable-next-line max-len
        const getHtml = (len, text) => `<span><span class="strong number">${len}</span>${text}</span>`;
        if (hostList.length > 0) {
            textArr.push(getHtml(hostList.length, I18n.t('台主机.result')));
        }
        if (nodeList.length > 0) {
            textArr.push(getHtml(nodeList.length, I18n.t('个节点.result')));
        }
        if (dynamicGroupList.length > 0) {
            textArr.push(getHtml(dynamicGroupList.length, I18n.t('个分组.result')));
        }
        return `${textArr.join('<span class="sep-location"></span>\n')}`;
    }
}
