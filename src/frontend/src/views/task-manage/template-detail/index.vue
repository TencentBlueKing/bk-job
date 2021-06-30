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
    <smart-action class="task-template-detail" offset-target="detail-content">
        <detail-layout class="task-template-detail-layout" mode="see">
            <detail-item :label="$t('template.模板名称：')">
                <auth-component
                    auth="job_template/edit"
                    :resource-id="taskId">
                    <jb-edit-input
                        class="input"
                        field="name"
                        :value="formData.name"
                        :rules="rules.name"
                        :remote-hander="handleUpdateTemplate" />
                    <div slot="forbid">{{ formData.name }}</div>
                </auth-component>
            </detail-item>
            <detail-item :label="$t('template.场景标签：')">
                <auth-component
                    auth="job_template/edit"
                    :resource-id="taskId">
                    <jb-edit-tag
                        class="input"
                        field="tags"
                        :value="formData.tags"
                        :remote-hander="handleUpdateTemplate" />
                    <div slot="forbid">{{ formData.tagText }}</div>
                </auth-component>
            </detail-item>
            <detail-item :label="$t('template.模板描述：')">
                <auth-component
                    auth="job_template/edit"
                    :resource-id="taskId">
                    <jb-edit-textarea
                        class="input"
                        field="description"
                        :placeholder="$t('template.填写该模板的功能介绍等详细描述...')"
                        :maxlength="500"
                        :value="formData.description"
                        :remote-hander="handleUpdateTemplate" />
                    <div slot="forbid">{{ formData.description || '--' }}</div>
                </auth-component>
            </detail-item>
            <detail-item :label="$t('template.全局变量：')" class="gloval-var-item">
                <render-global-var :list="formData.variables" />
            </detail-item>
            <detail-item :label="$t('template.作业步骤：')" class="task-step-item">
                <render-task-step :list="formData.stepList" :variable="formData.variables" />
            </detail-item>
        </detail-layout>
        <template #action>
            <div class="action-box">
                <bk-button
                    theme="primary"
                    class="w120 mr10"
                    @click="handleGoExec">
                    {{ $t('template.选择方案') }}
                </bk-button>
                <auth-button
                    class="mr10"
                    :resource-id="taskId"
                    auth="job_template/debug"
                    @click="handleGoDebug">
                    {{ $t('template.调试') }}
                </auth-button>
                <auth-button
                    class="mr10"
                    :resource-id="taskId"
                    auth="job_template/edit"
                    @click="handleGoEdit">
                    {{ $t('template.编辑') }}
                </auth-button>
                <span v-bk-tooltips="notNeedUpdateTips">
                    <bk-button
                        class="action-sync mr10"
                        :disabled="isNotNeedUpdate"
                        :loading="isPlanListLoading"
                        @click="handleGoSyncPlan">
                        {{ $t('template.同步方案') }}
                        <div v-if="!isNotNeedUpdate" class="update-flag" v-bk-tooltips="$t('template.待同步')">
                            <Icon type="sync-small" />
                        </div>
                    </bk-button>
                </span>
                <jb-popover-confirm
                    class="action-del"
                    :title="$t('template.确定删除该作业模板？')"
                    :content="$t('template.注意！模板下关联的所有执行方案也将被清除')"
                    :confirm-handler="handleDelete">
                    <auth-button
                        class="delete-btn"
                        :resource-id="taskId"
                        auth="job_template/delete">
                        {{ $t('template.删除') }}
                    </auth-button>
                </jb-popover-confirm>
            </div>
        </template>
        <element-teleport v-if="formData.name">
            <div style="font-size: 12px; color: #63656e;">（{{ formData.name }}）</div>
        </element-teleport>
        <back-top />
    </smart-action>
</template>
<script>
    import I18n from '@/i18n';
    import TaskManageService from '@service/task-manage';
    import TaskPlanService from '@service/task-plan';
    import {
        taskTemplateName,
    } from '@utils/validator';
    import DetailLayout from '@components/detail-layout';
    import DetailItem from '@components/detail-layout/item';
    import BackTop from '@components/back-top';
    import JbEditInput from '@components/jb-edit/input';
    import JbEditTag from '@components/jb-edit/tag';
    import JbEditTextarea from '@components/jb-edit/textarea';
    import JbPopoverConfirm from '@components/jb-popover-confirm';
    import RenderGlobalVar from '../common/render-global-var';
    import RenderTaskStep from '../common/render-task-step';

    const getDefaultData = () => ({
        createTime: '',
        creator: '',
        description: '',
        favored: false,
        id: 0,
        lastModifyTime: '',
        lastModifyUser: '',
        name: '',
        needUpdate: false,
        status: 0,
        stepList: [],
        tags: [],
        variables: [],
    });
    export default {
        name: 'TemplateDetail',
        components: {
            DetailLayout,
            DetailItem,
            BackTop,
            RenderGlobalVar,
            RenderTaskStep,
            JbEditInput,
            JbEditTag,
            JbEditTextarea,
            JbPopoverConfirm,
        },
        data () {
            return {
                isLoading: true,
                isPlanListLoading: true,
                formData: getDefaultData(),
                planList: [],
                deleteLoading: false,
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
            isNotNeedUpdate () {
                return !this.planList.some(_ => _.needUpdate);
            },
            notNeedUpdateTips () {
                if (this.planList.length < 1) {
                    return I18n.t('template.该模版下面没有执行方案');
                }
                if (this.planList.every(_ => !_.needUpdate)) {
                    return I18n.t('template.该模版下面的所有执行方案已是最新版');
                }
                return '';
            },
        },
        created () {
            this.taskId = this.$route.params.id;
            this.formDataLocal = {};
            
            this.rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('template.模板名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: taskTemplateName.validator,
                        message: taskTemplateName.message,
                        trigger: 'blur',
                    },
                    {
                        validator: this.checkName,
                        message: I18n.t('template.模板名称已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
            };

            this.fetchData();
            this.fetchPlanList();
        },
        methods: {
            /**
             * @desc 获取作业模版详情
             * @param {Boolean} refresh 更新元数据重新获取
             */
            fetchData (refresh = false) {
                TaskManageService.taskDetail({
                    id: this.taskId,
                }, {
                    permission: 'page',
                })
                    .then((data) => {
                        if (refresh) {
                            // 编辑（name, description, tags）导致的更新，只更新元数据
                            const { name, description, tags } = data;
                            this.formData.name = name;
                            this.formData.description = description;
                            this.formData.tags = tags;
                            this.formDataLocal = data;
                        } else {
                            this.formData = data;
                            this.formDataLocal = { ...data };
                        }
                    })
                    .catch((error) => {
                        if ([
                            1,
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
             * @desc 获取作业模版关联的执行方案列表
             */
            fetchPlanList () {
                this.isPlanListLoading = true;
                TaskPlanService.fetchTaskPlan({
                    id: this.taskId,
                }).then((data) => {
                    this.planList = Object.freeze(data);
                })
                    .finally(() => {
                        this.isPlanListLoading = false;
                    });
            },
            /**
             * @desc 检测作业模版名是否存在
             */
            checkName (name) {
                return TaskManageService.taskCheckName({
                    id: this.taskId,
                    name,
                });
            },
            /**
             * @desc 更新作业模版元数据
             * @param {Object} payload 将要更新的字段名和值
             */
            handleUpdateTemplate (payload) {
                const { name, description, tags } = this.formDataLocal;
                return TaskManageService.taskUpdateBasic({
                    id: this.taskId,
                    name,
                    description,
                    tags,
                    ...payload,
                }).then(() => {
                    this.fetchData(true);
                });
            },
            /**
             * @desc 执行执行方案
             */
            handleGoExec () {
                this.$router.push({
                    name: 'viewPlan',
                    params: {
                        templateId: this.taskId,
                    },
                    query: {
                        from: 'templateDetail',
                    },
                });
            },
            /**
             * @desc 调试执行方案
             */
            handleGoDebug () {
                this.$router.push({
                    name: 'debugPlan',
                    params: {
                        id: this.taskId,
                    },
                    query: {
                        from: 'templateDetail',
                    },
                });
            },
            /**
             * @desc 同步执行方案
             */
            handleGoSyncPlan () {
                this.$router.push({
                    name: 'syncPlanBatch',
                    query: {
                        planIds: this.planList.map(_ => _.id).join(','),
                        from: 'templateDetail',
                    },
                });
            },
            /**
             * @desc 编辑作业模版
             */
            handleGoEdit () {
                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        id: this.taskId,
                    },
                    query: {
                        from: 'templateDetail',
                    },
                });
            },
            /**
             * @desc 删除作业摸吧，然后路由回退
             */
            handleDelete () {
                return TaskManageService.taskDelete({
                    id: this.taskId,
                }).then(() => {
                    this.routerBack();
                    return true;
                });
            },
            /**
             * @desc 路由回退
             */
            routerBack () {
                this.$router.push({
                    name: 'taskList',
                });
            },
        },
    };
</script>
<style lang='postcss'>
    @import '@/css/mixins/media';

    .task-template-detail {
        .task-template-detail-layout {
            margin-bottom: 30px;
        }

        .detail-item {
            justify-content: flex-start;
            margin-bottom: 8px;

            .label {
                width: 76px;
            }
        }

        .gloval-var-item {
            margin-top: 20px;
            margin-bottom: 30px;
        }

        .task-step-item {
            margin-bottom: 20px;
        }

        .input,
        .action-box {
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

        .action-box {
            display: flex;
        }

        .action-button {
            margin-right: 10px;
        }

        .action-del {
            margin-left: auto;
        }

        .action-sync {
            position: relative;

            .update-flag {
                position: absolute;
                top: -9px;
                right: -9px;
                font-size: 16px;
                line-height: 0;
                color: #ea3636;
                background: #fff;
                border: 1px solid #fff;
                border-radius: 50%;
            }
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
