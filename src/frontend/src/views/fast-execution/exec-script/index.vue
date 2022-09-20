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
        class="exec-script-page">
        <resize-layout
            class="exec-scipt-content"
            :right-fixed="true"
            :right-width="366">
            <smart-action offset-target="bk-form-content">
                <jb-form
                    ref="execScriptForm"
                    v-test="{ type: 'form', value: 'executeScript' }"
                    class="fast-execution-script-form"
                    :model="formData">
                    <item-factory
                        field="name"
                        :form-data="formData"
                        :label="$t('execution.脚本名称')"
                        name="scriptName"
                        :placeholder="$t('execution.取一个便于记忆的任务名，方便后续在历史记录中快速定位...')"
                        @on-change="handleChange" />
                    <item-factory
                        content-field="content"
                        :form-data="formData"
                        language-field="scriptLanguage"
                        name="scriptSourceOfExecution"
                        script-id-field="scriptId"
                        script-source-field="scriptSource"
                        script-version-id-field="scriptVersionId"
                        @on-reset="handleReset" />
                    <item-factory
                        :key="reset"
                        content-field="content"
                        :form-data="formData"
                        language-field="scriptLanguage"
                        name="scriptContent"
                        script-source-field="scriptSource"
                        @on-change="handleChange" />
                    <item-factory
                        :form-data="formData"
                        name="scriptParam"
                        param-field="scriptParam"
                        secure-field="secureParam"
                        @on-change="handleChange" />
                    <item-factory
                        field="timeout"
                        :form-data="formData"
                        name="scriptTimeout"
                        @on-change="handleChange" />
                    <item-factory
                        field="account"
                        :form-data="formData"
                        name="scriptAccount"
                        script-language-field="scriptLanguage"
                        @on-change="handleChange" />
                    <item-factory
                        field="targetServers"
                        :form-data="formData"
                        name="executeTargetOfExecution"
                        @on-change="handleChange" />
                    <item-factory
                        enabled-field="rollingEnabled"
                        expr-field="rollingExpr"
                        :form-data="formData"
                        mode-field="rollingMode"
                        name="rolling"
                        @on-change="handleChange"
                        @on-reset="handleReset" />
                </jb-form>
                <template #action>
                    <div style="display: flex;">
                        <bk-button
                            v-test="{ type: 'button', value: 'fastExecuteScriptSubmit' }"
                            class="w120 mr10"
                            :loading="isSubmiting"
                            theme="primary"
                            @click="handleSubmit">
                            {{ $t('execution.执行') }}
                        </bk-button>
                        <bk-button
                            v-test="{ type: 'button', value: 'fastExecuteScriptCancel' }"
                            @click="handleCancel">
                            {{ $t('execution.重置') }}
                        </bk-button>
                    </div>
                </template>
            </smart-action>
            <div
                id="rollingExprGuide"
                slot="right" />
        </resize-layout>
        <div
            v-if="historyList.length > 0"
            class="submit-history-record"
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

    import TaskExecuteService from '@service/task-execute';

    import TaskStepModel from '@model/task/task-step';
    import TaskHostNodeModel from '@model/task-host-node';

    import {
        genDefaultName,
        scriptErrorConfirm,
    } from '@utils/assist';
    import {
        debugScriptCache,
        execScriptHistory,
    } from '@utils/cache-helper';

    import ResizeLayout from '@components/resize-layout';
    import ItemFactory from '@components/task-step/script/item-factory';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        isScriptContentLoading: false,
        // 快速执行name
        name: genDefaultName(I18n.t('execution.快速执行脚本')),
        // 脚本来源
        scriptSource: TaskStepModel.scriptStep.TYPE_SOURCE_LOCAL,
        // 脚本类型，默认shell
        scriptLanguage: 1,
        // 引用脚本的id
        scriptId: '',
        // 引用脚本的版本id
        scriptVersionId: '',
        // 脚本内容
        content: '',
        // 脚本参数
        scriptParam: '',
        // 敏感参数 0-关闭 1-开启
        secureParam: 0,
        // 超时
        timeout: 7200,
        // 账号
        account: '',
        // 目标服务器
        targetServers: new TaskHostNodeModel({}),
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
            ItemFactory,
            ResizeLayout,
        },
        data () {
            return {
                reset: 0,
                isLoading: false,
                formData: getDefaultData(),
                historyList: [],
                isSubmiting: false,
                isShowHistory: false,
                ipSelectorData: {},
            };
        },
        created () {
            this.parseUrlParams();
        },
        mounted () {
            window.IPInputScope = 'SCRIPT_EXECUTE';
        },
        /**
         * @desc 销毁时清空脚本调试的数据
         */
        beforeDestroy () {
            debugScriptCache.clearItem();
            window.IPInputScope = '';
        },
        methods: {
            handleIpChange (value) {
                this.ipSelectorData = value;
            },
            /**
             * @desc 重做时获取任务详细信息
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
                        account,
                        content,
                        ignoreError,
                        scriptId,
                        scriptLanguage,
                        scriptParam,
                        scriptSource,
                        scriptVersionId,
                        secureParam,
                        timeout,
                        executeTarget,
                    } = data.stepInfo.scriptStepInfo;

                    this.formData = {
                        ...this.formData,
                        name,
                        account,
                        content,
                        ignoreError,
                        scriptId,
                        scriptLanguage,
                        scriptParam,
                        scriptSource,
                        scriptVersionId,
                        secureParam,
                        timeout,
                        targetServers: executeTarget,
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
             * @desc 解析url参数
             *
             * 有多种类型的url风格
             * 1，脚本管理，调试脚本内容
             * 2，从步骤执行详情页面回退，回填执行的任务数据
             * 3，执行列表重做任务
             * 4，脚本管理，执行指定版本脚本
             */
            parseUrlParams () {
                const { model } = this.$route.query;
                // 调试脚本模式
                if (model === 'debugScript') {
                    const debugScriptContent = debugScriptCache.getItem();
                    if (debugScriptContent) {
                        this.formData.scriptLanguage = debugScriptContent.type;
                        this.formData.content = debugScriptContent.content;
                    }
                    return;
                }
                this.taskInstanceId = parseInt(this.$route.params.taskInstanceId, 10);
                this.scriptVersionId = parseInt(this.$route.params.scriptVersionId, 10);
                const { from } = this.$route.query;
                
                this.timeTravel();

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
                    return;
                }
                
                // 执行指定版本的脚本
                if (this.scriptVersionId > 0) {
                    this.formData.scriptVersionId = this.scriptVersionId;
                    this.formData.scriptSource = TaskStepModel.scriptStep.TYPE_SOURCE_PUBLIC;
                }
            },
            /**
             * @desc 读取执行历史
             */
            timeTravel () {
                this.historyList = Object.freeze(execScriptHistory.getItem());
            },
            /**
             * @desc 缓存执行历史
             */
            pushLocalStorage (history) {
                const historyList = execScriptHistory.getItem();
                historyList.unshift(history);
                execScriptHistory.setItem(historyList);
            },
            /**
             * @desc 展开执行历史面板
             */
            handleShowHistory () {
                this.isShowHistory = !this.isShowHistory;
            },
            /**
             * @desc 定位到历史执行任务详情
             * @param {Object} task 历史执行任务数据
             */
            handleGoHistoryDetail (task) {
                this.$router.push({
                    name: 'historyStep',
                    params: {
                        taskInstanceId: task.taskInstanceId,
                    },
                    query: {
                        stepInstanceId: task.stepInstanceId,
                        from: 'fastExecuteScript',
                    },
                });
            },
            /**
             * @desc 表单字段更新
             * @param {String} field 字段名
             * @param {Any} value 字段值
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
             * @desc 执行任务
             */
            handleSubmit () {
                this.isSubmiting = true;
                this.$refs.execScriptForm.validate()
                    .then(scriptErrorConfirm)
                    .then(() => {
                        const {
                            name,
                            scriptSource,
                            scriptId,
                            scriptVersionId,
                            scriptLanguage,
                            content,
                            scriptParam,
                            secureParam,
                            timeout,
                            account,
                            targetServers,
                            rollingEnabled,
                            rollingExpr,
                            rollingMode,
                        } = this.formData;

                        const params = {
                            name,
                            scriptSource,
                            scriptId,
                            scriptVersionId,
                            scriptLanguage,
                            content,
                            scriptParam,
                            secureParam,
                            timeout,
                            account,
                            targetServers,
                            rollingEnabled,
                            rollingConfig: {
                                expr: rollingExpr,
                                mode: rollingMode,
                            },
                        };
                        // 重做时需要带上taskInstanceId，主要处理敏感参数
                        // 标记是重做任务
                        if (this.taskInstanceId) {
                            params.taskInstanceId = this.taskInstanceId;
                            params.isRedoTask = true;
                        }
                        
                        return TaskExecuteService.executeScript(params)
                            .then((data) => {
                                window.changeConfirm = false;
                                this.$router.push({
                                    name: 'historyStep',
                                    params: {
                                        taskInstanceId: data.taskInstanceId,
                                    },
                                    query: {
                                        stepInstanceId: data.stepInstanceId,
                                        from: 'fastExecuteScript',
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
             * @desc 取消、重置操作数据
             */
            handleCancel () {
                this.$refs.execScriptForm.clearError();
                this.formData = getDefaultData();
                this.reset += 1;
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

    .exec-script-page {
        .exec-scipt-content {
            height: calc(100vh - 104px);
        }

        .fast-execution-script-form {
            padding: 20px 24px 0;
            margin-bottom: 10px;

            .bk-select {
                background: #fff;
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

        .submit-history-record {
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
