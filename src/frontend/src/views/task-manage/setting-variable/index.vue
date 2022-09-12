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
    <div class="setting-variable-page">
        <smart-action offset-target="variable-value">
            <global-variable-layout
                style="padding-bottom: 20px;"
                type="vertical">
                <global-variable
                    v-for="variable in usedList"
                    :key="variable.id"
                    ref="used"
                    :data="variable"
                    :type="variable.type" />
                <Empty
                    v-if="usedList.length < 1"
                    key="empty"
                    style="height: 160px; max-width: 960px; background-color: #f0f1f5;"
                    :title="$t('template.暂无引用的变量')" />
                <toggle-display
                    v-if="unusedList.length > 0"
                    :count="unusedList.length"
                    style="max-width: 960px; margin-top: 20px;">
                    <div style="margin-top: 20px;">
                        <global-variable
                            v-for="variable in unusedList"
                            :key="variable.id"
                            ref="unused"
                            :data="variable"
                            :type="variable.type" />
                    </div>
                </toggle-display>
            </global-variable-layout>
            <template #action>
                <div class="action-wraper">
                    <bk-button
                        class="w120 mr10"
                        :loading="isSubmiting"
                        theme="primary"
                        @click="handleGoExec">
                        {{ $t('template.执行') }}
                    </bk-button>
                    <bk-button
                        @click="handleCancle">
                        {{ $t('template.取消') }}
                    </bk-button>
                    <bk-button
                        v-if="hasHostVariable"
                        class="remove-all"
                        @click="handleRemoveAllInvalidHost">
                        {{ $t('template.移除无效主机') }}
                    </bk-button>
                </div>
            </template>
        </smart-action>
        <back-top />
        <element-teleport v-if="planName">
            <div style="font-size: 12px; color: #63656e;">
                （{{ planName }}）
            </div>
        </element-teleport>
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskExecuteService from '@service/task-execute';
    import TaskPlanService from '@service/task-plan';

    import { findUsedVariable } from '@utils/assist';

    import BackTop from '@components/back-top';
    import GlobalVariable from '@components/global-variable/edit';
    import GlobalVariableLayout from '@components/global-variable/layout';
    import ToggleDisplay from '@components/global-variable/toggle-display';

    import I18n from '@/i18n';
    
    export default {
        name: '',
        components: {
            GlobalVariableLayout,
            GlobalVariable,
            ToggleDisplay,
            BackTop,
        },
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                hasHostVariable: false,
                usedList: [],
                unusedList: [],
                planName: '',
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
        },
        created () {
            this.taskId = this.$route.params.id;
            this.templateId = this.$route.params.templateId;
            this.isDebugPlan = Boolean(this.$route.params.debug);
            this.fetchData();
        },
        methods: {
            /**
             * @desc 获取执行方案数据
             */
            fetchData () {
                const serviceHandler = this.isDebugPlan
                    ? TaskPlanService.fetchDebugInfo
                    : TaskPlanService.fetchPlanDetailInfo;
                serviceHandler({
                    id: this.taskId,
                    templateId: this.templateId,
                }, {
                    permission: 'page',
                }).then((plan) => {
                    const {
                        name,
                        stepList,
                        variableList,
                    } = plan;
                    this.planName = name;
                    const planStepList = stepList.filter(step => step.enable === 1);
                    const usedVariableNameMap = findUsedVariable(planStepList).reduce((result, item) => {
                        result[item] = true;
                        return result;
                    }, {});
                    
                    // 执行方案中的步骤使用了得变量
                    const usedList = [];
                    // 未被使用的变量
                    const unusedList = [];
                    variableList.forEach((variable) => {
                        if (usedVariableNameMap[variable.name]) {
                            usedList.push(variable);
                        } else {
                            unusedList.push(variable);
                        }
                    });
                    this.usedList = Object.freeze(usedList);
                    this.unusedList = Object.freeze(unusedList);
                    this.hasHostVariable = _.findIndex(variableList, variable => variable.isHost) > -1;
                })
                    .catch((error) => {
                        if ([
                            1243009,
                            400,
                        ].includes(error.code)) {
                            setTimeout(() => {
                                this.$router.push({
                                    name: 'taskList',
                                });
                            }, 3000);
                        }
                    })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 开始执行
             */
            handleGoExec () {
                const validateQueue = [];
                if (this.$refs.used) {
                    this.$refs.used.forEach((item) => {
                        validateQueue.push(item.validate());
                    });
                }
                if (this.$refs.unused) {
                    this.$refs.unused.forEach((item) => {
                        validateQueue.push(item.validate());
                    });
                }
                this.isSubmiting = true;
                Promise.all(validateQueue)
                    .then(taskVariables => TaskExecuteService.taskExecution({
                        taskId: this.taskId,
                        taskVariables: taskVariables.map(({
                            id,
                            name,
                            type,
                            value,
                            targetValue,
                        }) => ({
                            id,
                            name,
                            type,
                            value,
                            targetValue,
                        })),
                    }).then(({ taskInstanceId }) => {
                        this.$bkMessage({
                            theme: 'success',
                            message: I18n.t('template.执行成功'),
                        });
                        window.changeConfirm = false;
                        this.$router.push({
                            name: 'historyTask',
                            params: {
                                id: taskInstanceId,
                            },
                            query: {
                                from: 'planList',
                            },
                        });
                    }))
                    .catch(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 取消执行
             */
            handleCancle () {
                this.routerBack();
            },
            /**
             * @desc 一键移除所有无效主机
             */
            handleRemoveAllInvalidHost () {
                this.$refs.used && this.$refs.used.forEach(item => item.removeAllInvalidHost());
                this.$refs.unused && this.$refs.unused.forEach(item => item.removeAllInvalidHost());
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                const { from } = this.$route.query;
                if (from === 'debugPlan') {
                    this.$router.push({
                        name: 'debugPlan',
                        params: {
                            id: this.templateId,
                        },
                    });
                    return;
                }
                if (from === 'viewPlan') {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: this.templateId,
                        },
                        query: {
                            viewPlanId: this.taskId,
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'planList',
                    query: {
                        viewTemplateId: this.templateId,
                        viewPlanId: this.taskId,
                    },
                });
            },
        },
    };
</script>
<style lang='postcss'>
    .setting-variable-page {
        .action-wraper {
            display: flex;
            align-items: center;
            width: 960px;

            .remove-all {
                margin-left: 40px;
            }
        }
    }
</style>
