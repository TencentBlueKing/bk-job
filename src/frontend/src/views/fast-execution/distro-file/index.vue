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
    <div class="distro-file-page" v-bkloading="{ isLoading }">
        <smart-action offset-target="bk-form-content">
            <jb-form class="push-file-form" ref="pushFileForm" :model="formData">
                <card-layout :title="$t('execution.基本信息')" class="block">
                    <item-factory
                        name="stepName"
                        field="name"
                        :label="$t('execution.任务名称')"
                        :placeholder="$t('execution.取一个便于记忆的任务名，方便后续在历史记录中快速定位...')"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="timeout"
                        field="timeout"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="speedLimit"
                        field="uploadSpeedLimit"
                        :label="$t('execution.上传限速')"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="speedLimit"
                        field="downloadSpeedLimit"
                        :label="$t('execution.下载限速')"
                        :form-data="formData"
                        @on-change="handleChange" />
                </card-layout>
                <card-layout :title="$t('execution.文件来源')" class="block">
                    <item-factory
                        name="sourceFileOfExecution"
                        field="fileSourceList"
                        :form-data="formData"
                        @on-change="handleChange" />
                </card-layout>
                <card-layout :title="$t('execution.传输目标')" class="block" style="margin-bottom: 0;">
                    <item-factory
                        ref="targetPath"
                        name="targetPath"
                        field="path"
                        :tips-placement="targetPathTipsPlacement"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="transferMode"
                        field="transferMode"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="executeAccount"
                        field="account"
                        :form-data="formData"
                        @on-change="handleChange" />
                    <item-factory
                        name="targetServerOfExecution"
                        field="server"
                        :form-data="formData"
                        @on-change="handleChange" />
                </card-layout>
            </jb-form>
            <template #action>
                <bk-button
                    class="w120 mr10"
                    theme="primary"
                    :loading="isSubmiting"
                    @click="handleSubmit">
                    {{ $t('execution.执行') }}
                </bk-button>
                <bk-button @click="handleCancel">{{ $t('execution.重置') }}</bk-button>
            </template>
        </smart-action>
        <div v-if="historyList.length > 0" class="execution-history" :class="{ active: isShowHistory }">
            <div class="toggle-btn" @click="handleShowHistory">
                <Icon class="toggle-flag" type="angle-double-left" />
                <div class="recent-result">{{ $t('execution.最近结果') }}</div>
            </div>
            <div class="history-content">
                <div
                    v-for="item in historyList"
                    class="item"
                    :key="item.id"
                    @click="handleGoHistoryDetail(item)">
                    {{ item.name }}
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';
    import {
        mapState,
    } from 'vuex';
    import I18n from '@/i18n';
    import TaskExecuteService from '@service/task-execute';
    import TaskHostNodeModel from '@model/task-host-node';
    import JbForm from '@components/jb-form';
    import CardLayout from '@components/task-step/file/card-layout';
    import ItemFactory from '@components/task-step/file/item-factory';
    import {
        getScriptName,
    } from '@utils/assist';
    import {
        pushFileHistory,
    } from '@utils/cache-helper';

    const getDefaultData = () => ({
        // 快速执行name
        name: getScriptName(I18n.t('execution.快速执行分发文件')),
        // 源文件列表
        fileSourceList: [],
        // 超时
        timeout: 7200,
        // 上传文件限速
        uploadSpeedLimit: 0,
        // 传输模式： 1 - 严谨模式； 2 - 强制模式；3 - 安全模式
        transferMode: 2,
        // 下载文件限速
        downloadSpeedLimit: 0,
        // 执行账号
        account: '',
        // 目标路径
        path: '',
        // 目标服务器
        server: new TaskHostNodeModel({}),
    });

    export default {
        name: '',
        components: {
            JbForm,
            CardLayout,
            ItemFactory,
        },
        data () {
            return {
                isLoading: false,
                formData: getDefaultData(),
                isSubmiting: false,
                historyList: [],
                isShowHistory: false,
                targetPathTipsPlacement: '',
            };
        },
        computed: {
            ...mapState('distroFile', {
                isEditNewSourceFile: state => state.isEditNewSourceFile,
            }),
        },
        created () {
            this.init();
            this.calcTargetPathTipsPlacement();
        },
        mounted () {
            window.addEventListener('resize', this.calcTargetPathTipsPlacement);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcTargetPathTipsPlacement);
            });
        },
        methods: {
            /**
             * @desc 执行历史重做
             *
             * 通过任务taskInstanceId获取任务的详情
             */
            fetchData () {
                this.$request(TaskExecuteService.fetchTaskInstance({
                    id: this.taskInstanceId,
                }), () => {
                    this.isLoading = true;
                }).then((data) => {
                    const { stepInfo } = data;
                    const {
                        downloadSpeedLimit,
                        fileDestination,
                        fileSourceList,
                        timeout,
                        transferMode,
                        uploadSpeedLimit,
                    } = stepInfo.fileStepInfo;
                    const {
                        account,
                        path,
                        server,
                    } = fileDestination;
                    this.formData = {
                        ...this.formData,
                        name: stepInfo.name,
                        uploadSpeedLimit,
                        downloadSpeedLimit,
                        account,
                        path,
                        server,
                        timeout,
                        fileSourceList,
                        transferMode,
                    };
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 初始化逻辑
             *
             * 1，读取执行历史缓存
             * 2，解析url
             *     -- 路由回退重做
             *     -- 执行历史指定任务重做
             */
            init () {
                this.taskInstanceId = parseInt(this.$route.params.taskInstanceId, 10);
                this.timeTravel();

                const { from } = this.$route.query;
                // 步骤执行详情历史编辑——从历史缓存数据中查找对应数据的formData
                if (from === 'historyStep') {
                    let historyRecord = null;
                    if (this.taskInstanceId) {
                        // 有指定taskInstanceId
                        historyRecord = _.find(this.historyList, _ => _.taskInstanceId === this.taskInstanceId);
                    } else {
                        // 没有指定taskInstanceId，默认取第一个
                        historyRecord = _.head(this.historyList);
                    }
                    // 历史缓存中有数据使用缓存数据
                    if (historyRecord) {
                        this.formData = historyRecord.formData;
                        return;
                    }
                }
                // 执行历史——通过taskInstanceId重做
                if (this.taskInstanceId > 0) {
                    this.fetchData();
                }
            },
            calcTargetPathTipsPlacement: _.throttle(function () {
                this.targetPathTipsPlacement = window.innerWidth > 1650 ? 'right-start' : 'top';
            }, 60),
            /**
             * @desc 回溯执行历史
             *
             */
            timeTravel () {
                this.historyList = Object.freeze(pushFileHistory.getItem());
            },
            /**
             * @desc 执行记录缓存
             * @param {Array} history 执行历史记录
             *
             */
            pushLocalStorage (history) {
                const historyList = pushFileHistory.getItem();
                historyList.unshift(history);
                pushFileHistory.setItem(historyList);
            },
            /**
             * @desc 展开执行历史列表
             *
             */
            handleShowHistory () {
                this.isShowHistory = !this.isShowHistory;
            },
            /**
             * @desc 查看执行历史记录任务详情
             *
             */
            handleGoHistoryDetail (payload) {
                this.$router.push({
                    name: 'historyStep',
                    params: {
                        taskInstanceId: payload.taskInstanceId,
                    },
                    query: {
                        stepInstanceId: payload.stepInstanceId,
                        from: 'fastPushFile',
                    },
                });
            },
            /**
             * @desc 表单值更新
             * @param {String} field 字段名
             * @param {Any} value  字段最新值
             *
             */
            handleChange (field, value) {
                this.formData[field] = value;
            },
            /**
             * @desc 执行
             */
            handleSubmit () {
                this.isSubmiting = true;
                this.$refs.pushFileForm.validate()
                    // 检测没有保存的源文件
                    .then(() => new Promise((resolve, reject) => {
                        if (!this.isEditNewSourceFile) {
                            return resolve();
                        }
                        this.$bkInfo({
                            title: I18n.t('execution.您有未保存的源文件'),
                            type: 'warning',
                            okText: I18n.t('execution.继续执行'),
                            cancelText: I18n.t('execution.去保存'),
                            confirmFn: () => {
                                setTimeout(() => {
                                    resolve();
                                }, 300);
                            },
                            cancelFn: () => {
                                setTimeout(() => {
                                    reject(new Error('save'));
                                }, 300);
                            },
                        });
                    }))
                    // 检测源文件的同名文件和目录
                    .then(() => new Promise((resolve, reject) => {
                        const fileLocationMap = {};
                        const pathReg = /([^/]+\/?)\*?$/;
                        let isDouble = false;
                        // 路径中以 * 结尾表示分发所有文件，可能和分发具体文件冲突
                        let hasDirAllFile = false;
                        let hasFile = false;
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.formData.fileSourceList.length; i++) {
                            const currentFileSource = this.formData.fileSourceList[i];
                            // eslint-disable-next-line no-plusplus
                            for (let j = 0; j < currentFileSource.fileLocation.length; j++) {
                                const currentFileLocation = currentFileSource.fileLocation[j];
                                // 分发所有文件
                                if (/\*$/.test(currentFileLocation)) {
                                    hasDirAllFile = true;
                                    if (hasFile) {
                                        isDouble = true;
                                        break;
                                    }
                                    continue;
                                }
                                // 分发具体的文件
                                if (!/(\/|(\/\*))$/.test(currentFileLocation)) {
                                    hasFile = true;
                                    if (hasDirAllFile) {
                                        isDouble = true;
                                        break;
                                    }
                                }
                                const pathMatch = currentFileLocation.match(pathReg);
                                if (pathMatch) {
                                    if (fileLocationMap[pathMatch[1]]) {
                                        isDouble = true;
                                        break;
                                    } else {
                                        fileLocationMap[pathMatch[1]] = 1;
                                    }
                                }
                            }
                        }
                            
                        if (!isDouble) {
                            resolve();
                            return;
                        }
                        // 有重名目录和文件
                        this.$bkInfo({
                            title: I18n.t('execution.源文件可能出现同名'),
                            subTitle: I18n.t('execution.多文件源传输场景下容易出现同名文件覆盖的问题，你可以在目标路径中使用 [源服务器IP] 的变量来尽可能规避风险。'),
                            okText: I18n.t('execution.好的，我调整一下'),
                            cancelText: I18n.t('execution.已知悉，确定执行'),
                            closeIcon: false,
                            width: 500,
                            confirmFn: () => {
                                // 聚焦到目标路径输入框
                                this.$refs.targetPath.$el.scrollIntoView();
                                this.$refs.targetPath.$el.querySelector('.bk-form-input').focus();
                                reject(new Error('transferMode change'));
                            },
                            cancelFn: () => {
                                resolve();
                            },
                        });
                    }))
                    .then(() => {
                        const {
                            name,
                            timeout,
                            uploadSpeedLimit,
                            downloadSpeedLimit,
                            transferMode,
                            fileSourceList,
                            account,
                            path,
                            server,
                        } = this.formData;
                        
                        return TaskExecuteService.pushFile({
                            name,
                            uploadSpeedLimit: parseInt(uploadSpeedLimit, 10),
                            downloadSpeedLimit: parseInt(downloadSpeedLimit, 10),
                            timeout: parseInt(timeout, 10),
                            fileSourceList,
                            transferMode,
                            fileDestination: {
                                account,
                                path,
                                server,
                            },
                        }).then((data) => {
                            window.changeAlert = false;
                            this.$router.push({
                                name: 'historyStep',
                                params: {
                                    taskInstanceId: data.taskInstanceId,
                                },
                                query: {
                                    stepInstanceId: data.stepInstanceId,
                                    from: 'fastPushFile',
                                },
                            });
                            this.pushLocalStorage({
                                id: Date.now(),
                                name,
                                taskInstanceId: data.taskInstanceId,
                                stepInstanceId: data.stepInstanceId,
                                formData: this.formData,
                            });
                        });
                    })
                    .catch((error) => {
                        console.log(error.message);
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 重置
             */
            handleCancel () {
                this.$refs.pushFileForm.clearError();
                this.formData = getDefaultData();
            },
        },
    };
</script>
<style lang='postcss'>
    @import '@/css/mixins/media';

    html[lang='en-US'] {
        .recent-result {
            margin-top: 6px;
            transform: rotate(90deg);
        }
    }

    .distro-file-page {
        .push-file-form {
            margin-bottom: 24px;

            .card-box {
                margin-bottom: 6px;
            }

            .form-item-content {
                width: 500px;

                @media (--small-viewports) {
                    width: 500px;
                }

                @media (--medium-viewports) {
                    width: 560px;
                }

                @media (--large-viewports) {
                    width: 620px;
                }

                @media (--huge-viewports) {
                    width: 680px;
                }
            }
        }

        .execution-history {
            position: fixed;
            top: 127px;
            right: 0;
            z-index: 99;
            font-size: 12px;
            line-height: 30px;
            color: #c4c6cc;
            background: #63656e;
            border-bottom-left-radius: 2px;
            transform: translateX(100%);
            transition: all 0.35s;
            user-select: none;

            &.active {
                transform: translateX(0);

                .toggle-flag {
                    transform: rotateZ(180deg);
                }
            }

            .toggle-btn {
                position: absolute;
                top: 0;
                left: -22px;
                display: flex;
                width: 22px;
                height: 88px;
                line-height: 13px;
                color: #dcdee5;
                text-align: center;
                cursor: pointer;
                background: #63656e;
                border-right: 1px solid #757783;
                border-bottom-left-radius: 8px;
                border-top-left-radius: 8px;
                flex-direction: column;
                justify-content: center;

                .toggle-flag {
                    margin-bottom: 5px;
                    color: #979ba5;
                    transition: all 0.2s;
                }
            }

            .history-content {
                display: flex;
                min-height: 90px;
                padding: 12px 0;
                flex-direction: column;
                justify-content: center;
            }

            .item {
                padding-right: 16px;
                padding-left: 16px;
                cursor: pointer;
                transition: all 0.15s;

                &:hover {
                    color: #fff;
                    background: #4f515a;
                }
            }
        }
    }
</style>
