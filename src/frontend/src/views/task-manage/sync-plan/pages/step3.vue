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
    <layout
        class="sync-plan-step3"
        :empty="isEmpty"
        :loading="isLoading">
        <div class="layout-wraper">
            <div class="layout-left">
                <scroll-faker>
                    <div
                        v-for="timeTask in timeTaskList"
                        :key="timeTask.id"
                        class="job-tab"
                        :class="{ active: timeTask.id === currentTaskId }"
                        @click="handleTabChange(timeTask.id)">
                        <span class="job-name">{{ timeTask.name }}</span>
                        <Icon
                            v-if="timeTask.hasConfirm"
                            class="job-check"
                            type="check" />
                        <div @click.stop="">
                            <bk-switcher
                                class="job-switch"
                                size="small"
                                theme="primary"
                                :value="timeTask.enable"
                                @change="value => handleEnableChange(timeTask.id, value)" />
                        </div>
                    </div>
                </scroll-faker>
            </div>
            <div class="layout-right">
                <scroll-faker>
                    <crontab-detail
                        :key="detailInfo.id"
                        :data="detailInfo"
                        :variable-list="templateVariableList"
                        @on-change="handleVariableChange"
                        @on-update-confirm="handleUpdateConfirm" />
                </scroll-faker>
            </div>
        </div>
        <template #footer>
            <bk-button @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
            <bk-button @click="handleLast">
                {{ $t('template.上一步') }}
            </bk-button>
            <jb-popover-confirm
                :confirm-handler="handleSubmit"
                :content="$t('template.关联定时任务的调整将立即生效')"
                :title="$t('template.是否确认同步？')">
                <bk-button
                    class="w120"
                    theme="primary">
                    {{ $t('template.立即同步') }}
                </bk-button>
            </jb-popover-confirm>
        </template>
    </layout>
</template>
<script>
    import TaskPlanService from '@service/task-plan';
    import TimeTaskService from '@service/time-task';

    import {
        leaveConfirm,
    } from '@utils/assist';

    import ScrollFaker from '@components/scroll-faker';

    import CrontabDetail from '../components/crontab-detail';
    import Layout from '../components/layout';

    import I18n from '@/i18n';

    const generatorData = ({ id, name, enable }) => ({
        id,
        name,
        enable,
        hasConfirm: false,
        variableValue: [],
    });

    export default {
        name: '',
        components: {
            ScrollFaker,
            Layout,
            CrontabDetail,
        },
        props: {
            templateInfo: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                isLoading: true,
                isEmpty: false,
                timeTaskList: [],
                currentTaskId: 0,
            };
        },
        computed: {
            detailInfo () {
                return this.timeTaskList.find(item => item.id === this.currentTaskId) || {};
            },
        },
        created () {
            this.id = this.$route.params.id;
            this.templateId = this.$route.params.templateId;
            this.templateVariableList = this.templateInfo.variables;
            this.fetchTimeTaskList();
        },
        methods: {
            /**
             * @desc 获取执行方案关联的定时任务列表
             */
            fetchTimeTaskList () {
                this.isLoading = true;
                TimeTaskService.fetchTaskOfPlan({
                    id: this.id,
                }).then((data) => {
                    if (data.length < 1) {
                        this.isEmpty = true;
                        return;
                    }
                    this.timeTaskList = data.map(item => generatorData(item));
                    if (data.length > 0) {
                        this.currentTaskId = data[0].id;
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 切换定时任务
             * @param {Number} id 定时任务id
             */
            handleTabChange (id) {
                this.currentTaskId = id;
            },
            /**
             * @desc 更新定时任务的开启状态
             * @param {Number} id 定时任务id
             * @param {Boolean} enable 开启状态
             */
            handleEnableChange (id, enable) {
                const timeTask = this.timeTaskList.find(item => item.id === id);
                timeTask.enable = enable;
                window.changeConfirm = true;
            },
            /**
             * @desc 更新定时任务的开启状态
             * @param {Array} variableValue 定时任务中全局变量
             */
            handleVariableChange (variableValue) {
                const timeTask = this.timeTaskList.find(item => item.id === this.currentTaskId);
                timeTask.variableValue = variableValue;
                timeTask.hasConfirm = true;
                window.changeConfirm = true;
            },
            /**
             * @desc 更新定时任务同步的确认状态
             * @param {Boolean} comfirm 定时任务的确认状态
             */
            handleUpdateConfirm (comfirm) {
                const timeTask = this.timeTaskList.find(item => item.id === this.currentTaskId);
                timeTask.hasConfirm = comfirm;
                window.changeConfirm = true;
            },
            /**
             * @desc 提交定时任务的确认操作
             */
            handleSubmit () {
                const hasAllConfirm = this.timeTaskList.filter(item => item.enable).every(item => item.hasConfirm);
                if (!hasAllConfirm) {
                    this.messageWarn(I18n.t('template.有定时任务还未确认'));
                    return;
                }
                return TaskPlanService.planSyncInfo({
                    planId: this.id,
                    templateId: this.templateId,
                    templateVersion: this.templateInfo.version,
                }).then(() => TimeTaskService.updatePlanTask({
                    cronJobInfoList: this.timeTaskList,
                }), () => {
                    this.messageError(I18n.t('template.执行方案同步失败'));
                })
                    .then(() => {
                        window.changeConfirm = false;
                        this.messageSuccess(I18n.t('template.同步成功'), () => {
                            this.$router.push({
                                name: 'viewPlan',
                                params: {
                                    templateId: this.templateId,
                                },
                                query: {
                                    viewPlanId: this.id,
                                },
                            });
                        });
                        return true;
                    }, () => {
                        this.messageError(I18n.t('template.定时任务更新失败'));
                    });
            },
            /**
             * @desc 回退到上一步
             */
            handleLast () {
                leaveConfirm()
                    .then(() => {
                        this.$emit('on-change', 2);
                    });
            },
            /**
             * @desc 取消同步
             */
            handleCancel () {
                this.$emit('on-cancel');
            },
        },
    };
</script>
<style lang='postcss'>
    .sync-plan-step3 {
        .layout-wraper {
            display: flex;
            background: #fff;

            .layout-left {
                position: relative;
                height: calc(100vh - 210px);
                background: #fafbfd;
                flex: 0 0 320px;

                &::after {
                    position: absolute;
                    top: 0;
                    right: 0;
                    width: 1px;
                    height: 100%;
                    background: #dcdee5;
                    content: "";
                }

                .job-tab {
                    position: relative;
                    z-index: 1;
                    display: flex;
                    height: 40px;
                    padding: 0 13px 0 24px;
                    font-size: 14px;
                    color: #63656e;
                    cursor: pointer;
                    border-bottom: 1px solid #dcdee5;
                    align-items: center;

                    &.active {
                        background: #fff;
                    }

                    .job-name {
                        margin-right: auto;
                    }

                    .job-check {
                        font-size: 24px;
                        color: #2dcb56;
                    }

                    .job-switch {
                        margin-left: 20px;
                    }
                }
            }

            .layout-right {
                height: calc(100vh - 210px);
                flex: 1;
            }
        }
    }
</style>
