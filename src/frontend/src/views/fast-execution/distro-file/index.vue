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
    <div
        v-bkloading="{ isLoading }"
        class="push-file-page">
        <resize-layout
            class="push-file-content"
            :right-fixed="true"
            :right-width="366">
            <smart-action offset-target="bk-form-content">
                <jb-form
                    ref="pushFileForm"
                    v-test="{ type: 'form', value: 'pushFile' }"
                    class="push-file-form"
                    :model="formData">
                    <card-layout :title="$t('execution.基本信息')">
                        <item-factory
                            field="name"
                            :form-data="formData"
                            :label="$t('execution.任务名称')"
                            name="stepName"
                            :placeholder="$t('execution.取一个便于记忆的任务名，方便后续在历史记录中快速定位...')"
                            @on-change="handleChange" />
                        <item-factory
                            field="timeout"
                            :form-data="formData"
                            name="timeout"
                            @on-change="handleChange" />
                        <item-factory
                            field="uploadSpeedLimit"
                            :form-data="formData"
                            :label="$t('execution.上传限速')"
                            name="speedLimit"
                            @on-change="handleChange" />
                        <item-factory
                            field="downloadSpeedLimit"
                            :form-data="formData"
                            :label="$t('execution.下载限速')"
                            name="speedLimit"
                            @on-change="handleChange" />
                    </card-layout>
                    <card-layout :title="$t('execution.文件来源')">
                        <item-factory
                            field="fileSourceList"
                            :form-data="formData"
                            name="sourceFileOfExecution"
                            @on-change="handleChange" />
                    </card-layout>
                    <card-layout
                        style="margin-bottom: 20px;"
                        :title="$t('execution.传输目标')">
                        <item-factory
                            ref="targetPath"
                            field="path"
                            :form-data="formData"
                            name="targetPath"
                            :tips-placement="targetPathTipsPlacement"
                            @on-change="handleChange" />
                        <item-factory
                            field="transferMode"
                            :form-data="formData"
                            name="transferMode"
                            @on-change="handleChange" />
                        <item-factory
                            field="account"
                            :form-data="formData"
                            name="executeAccount"
                            @on-change="handleChange" />
                        <item-factory
                            field="server"
                            :form-data="formData"
                            name="targetServerOfExecution"
                            @on-change="handleChange" />
                        <item-factory
                            enabled-field="rollingEnabled"
                            expr-field="rollingExpr"
                            :form-data="formData"
                            mode-field="rollingMode"
                            name="rolling"
                            @on-change="handleChange"
                            @on-reset="handleReset" />
                    </card-layout>
                </jb-form>
                <template #action>
                    <bk-button
                        v-test="{ type: 'button', value: 'fastPushFileSubmit' }"
                        class="w120 mr10"
                        :loading="isSubmiting"
                        theme="primary"
                        @click="handleSubmit">
                        {{ $t('execution.执行') }}
                    </bk-button>
                    <bk-button
                        v-test="{ type: 'button', value: 'fastPushFileReset' }"
                        @click="handleCancel">
                        {{ $t('execution.重置') }}
                    </bk-button>
                </template>
            </smart-action>
            <div
                id="rollingExprGuide"
                slot="right" />
        </resize-layout>
        <div
            v-if="historyList.length > 0"
            class="execution-history"
            :class="{ active: isShowHistory }">
            <div
                class="toggle-btn"
                @click="handleShowHistory">
                <Icon
                    class="toggle-flag"
                    type="angle-double-left" />
                <div class="recent-result">
                    {{ $t('execution.最近结果') }}
                </div>
            </div>
            <div class="history-content">
                <div
                    v-for="item in historyList"
                    :key="item.id"
                    class="item"
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

    import TaskExecuteService from '@service/task-execute';

    import TaskStepModel from '@model/task/task-step';
    import TaskHostNodeModel from '@model/task-host-node';

    import {
        compareHost,
        detectionSourceFileDupLocation,
        genDefaultName,
    } from '@utils/assist';
    import {
        pushFileHistory,
    } from '@utils/cache-helper';

    import JbForm from '@components/jb-form';
    import ResizeLayout from '@components/resize-layout';
    import CardLayout from '@components/task-step/file/card-layout';
    import ItemFactory from '@components/task-step/file/item-factory';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        // 快速执行name
        name: genDefaultName(I18n.t('execution.快速执行分发文件')),
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
        // 开启滚动
        rollingEnabled: false,
        // 滚动执行配置，编辑时拍平
        // 提交时合并
        // rollingConfig: {
        //     expr: '10%',
        //     mode: 1,
        // }
        rollingExpr: '',
        rollingMode: 1,
    });

    export default {
        name: '',
        components: {
            JbForm,
            CardLayout,
            ItemFactory,
            ResizeLayout,
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
            window.IPInputScope = 'FILE_DISTRIBUTION';
            window.addEventListener('resize', this.calcTargetPathTipsPlacement);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.calcTargetPathTipsPlacement);
                window.IPInputScope = '';
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
                    const {
                        name,
                        rollingEnabled,
                        rollingConfig: {
                            expr: rollingExpr,
                            mode: rollingMode,
                        },
                    } = data.stepInfo;
                    const {
                        downloadSpeedLimit,
                        fileDestination: {
                            account,
                            path,
                            server,
                        },
                        fileSourceList,
                        timeout,
                        transferMode,
                        uploadSpeedLimit,
                    } = data.stepInfo.fileStepInfo;
                    
                    this.formData = {
                        ...this.formData,
                        name,
                        uploadSpeedLimit,
                        downloadSpeedLimit,
                        account,
                        path,
                        server,
                        timeout,
                        fileSourceList,
                        transferMode,
                        rollingEnabled,
                        rollingExpr,
                        rollingMode,
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
             */
            handleChange (field, value) {
                this.formData[field] = value;
            },
            /**
             * @desc 批量更新字段
             * @param {Object} payload 将要更新的字段值
             */
            handleReset (payload) {
                this.formData = {
                    ...this.formData,
                    ...payload,
                };
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
                    // 检测服务器源文件的主机和执行目标服务器主机相同
                    .then(() => new Promise((resolve, reject) => {
                        let sameHost = false;
                        // eslint-disable-next-line no-plusplus
                        for (let i = 0; i < this.formData.fileSourceList.length; i++) {
                            const currentFileSource = this.formData.fileSourceList[i];
                            // 服务器源文件
                            if (currentFileSource.fileType === TaskStepModel.fileStep.TYPE_SERVER) {
                                if (compareHost(this.formData.server, currentFileSource.host)) {
                                    sameHost = true;
                                    break;
                                }
                            }
                        }
                        if (sameHost) {
                            this.$bkInfo({
                                title: I18n.t('execution.源和目标服务器相同'),
                                subTitle: I18n.t('execution.检测到文件传输源和目标服务器是同一批，若是单台建议使用本地 cp 方式效率会更高，请问你是否确定参数无误？'),
                                width: 500,
                                okText: I18n.t('execution.好的，我调整一下'),
                                cancelText: I18n.t('execution.是的，确定无误'),
                                confirmFn: () => {
                                    reject(new Error('execute'));
                                },
                                cancelFn: () => {
                                    resolve();
                                },
                            });
                        } else {
                            resolve();
                        }
                    }))
                    // 检测源文件的同名文件和目录
                    .then(() => new Promise((resolve, reject) => {
                        if (detectionSourceFileDupLocation(this.formData.fileSourceList)) {
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
                        } else {
                            resolve();
                        }
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
                            rollingEnabled,
                            rollingExpr,
                            rollingMode,
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
                            rollingEnabled,
                            rollingConfig: {
                                expr: rollingExpr,
                                mode: rollingMode,
                            },
                        }).then((data) => {
                            window.changeConfirm = false;
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
                    .catch(() => {
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
    @import "@/css/mixins/media";

    html[lang="en-US"] {
        .recent-result {
            margin-top: 6px;
            transform: rotate(90deg);
        }
    }

    .push-file-page {
        .push-file-content {
            height: calc(100vh - 104px);
        }

        .push-file-form {
            padding: 20px 24px 0;

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

        .card-layout {
            margin-bottom: 6px;
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
