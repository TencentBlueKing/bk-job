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
    <div class="setting-variable-page" v-bkloading="{ isLoading }">
        <smart-action offset-target="variable-value">
            <global-variable-layout>
                <global-variable
                    v-for="variable in variableList"
                    ref="variable"
                    :type="variable.type"
                    :key="variable.id"
                    :data="variable" />
            </global-variable-layout>
            <template #action>
                <bk-button
                    theme="primary"
                    class="w120 mr10"
                    :loading="isSubmiting"
                    @click="handleGoExec">
                    {{ $t('template.执行') }}
                </bk-button>
                <bk-button
                    @click="handleCancle">
                    {{ $t('template.取消') }}
                </bk-button>
            </template>
        </smart-action>
        <back-top />
        <element-teleport v-if="planName">
            <div style="font-size: 12px; color: #63656e;">（{{ planName }}）</div>
        </element-teleport>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskExecuteService from '@service/task-execute';
    import TaskPlanService from '@service/task-plan';
    import GlobalVariableLayout from '@components/global-variable/layout';
    import GlobalVariable from '@components/global-variable/edit';
    import BackTop from '@components/back-top';
    
    export default {
        name: '',
        components: {
            GlobalVariableLayout,
            GlobalVariable,
            BackTop,
        },
        data () {
            return {
                isLoading: true,
                isSubmiting: false,
                variableList: [],
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
                    this.planName = plan.name;
                    this.variableList = Object.freeze(plan.variableList);
                })
                    .catch((error) => {
                        if ([1243009, 400].includes(error.code)) {
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
                if (!this.$refs.variable) {
                    return;
                }
                this.isSubmiting = true;
                Promise.all(this.$refs.variable.map(item => item.validate()))
                    .then(taskVariables => TaskExecuteService.taskExecution({
                        taskId: this.taskId,
                        taskVariables: taskVariables.map(({ id, name, type, value, targetValue }) => ({
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
                        window.changeAlert = false;
                        this.$router.push({
                            name: 'historyTask',
                            params: {
                                id: taskInstanceId,
                            },
                            query: {
                                from: 'plan',
                            },
                        });
                    }))
                    .finally(() => {
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
                } else {
                    this.$router.push({
                        name: 'viewPlan',
                        params: {
                            templateId: this.templateId,
                        },
                        query: {
                            viewPlanId: this.taskId,
                        },
                    });
                }
            },
        },
    };
</script>
<style lang='postcss'>
    .setting-variable-page {
        .title {
            margin-bottom: 20px;
            font-size: 14px;
            line-height: 1;
            color: #313238;
        }

        .variable-list {
            display: inline-block;
        }
    }
</style>
