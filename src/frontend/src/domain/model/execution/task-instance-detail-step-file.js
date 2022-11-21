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

import I18n from '@/i18n';

const transferModeMap = {
    1: I18n.t('严谨模式'),
    2: I18n.t('强制模式'),
    3: I18n.t('保险模式'),
};

export default class TaskInstanceDetailStepFile {
    constructor (payload = {}) {
        this.timeout = payload.timeout;
        this.uploadSpeedLimit = payload.uploadSpeedLimit || 0;
        this.downloadSpeedLimit = payload.downloadSpeedLimit || 0;
        this.notExistPathHandler = payload.notExistPathHandler;
        this.duplicateHandler = payload.duplicateHandler;
        this.transferMode = payload.transferMode || 1;
        this.ignoreError = payload.ignoreError || 0;
        this.rollingEnabled = Boolean(payload.rollingEnabled);
        
        this.fileDestination = this.initFileDestination(payload.fileDestination);
        this.fileSourceList = this.initFileSourceList(payload.fileSourceList);
        this.rollingConfig = this.initRollingConfig(payload.rollingConfig);
    }

    /**
     * @desc 上传限速展示文本
     * @returns { String }
     */
    get uploadSpeedLimitText () {
        if (this.uploadSpeedLimit < 1) {
            return I18n.t('否');
        }
        return `${this.uploadSpeedLimit} (MB/s)`;
    }

    /**
     * @desc 下载限速展示文本
     * @returns { String }
     */
    get downloadSpeedLimitText () {
        if (this.downloadSpeedLimit < 1) {
            return I18n.t('否');
        }
        return `${this.downloadSpeedLimit} (MB/s)`;
    }

    /**
     * @desc 忽略错误展示文本
     * @returns { String }
     */
    get ignoreErrorText () {
        return this.ignoreError === 0 ? I18n.t('不忽略') : I18n.t('自动忽略错误');
    }

    /**
     * @desc 传输模式展示文本
     * @returns { String }
     */
    get transferModeText () {
        return transferModeMap[this.transferMode];
    }

    /**
     * @desc 处理文件分发目标
     * @param { Object } fileDestination
     * @returns { Object }
     */
    initFileDestination (fileDestination) {
        const {
            account,
            path,
            server,
        } = fileDestination;
        return {
            account: account || '',
            path: path || '',
            server: new TaskHostNodeModel(server || {}),
        };
    }

    /**
     * @desc 处理文件源
     * @param { Array } fileSourceList
     * @returns { Array }
     */
    initFileSourceList (fileSourceList) {
        if (!_.isArray(fileSourceList)) {
            return [];
        }
        return fileSourceList.map(item => ({
            id: item.id,
            fileType: item.fileType,
            fileLocation: item.fileLocation || [],
            fileHash: item.fileHash,
            fileSize: parseInt(item.fileSize, 10) || 0,
            fileSourceId: item.fileSourceId || 0,
            host: new TaskHostNodeModel(item.host || {}),
            account: item.account || 0,
        }));
    }

    /**
     * @desc 处理滚动执行配置
     * @param { Object } rollingConfig 滚动执行配置
     * @returns { Object }
     */
    initRollingConfig (rollingConfig) {
        const config = {
            expr: '',
            mode: 1,
        };
        if (rollingConfig) {
            const {
                expr = '',
                mode = 0,
            } = rollingConfig;
            Object.assign(config, {
                expr,
                mode,
            });
        }
        return config;
    }
}
