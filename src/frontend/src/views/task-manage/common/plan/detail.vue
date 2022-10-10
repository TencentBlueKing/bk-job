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
    <permission-section :key="id">
        <div class="task-plan-view-box">
            <layout
                v-bind="$attrs"
                :loading="isLoading"
                :plan-name="planInfo.name"
                :title="planInfo.name">
                <div
                    slot="title"
                    class="view-title">
                    <edit-title
                        :data="planInfo"
                        @on-edit-success="handleEditSuccess" />
                    <span
                        v-if="planInfo.cronJobCount > 0"
                        v-bk-tooltips.html="`
                            <div>${$t('template.有')} ${planInfo.cronJobCount} ${$t('template.个定时任务')}</div>
                            <div>${$t('template.点击前往查看')}</div>
                        `"
                        class="cron-job-tag"
                        @click="handleGoCronList">
                        <Icon
                            svg
                            type="job-timing" />
                        <span style="margin-left: 2px;">{{ planInfo.cronJobCount }}</span>
                    </span>
                </div>
                <div
                    slot="sub-title"
                    class="link-wraper">
                    <auth-button
                        auth="job_template/view"
                        :resource-id="templateId"
                        text
                        @click="handleGoTemplate">
                        <span>{{ $t('template.所属作业模板') }}</span>
                        <Icon type="jump" />
                    </auth-button>
                </div>
                <jb-form form-type="vertical">
                    <jb-form-item
                        :label="$t('template.全局变量.label')"
                        style="margin-bottom: 30px;">
                        <render-global-var
                            :key="id"
                            :default-field="$t('template.变量值')"
                            :list="usedVariableList" />
                        <toggle-display
                            v-if="unusedVariableList.length > 0"
                            :count="unusedVariableList.length"
                            style="margin-top: 20px;">
                            <render-global-var
                                :key="id"
                                :default-field="$t('template.变量值')"
                                :list="unusedVariableList"
                                style="margin-top: 18px;" />
                        </toggle-display>
                    </jb-form-item>
                    <jb-form-item :label="$t('template.执行步骤')">
                        <render-task-step
                            :key="id"
                            :list="planInfo.enableStepList"
                            :variable="planInfo.variableList" />
                    </jb-form-item>
                </jb-form>
                <template #footer>
                    <div class="action-box">
                        <bk-button
                            v-test="{ type: 'button', value: 'execPlan' }"
                            class="w120 mr10"
                            :loading="execLoading"
                            theme="primary"
                            @click="handleExec">
                            {{ $t('template.去执行') }}
                        </bk-button>
                        <bk-popover placement="top">
                            <auth-button
                                v-test="{ type: 'button', value: 'editPlan' }"
                                auth="job_plan/edit"
                                class="mr10"
                                :permission="planInfo.canEdit"
                                :resource-id="id"
                                @click="handleEdit">
                                <span>{{ $t('template.编辑') }}</span>
                                <span style="font-size: 12px; color: #979ba5;">
                                    ({{ planInfo.enableStepNums }}/{{ planInfo.templateStepNums }})
                                </span>
                            </auth-button>
                            <div slot="content">
                                <p>{{ $t('template.共有') }} {{ planInfo.templateStepNums }} {{ $t('template.个步骤，') }}</p>
                                <!-- eslint-disable-next-line max-len -->
                                <p>{{ $t('template.当前已选中') }} {{ planInfo.enableStepNums }} {{ $t('template.个.select') }}</p>
                            </div>
                        </bk-popover>
                        <auth-button
                            v-test="{ type: 'button', value: 'createCrontab' }"
                            auth="cron/create"
                            class="mr10"
                            @click="handleGoCron">
                            {{ $t('template.定时执行') }}
                        </auth-button>
                        <span :tippy-tips="!planInfo.needUpdate ? $t('template.无需同步') : ''">
                            <auth-button
                                v-test="{ type: 'button', value: 'syncPlan' }"
                                auth="job_plan/sync"
                                class="action-update"
                                :disabled="!planInfo.needUpdate"
                                :resource-id="id"
                                @click="handleSync">
                                <span>{{ $t('template.去同步') }}</span>
                                <div
                                    v-if="planInfo.needUpdate"
                                    class="update-flag">
                                    <Icon
                                        :tippy-tips="$t('template.未同步')"
                                        type="sync-8" />
                                </div>
                            </auth-button>
                        </span>
                        <jb-popover-confirm
                            class="action-del"
                            :confirm-handler="handleDelete"
                            :content="$t('template.若已设置了定时任务，需要先删除才能操作')"
                            :title="$t('template.确定删除该执行方案？')">
                            <auth-button
                                v-test="{ type: 'button', value: 'deletePlan' }"
                                auth="job_plan/delete"
                                class="delete-btn"
                                :permission="planInfo.canDelete"
                                :resource-id="id">
                                {{ $t('template.删除') }}
                            </auth-button>
                        </jb-popover-confirm>
                    </div>
                </template>
            </layout>
        </div>
    </permission-section>
</template>
<script>
    import TaskExecuteService from '@service/task-execute';
    import TaskPlanService from '@service/task-plan';

    import { findUsedVariable } from '@utils/assist';

    import PermissionSection from '@components/apply-permission/apply-section';

    import RenderGlobalVar from '../render-global-var';
    import RenderTaskStep from '../render-task-step';

    import EditTitle from './components/edit-title.vue';
    import Layout from './components/layout';
    import ToggleDisplay from './components/toggle-display';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            PermissionSection,
            RenderGlobalVar,
            RenderTaskStep,
            ToggleDisplay,
            Layout,
            EditTitle,
        },
        props: {
            id: {
                type: [Number, String],
                required: true,
            },
            templateId: {
                type: [Number, String],
                required: true,
            },
        },
        data () {
            return {
                planInfo: {
                    variableList: [],
                    enableStepList: [],
                },
                usedVariableList: [],
                unusedVariableList: [],
                isLoading: true,
                execLoading: false,
                deleteLoading: false,
            };
        },
        
        watch: {
            id: {
                handler (value) {
                    this.fetchData(value);
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 获取执行方案详情
             */
            fetchData () {
                this.isLoading = true;
                TaskPlanService.fetchPlanDetailInfo({
                    id: this.id,
                    templateId: this.templateId,
                }, {
                    permission: 'catch',
                }).then((data) => {
                    this.planInfo = Object.freeze(data);
                    // 处理执行方案步骤中变量的使用情况
                    const planStepList = data.stepList.filter(({ enable }) => enable === 1);
                    const usedVariableNameMap = findUsedVariable(planStepList).reduce((result, variableName) => {
                        result[variableName] = true;
                        return result;
                    }, {});

                    const usedVariableList = [];
                    const unusedVariableList = [];
                    data.variableList.forEach((variable) => {
                        if (usedVariableNameMap[variable.name]) {
                            usedVariableList.push(variable);
                        } else {
                            unusedVariableList.push(variable);
                        }
                    });
                    this.usedVariableList = Object.freeze(usedVariableList);
                    this.unusedVariableList = Object.freeze(unusedVariableList);
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 立即执行执行方案
             */
            fetchTaskExecution () {
                this.execLoading = true;
                TaskExecuteService.taskExecution({
                    taskId: this.id,
                    taskVariables: [],
                }).then(({ taskInstanceId }) => {
                    this.$bkMessage({
                        theme: 'success',
                        message: I18n.t('template.操作成功'),
                    });
                    this.$router.push({
                        name: 'historyTask',
                        params: {
                            id: taskInstanceId,
                        },
                        query: {
                            from: this.$route.name,
                        },
                    });
                })
                    .finally(() => {
                        this.execLoading = false;
                    });
            },
            /**
             * @desc 外部调用——刷新接口数据
             */
            refresh () {
                this.fetchData();
            },
            /**
             * @desc 编辑执行方案
             */
            handleEdit () {
                this.$emit('on-edit', {
                    id: this.templateId,
                    active: this.id,
                });
            },
            /**
             * @desc 执行方案编辑成功
             */
            handleEditSuccess () {
                this.$emit('on-edit-success');
            },
            /**
             * @desc 查看执行方案关联的定时任务列表
             */
            handleGoCronList () {
                const { href } = this.$router.resolve({
                    name: 'cronList',
                    query: {
                        planId: this.id,
                    },
                });
                window.open(href);
            },
            /**
             * @desc 查看执行方案关联的作业模板详情
             */
            handleGoTemplate () {
                const { href } = this.$router.resolve({
                    name: 'templateDetail',
                    params: {
                        id: this.templateId,
                    },
                });
                window.open(href);
            },
            /**
             * 以当前执行方案新建定时任务
             */
            handleGoCron () {
                const { href } = this.$router.resolve({
                    name: 'cronList',
                    query: {
                        mode: 'create',
                        templateId: this.templateId,
                        planId: this.id,
                    },
                });
                window.open(href);
            },
            /**
             * @desc 删除执行方案
             */
            handleDelete () {
                return TaskPlanService.planDelete({
                    id: this.id,
                    templateId: this.templateId,
                }).then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: I18n.t('template.操作成功'),
                    });
                    setTimeout(() => {
                        this.$emit('on-delete');
                    });
                    return true;
                });
            },
            /**
             * @desc 同步执行方案
             */
            handleSync () {
                this.$router.push({
                    name: 'syncPlan',
                    params: {
                        id: this.id,
                        templateId: this.templateId,
                    },
                    query: {
                        from: this.$route.name,
                    },
                });
            },
            /**
             * @desc 执行
             *
             * 执行方案中没有变量——直接执行
             * 执行方案中有变量——跳转到设置变量页面
             */
            handleExec () {
                if (!this.planInfo.variableList.length) {
                    this.$bkInfo({
                        title: I18n.t('template.确认执行？'),
                        subTitle: I18n.t('template.未设置全局变量，点击确认将直接执行。'),
                        confirmFn: () => {
                            this.fetchTaskExecution();
                        },
                    });
                    return;
                }
                this.$router.push({
                    name: 'settingVariable',
                    params: {
                        id: this.id,
                        templateId: this.templateId,
                    },
                    query: {
                        from: this.$route.name,
                    },
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import "@/css/mixins/media";

    .task-plan-view-box {
        .view-title {
            flex: 1;
            display: flex;
            align-items: center;

            .cron-job-tag {
                display: inline-flex;
                display: flex;
                height: 16px;
                padding: 0 4px;
                margin-left: 14px;
                font-size: 12px;
                color: #fff;
                cursor: pointer;
                background: #3a84ff;
                border-radius: 8px;
                user-select: none;
                justify-content: center;
                align-items: center;
            }

            .link-wraper {
                display: flex;
            }
        }

        .action-box {
            display: flex;
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

        .action-update {
            position: relative;

            .update-flag {
                position: absolute;
                top: -10px;
                right: -10px;
                font-size: 16px;
                line-height: 0;
                color: #ea3636;
                background: #fff;
                border: 2px solid #fff;
            }
        }

        .action-del {
            margin-left: auto;
        }

        .delete-btn {
            &:hover {
                color: #fff;
                background: #ea3636;
                border-color: transparent;
            }
        }
    }
</style>
