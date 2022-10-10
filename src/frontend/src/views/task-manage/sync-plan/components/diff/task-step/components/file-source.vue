<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
    <div class="sync-step-server-file">
        <bk-collapse
            v-if="isShowLocalFile || isShowServerFile"
            class="host-detail"
            :value="activeResult">
            <jb-collapse-item
                v-if="isShowLocalFile"
                :active="activeResult"
                name="local">
                <span class="collapse-title">{{ $t('template.已选择') }}<span class="number">{{ localFileCount }}</span>{{ $t('template.个本地文件') }}</span>
                <template #content>
                    <table class="file-table">
                        <tbody>
                            <tr
                                v-for="(row, index) in localFileList"
                                :key="index"
                                :class="localFileDiff[row.realId]">
                                <td>
                                    <div
                                        v-bk-overflow-tips
                                        class="file-path-text">
                                        {{ row.fileLocationText }}
                                    </div>
                                </td>
                                <td>{{ row.fileSizeText }}</td>
                            </tr>
                        </tbody>
                    </table>
                </template>
            </jb-collapse-item>
            <jb-collapse-item
                v-if="isShowServerFile"
                :active="activeResult"
                name="server">
                <span class="collapse-title">{{ $t('template.已选择') }}<span class="number">{{ serverFileCount }}</span>{{ $t('template.个服务器文件') }}</span>
                <template #content>
                    <table class="file-table">
                        <thead>
                            <th>{{ $t('template.文件路径') }}</th>
                            <th>{{ $t('template.服务器列表') }}</th>
                            <th>{{ $t('template.服务器账号') }}</th>
                        </thead>
                        <tbody>
                            <tr
                                v-for="(row, index) in serverFileList"
                                :key="index"
                                :class="checkRowClass(row)">
                                <td>
                                    <div
                                        v-bk-tooltips="row.fileLocationText"
                                        class="file-path-text">
                                        {{ row.fileLocationText }}
                                    </div>
                                </td>
                                <td :class="checkDiffClass(row, 'host')">
                                    <file-source-server
                                        :last-host="lastServerList[index].host"
                                        :pre-host="preServerList[index].host" />
                                </td>
                                <td :class="checkDiffClass(row, 'account')">
                                    {{ generatorAccountAlias(row.account) }}
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </template>
            </jb-collapse-item>
        </bk-collapse>
    </div>
</template>
<script>
    import _ from 'lodash';

    import {
        findParent,
    } from '@utils/vdom';

    import JbCollapseItem from '@components/jb-collapse-item';

    import SourceFileVO from '@domain/variable-object/source-file';

    import {
        findStep,
    } from '../../common/utils';

    import FileSourceServer from './file-source-server';

    export default {
        name: '',
        components: {
            JbCollapseItem,
            FileSourceServer,
        },
        props: {
            id: {
                type: Number,
                required: true,
            },
            data: {
                type: Array,
                default: () => [],
            },
            account: {
                type: Array,
                default: () => [],
            },
        },
        data () {
            return {
                activeResult: [
                    'local',
                    'server',
                ],
                localFileCount: 0,
                serverFileCount: 0,
                localFileList: [],
                serverFileList: [],
                localFileDiff: {},
                serverFileDiff: {},
            };
        },
        computed: {
            isShowLocalFile () {
                return this.localFileList.length > 0;
            },
            isShowServerFile () {
                return this.serverFileList.length > 0;
            },
        },
        created () {
            this.preServerList = [];
            this.lastServerList = [];
            this.checkDiff();
        },
        methods: {
            generatorAccountAlias (accountId) {
                const account = this.account.find(_ => _.id === accountId);
                if (!account) {
                    return '';
                }
                return account.alias;
            },
            checkDiff () {
                const dataSourceParent = findParent(this, 'SyncPlanStep2');
                
                const currentPlanStep = findStep(dataSourceParent.planStepList, this.id);
                const currentTemplateStep = findStep(dataSourceParent.templateStepList, this.id);

                const templateLocalFileList = [];
                const templateServerFileList = [];
                const planLocalFileList = [];
                const planServerFileList = [];

                // 服务器文件、本地文件分开处理
                const currentTemplateFileSouceList = currentTemplateStep.fileStepInfo.fileSourceList;
                currentTemplateFileSouceList.forEach((item) => {
                    const fileItem = new SourceFileVO(item);
                    if (fileItem.isServerFile) {
                        templateServerFileList.push(fileItem);
                    } else {
                        templateLocalFileList.push(fileItem);
                    }
                });
                const currentPlanFileSouceList = currentPlanStep.fileStepInfo.fileSourceList;
                currentPlanFileSouceList.forEach((item) => {
                    const fileItem = new SourceFileVO(item);
                    if (fileItem.isServerFile) {
                        planServerFileList.push(fileItem);
                    } else {
                        planLocalFileList.push(fileItem);
                    }
                });

                // 不同类型文件个数
                // 同步前展示执行方案中数量
                // 同步后展示作业模板中的数量
                const stepParent = findParent(this, 'DiffTaskStep');
                if (stepParent.type === 'sync-after') {
                    this.localFileCount = templateLocalFileList.length;
                    this.serverFileCount = templateServerFileList.length;
                } else {
                    this.localFileCount = planLocalFileList.length;
                    this.serverFileCount = planServerFileList.length;
                }
                
                // 如果步骤是新建步骤，不需要执行diff过程
                if (!dataSourceParent.stepDiff[this.id]
                    || (dataSourceParent.stepDiff[this.id]
                        && dataSourceParent.stepDiff[this.id].type === 'new')) {
                    this.localFileList = Object.freeze(templateLocalFileList);
                    this.localFileDiff = Object.freeze({});
                    this.preServerList = Object.freeze(templateServerFileList.map(_ => ({})));
                    this.lastServerList = Object.freeze(templateServerFileList);
                    this.serverFileDiff = Object.freeze({});
                    return;
                }
                
                const patchServerFile = (pre, last) => {
                    const keys = [
                        'host',
                        'account',
                    ];
                    const result = {};
                    keys.forEach((key) => {
                        result[key] = JSON.stringify(pre[key]) === JSON.stringify(last[key]) ? '' : 'changed';
                    });
                    return result;
                };
                // diff 本地文件
                // 作业模板中的本地文件和执行方案中的本地文件对比
                // 处理本地文件（文件名唯一）
                const localFileDiff = {};
                const localFileList = [];
                templateLocalFileList.forEach((currentTemplateFile) => {
                    localFileList.push(currentTemplateFile);
                    localFileDiff[currentTemplateFile.realId] = 'new';
                });
                let deleteLocalFile = null;
                let insertLocalIndex = 0;
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < planLocalFileList.length; i++) {
                    const currentFile = planLocalFileList[i];
                    if (localFileDiff[currentFile.realId]) {
                        localFileDiff[currentFile.realId] = 'same';
                        insertLocalIndex += 1;
                        if (deleteLocalFile) {
                            const index = _.findIndex(localFileList, _ => _.realId === currentFile.realId);
                            localFileList.splice(index, 0, deleteLocalFile);
                            deleteLocalFile = null;
                        }
                        continue;
                    }
                    if (deleteLocalFile) {
                        insertLocalIndex += 1;
                        localFileList.splice(insertLocalIndex, 0, deleteLocalFile);
                    }
                    localFileDiff[currentFile.realId] = 'delete';
                    deleteLocalFile = currentFile;
                }
                if (deleteLocalFile) {
                    insertLocalIndex += 1;
                    localFileList.splice(insertLocalIndex, 0, deleteLocalFile);
                }

                // diff 服务器文件
                // 作业模板中的服务器文件和执行方案中的服务器文件对比
                // 处理服务器文件（relaId会重复）
                const serverFileCacheByKey = {};
                const serverFileDiff = {};
                const serverFileList = [];
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < templateServerFileList.length; i++) {
                    const currentFile = templateServerFileList[i];
                    if (!serverFileCacheByKey[currentFile.realId]) {
                        serverFileCacheByKey[currentFile.realId] = [
                            currentFile,
                        ];
                    } else {
                        serverFileCacheByKey[currentFile.realId].push(currentFile);
                        // 为同名文件生成一个唯一key
                        const sameFileKey = `${currentFile.realId}_${Math.random()}_${Math.random()}`;
                        currentFile.sameFileKey = sameFileKey;
                        serverFileDiff[sameFileKey] = {
                            type: 'new',
                        };
                    }
                    serverFileList.push(currentFile);
                    serverFileDiff[currentFile.realId] = {
                        type: 'new',
                    };
                }
                let deleteServerFile = null;
                let insertServereIndex = 0;
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < planServerFileList.length; i++) {
                    const currentFile = planServerFileList[i];
                    if (serverFileDiff[currentFile.realId]) {
                        insertServereIndex += 1;
                        if (deleteServerFile) {
                            const index = _.findIndex(serverFileList, _ => _.realId === currentFile.realId);
                            serverFileList.splice(index, 0, deleteServerFile);
                            deleteServerFile = null;
                        }
                        const preFile = serverFileCacheByKey[currentFile.realId].shift();
                        if (preFile) {
                            if (preFile.sameFileKey) {
                                serverFileDiff[preFile.sameFileKey] = {
                                    type: 'different',
                                    value: patchServerFile(preFile, currentFile),
                                };
                                currentFile.sameFileKey = preFile.sameFileKey;
                            } else {
                                serverFileDiff[currentFile.realId] = {
                                    type: 'different',
                                    value: patchServerFile(preFile, currentFile),
                                };
                            }
                        } else {
                            // 在模板中不存在，表示被删掉了
                            const index = _.findIndex(serverFileList, _ => _.realId === currentFile.realId);
                            serverFileList.splice(index + 1, 0, deleteServerFile);
                            deleteServerFile = null;
                        }
                        continue;
                    }
                    if (deleteServerFile) {
                        insertServereIndex += 1;
                        serverFileList.splice(insertServereIndex, 0, deleteServerFile);
                    }
                    // 被删掉了
                    serverFileDiff[currentFile.realId] = {
                        type: 'delete',
                    };
                    deleteServerFile = currentFile;
                }
                // 最后一个被删除
                if (deleteServerFile) {
                    insertServereIndex += 1;
                    serverFileList.splice(insertServereIndex, 0, deleteServerFile);
                }
                
                // 同步后的服务器文件展示列表
                this.lastServerList = serverFileList;
                if (stepParent.type === 'sync-after') {
                    this.localFileList = Object.freeze(localFileList);
                    this.localFileDiff = Object.freeze(localFileDiff);
                    this.serverFileList = Object.freeze(serverFileList);
                    this.serverFileDiff = Object.freeze(serverFileDiff);
                }

                // 同步前的服务器展示列表
                const preLocalFileList = localFileList.map((fileItem) => {
                    if (localFileDiff[fileItem.realId] === 'new') {
                        return {};
                    }
                    return fileItem;
                });
                const preLocalFileDiff = Object.keys(localFileDiff).reduce((result, key) => {
                    if (localFileDiff[key].type === 'different') {
                        result[key] = localFileDiff[key];
                    }
                    return result;
                }, {});
                const preServerFileList = [];
                // 会存在重名文件，用于记录已处理项
                const invalidIndexMap = {};
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < serverFileList.length; i++) {
                    const currentFile = serverFileList[i];
                    // 文件是作业模板中新添加的，添加一个空白占位符
                    if (serverFileDiff[currentFile.sameFileKey] && serverFileDiff[currentFile.sameFileKey].type === 'new') {
                        preServerFileList.push({});
                        continue;
                    }
                    if (serverFileDiff[currentFile.realId].type === 'new') {
                        preServerFileList.push({});
                        continue;
                    }
                    // eslint-disable-next-line no-plusplus
                    for (let j = 0; j < planServerFileList.length; j++) {
                        const searchFile = planServerFileList[j];
                        if (invalidIndexMap[j]) {
                            continue;
                        }
                        if ((currentFile.sameFileKey && currentFile.sameFileKey === searchFile.sameFileKey)
                            || (currentFile.realId === searchFile.realId)) {
                            invalidIndexMap[j] = true;
                            preServerFileList.push(searchFile);
                            break;
                        }
                    }
                }
                const preServerFileDiff = Object.keys(serverFileDiff).reduce((result, key) => {
                    if (serverFileDiff[key].type === 'different') {
                        result[key] = serverFileDiff[key];
                    }
                    return result;
                }, {});
                if (stepParent.type === 'sync-before') {
                    this.localFileList = Object.freeze(preLocalFileList);
                    this.localFileDiff = Object.freeze(preLocalFileDiff);
                    this.serverFileList = Object.freeze(preServerFileList);
                    this.serverFileDiff = Object.freeze(preServerFileDiff);
                }
                this.preServerList = preServerFileList;
            },
            checkRowClass (row) {
                if (!this.serverFileDiff[row.realId]) {
                    return '';
                }
                if (this.serverFileDiff[row.sameFileKey]) {
                    return this.serverFileDiff[row.sameFileKey].type;
                }
                return this.serverFileDiff[row.realId].type;
            },
            checkDiffClass (row, key) {
                if (!this.serverFileDiff[row.realId]) {
                    return '';
                }
                if (this.serverFileDiff[row.realId].type !== 'different') {
                    return '';
                }
                return this.serverFileDiff[row.realId].value[key] || '';
            },
        },
    };
</script>
<style lang='postcss'>
.sync-step-server-file {
    flex: 1;

    .bk-collapse-item-header {
        display: flex;
        align-items: center;
        padding-left: 23px;

        .collapse-title {
            padding-left: 23px;
        }
    }

    .bk-table-empty-block {
        display: none;
    }

    .number {
        padding: 0 4px;
    }

    .file-table {
        width: 100%;
        background: #fff;
        table-layout: fixed;

        th {
            font-weight: normal;
            color: #313238;
            border-bottom: 1px solid #dcdee5;
        }

        td {
            color: #63656e;

            .file-path-text {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }

        th,
        td {
            height: 42px;
            padding-left: 16px;
            font-size: 12px;
            line-height: 16px;
            text-align: left;

            &:first-child {
                padding-left: 60px;
            }
        }

        tr:nth-child(n+2) {
            td {
                border-top: 1px solid #dcdee5;
            }
        }

        tr.different {
            td.changed {
                background: #fddfcb;
            }
        }

        tr.delete {
            td {
                color: #c4c6cc !important;
                text-decoration: line-through;
            }
        }

        tr.new {
            td:first-child {
                position: relative;

                &::before {
                    position: absolute;
                    top: 50%;
                    width: 24px;
                    height: 16px;
                    margin-left: -32px;
                    font-size: 12px;
                    line-height: 16px;
                    color: #fff;
                    text-align: center;
                    background: #f0c581;
                    border-radius: 2px;
                    content: "new";
                    transform: translateY(-50%);
                }
            }
        }
    }
}
</style>
