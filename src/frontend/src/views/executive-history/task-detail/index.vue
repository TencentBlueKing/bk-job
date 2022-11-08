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
        :key="taskInstanceId"
        v-bkloading="{ isLoading }"
        class="executive-history-task">
        <div class="step-list">
            <task-step-start key="start" />
            <task-step
                v-for="(step, index) in formData.stepExecution"
                :key="index"
                :data="step"
                @on-select="handleSelectStep"
                @on-update="handleUpdateStepStatus" />
            <task-step-end
                key="end"
                :finished="formData.taskExecution.isSuccess" />
        </div>
        <div class="step-list-action">
            <back-top
                :fixed="false"
                size="small" />
            <execution-process
                v-if="!isLoading"
                class="execution-process"
                :current="formData.currentStepRunningOrder"
                :total="formData.totalStep" />
        </div>
        <div class="task-action">
            <auth-component
                auth="task_instance/redo"
                :resource-id="taskInstanceId">
                <div
                    v-bk-tooltips.bottom="$t('history.去重做')"
                    class="action-btn"
                    @click="handleGoRetry">
                    <Icon type="redo" />
                </div>
                <div
                    slot="forbid"
                    class="action-btn">
                    <Icon type="redo" />
                </div>
            </auth-component>
            <div
                v-bk-tooltips.bottom="$t('history.全局变量')"
                class="action-btn"
                @click="handleShowGlobalVariable">
                <Icon type="global-var" />
            </div>
            <div
                v-bk-tooltips.bottom="$t('history.执行方案')"
                class="action-btn"
                @click="handleGoPlan">
                <Icon type="flow" />
            </div>
            <div
                v-bk-tooltips.bottom="$t('history.操作记录')"
                class="action-btn"
                @click="handleShowOperationRecord">
                <Icon type="clock" />
            </div>
        </div>
        <execution-status-bar
            :data="formData.taskExecution"
            type="task">
            <step-action
                v-if="formData.taskExecution.isForcedEnable"
                :confirm-handler="handleForceTask"
                name="forced"
                @on-cancel="handleCancelForceTask"
                @on-show="handleStartForceTask" />
        </execution-status-bar>
        <jb-sideslider
            :is-show.sync="isShowGlobalVariable"
            :quick-close="true"
            :show-footer="false"
            :title="$t('history.全局变量')"
            :width="960">
            <global-variable :id="taskInstanceId" />
        </jb-sideslider>
        <jb-sideslider
            :is-show.sync="isShowOperationRecord"
            :quick-close="true"
            :show-footer="false"
            :title="$t('history.操作记录')"
            :width="900">
            <operation-record
                :id="taskInstanceId"
                from="historyTask" />
        </jb-sideslider>
    </div>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';

    import BackTop from '@components/back-top';

    import ExecutionStatusBar from '../common/execution-status-bar';
    import GlobalVariable from '../common/global-variable';
    import OperationRecord from '../common/operation-record';
    import StepAction from '../common/step-action';

    import ExecutionProcess from './components/execution-process';
    import TaskStep from './components/task-step';
    import TaskStepEnd from './components/task-step/theme/end';
    import TaskStepStart from './components/task-step/theme/start';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            BackTop,
            TaskStep,
            ExecutionProcess,
            GlobalVariable,
            OperationRecord,
            StepAction,
            TaskStepStart,
            TaskStepEnd,
            ExecutionStatusBar,
        },
        data () {
            return {
                isLoading: true,
                taskInstanceId: 0,
                isShowGlobalVariable: false,
                isShowOperationRecord: false,
                formData: {
                    finished: true,
                    totalStep: 0,
                    currentStepRunningOrder: 0,
                    taskExecution: {},
                    stepExecution: [],
                },
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        created () {
            this.timer = '';
            this.taskPollingQueue = [];
            this.taskInstanceId = this.$route.params.id;
            this.isForceing = false;
            this.fetchData();
            this.startTimer();
            this.$once('hook:beforeDestroy', () => {
                this.clearTimer();
            });
        },
        
        methods: {
            fetchData (isFirst = true) {
                if (isFirst) {
                    this.isLoading = true;
                }
                
                TaskExecuteService.fetchTaskExecutionResult({
                    id: this.taskInstanceId,
                }, {
                    permission: 'page',
                }).then((data) => {
                    if (this.isForceing) {
                        return;
                    }
                    this.formData = data;
                    if (data.finished) {
                        this.taskPollingQueue = [];
                        return;
                    }
                    this.taskPollingQueue.push(() => this.fetchData(false));
                })
                    .catch((error) => {
                        this.taskPollingQueue = [];
                        if ([400, 1244006].includes(error.code)) {
                            setTimeout(() => {
                                this.$router.push({
                                    name: 'historyList',
                                });
                            }, 3000);
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            startTimer () {
                if (this.timerClear) {
                    return;
                }
                if (this.taskPollingQueue.length > 0) {
                    const nextTask = this.taskPollingQueue.shift();
                    nextTask();
                }
                setTimeout(() => {
                    this.startTimer();
                }, 1000);
            },
            clearTimer () {
                this.timerClear = true;
                this.taskPollingQueue = [];
            },
            handleSelectStep (stepInstance) {
                this.$router.push({
                    name: 'historyStep',
                    params: {
                        taskInstanceId: this.taskInstanceId,
                    },
                    query: {
                        stepInstanceId: stepInstance.stepInstanceId,
                        retryCount: stepInstance.retryCount,
                        from: this.$route.query.from || 'historyTask',
                    },
                });
            },
            handleStartForceTask () {
                this.isForceing = true;
            },
            handleCancelForceTask () {
                this.isForceing = false;
                this.fetchData();
            },
            handleForceTask () {
                return TaskExecuteService.updateTaskExecutionStepOperateTerminate({
                    taskInstanceId: this.taskInstanceId,
                }).then(() => {
                    this.isForceing = false;
                    this.fetchData();
                    this.messageSuccess(I18n.t('history.操作成功'));
                    return true;
                });
            },
            handleUpdateStepStatus () {
                this.fetchData();
            },
            handleGoRetry () {
                this.isLoading = true;
                TaskExecuteService.fetchTaskInstance({
                    id: this.taskInstanceId,
                }).then(({ variables }) => {
                    if (variables.length > 0) {
                        // 有变量，去设置变量
                        this.$router.push({
                            name: 'redoTask',
                            params: {
                                taskInstanceId: this.taskInstanceId,
                            },
                        });
                        return;
                    }
                    // 没有变量直接执行
                    this.$bkInfo({
                        title: I18n.t('history.确认执行？'),
                        subTitle: I18n.t('history.该方案未设置全局变量，点击确认将直接执行。'),
                        confirmFn: () => {
                            this.isLoading = true;
                            TaskExecuteService.redoTask({
                                taskInstanceId: this.taskInstanceId,
                                taskVariables: [],
                            }).then(({ taskInstanceId }) => {
                                this.$bkMessage({
                                    theme: 'success',
                                    message: I18n.t('history.执行成功'),
                                });
                                this.$router.push({
                                    name: 'historyTask',
                                    params: {
                                        id: taskInstanceId,
                                    },
                                });
                                this.taskInstanceId = taskInstanceId;
                            // this.fetchData()
                            })
                                .finally(() => {
                                    this.isLoading = false;
                                });
                        },
                    });
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleShowGlobalVariable () {
                this.isShowGlobalVariable = true;
            },
            handleShowOperationRecord () {
                this.isShowOperationRecord = true;
            },
            handleGoPlan () {
                let router = {};
                if (this.formData.taskExecution.debugTask) {
                    router = this.$router.resolve({
                        name: 'debugPlan',
                        params: {
                            id: this.formData.taskExecution.templateId,
                        },
                        query: {
                            from: 'historyTask',
                        },
                    });
                } else {
                    router = this.$router.resolve({
                        name: 'viewPlan',
                        params: {
                            templateId: this.formData.taskExecution.templateId,
                        },
                        query: {
                            from: 'historyTask',
                            viewPlanId: this.formData.taskExecution.taskId,
                            taskInstanceId: this.taskInstanceId,
                        },
                    });
                }
                window.open(router.href);
            },
            routerBack () {
                if (this.formData.taskExecution.debugTask) {
                    this.$router.push({
                        name: 'templateDetail',
                        params: {
                            id: this.formData.taskExecution.templateId,
                        },
                    });
                    return;
                }
                const { from } = this.$route.query;
                if (from === 'viewPlan') {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: this.formData.taskExecution.templateId,
                        },
                        query: {
                            viewPlanId: this.formData.taskExecution.taskId,
                        },
                    });
                    return;
                }
                if (from === 'planList') {
                    this.$router.push({
                        name: 'planList',
                        query: {
                            viewTemplateId: this.formData.taskExecution.templateId,
                            viewPlanId: this.formData.taskExecution.taskId,
                        },
                    });
                    return;
                }
                
                this.$router.push({
                    name: 'historyList',
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .executive-history-task {
        position: relative;
        top: 0;
        right: 0;
        padding-top: 13px;
        padding-bottom: 53px;

        .step-list {
            display: flex;
            flex-direction: column;
            width: 578px;
            margin: 0 auto;
        }

        .step-list-action {
            position: fixed;
            bottom: 18px;
            left: 50%;
            z-index: 9;
            display: flex;
            transform: translateX(-50%);
            user-select: none;

            .execution-process {
                margin-left: 10px;
            }
        }

        .task-action {
            position: fixed;
            top: 126px;
            right: 22px;
            display: flex;

            .action-btn {
                display: flex;
                width: 32px;
                height: 32px;
                padding: 0 7px;
                margin-left: 10px;
                font-size: 19px;
                color: #979ba5;
                cursor: pointer;
                background: #fff;
                border-radius: 50%;
                box-shadow: 0 2px 6px 0 rgb(0 0 0 / 6%);
                align-items: center;
                justify-content: center;

                &:hover {
                    color: #3a84ff;
                }
            }
        }
    }
</style>
