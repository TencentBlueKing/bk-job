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
        class="edit-execute-plan">
        <bk-alert
            class="info"
            :title="$t('template.调试方案的特殊性：不可删除、始终与作业模板同步、不能被API调用、只能在作业平台使用')" />
        <smart-action offset-target="bk-form-content">
            <jb-form
                ref="editPlanForm"
                :model="formData"
                :rules="rules">
                <jb-form-item :label="$t('template.全局变量.label')">
                    <render-global-var
                        :list="variableList"
                        mode="editOfPlan"
                        :select-value="selectedVariable"
                        @on-change="handleGlobalVariableChange" />
                </jb-form-item>
                <jb-form-item
                    :label="$t('template.执行步骤')"
                    property="enableSteps"
                    required>
                    <render-task-step
                        :list="taskStepList"
                        mode="select"
                        :select-value="formData.enableSteps"
                        @on-select="handleSelectStep" />
                </jb-form-item>
            </jb-form>
            <template #action>
                <bk-button
                    class="w120 mr10"
                    :loading="isSubmitLoading"
                    theme="primary"
                    @click="handleSumbit">
                    {{ $t('template.保存') }}
                </bk-button>
                <bk-button @click="handleCancle">
                    {{ $t('template.取消') }}
                </bk-button>
            </template>
        </smart-action>
    </div>
</template>
<script>
    import TaskPlanService from '@service/task-plan';

    import {
        findUsedVariable,
    } from '@utils/assist';

    import JbForm from '@components/jb-form';

    import RenderGlobalVar from '../common/render-global-var';
    import RenderTaskStep from '../common/render-task-step';

    import I18n from '@/i18n';

    const getDefaultData = () => ({
        id: 0,
        name: '',
        enableSteps: [],
        templateId: 0,
        variables: [],
    });

    export default {
        name: '',
        components: {
            JbForm,
            RenderGlobalVar,
            RenderTaskStep,
        },
        data () {
            return {
                formData: getDefaultData(),
                variableList: [],
                taskStepList: [],
                isLoading: true,
                isSubmitLoading: false,
            };
        },
        computed: {
            isSkeletonLoading () {
                return this.isLoading;
            },
            selectedVariable () {
                const selectedSteps = this.taskStepList.filter(step => this.formData.enableSteps.includes(step.id));
                return findUsedVariable(selectedSteps);
            },
        },
        created () {
            this.rules = {
                enableSteps: [
                    {
                        validator: () => this.formData.enableSteps.length,
                        message: I18n.t('template.执行步骤必填'),
                        trigger: 'blur',
                    },
                ],
            };
            this.formData.templateId = Number(this.$route.params.id);
            this.fetchData();
        },
        methods: {
            fetchData () {
                this.$request(TaskPlanService.fetchDebugInfo({
                    templateId: this.formData.templateId,
                }, {
                    permission: 'page',
                }), () => {
                    this.isLoading = true;
                }).then((data) => {
                    this.variableList = Object.freeze(data.variableList);
                    this.taskStepList = Object.freeze(data.stepList);

                    this.formData.name = data.name;
                    this.formData.id = data.id;
                    this.formData.enableSteps = data.enableStepId;
                    this.formData.variables = data.variableList;
                })
                    .catch((error) => {
                        if ([1243027, 400].includes(error.code)) {
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

            handleGlobalVariableChange (payload) {
                this.formData.variables = payload.map((item) => {
                    const data = { ...item };
                    if (!data.delete) {
                        delete data.delete;
                    }
                    return data;
                });
            },

            handleSelectStep (payload) {
                const index = this.formData.enableSteps.findIndex(item => item === payload.id);

                if (index > -1) {
                    this.formData.enableSteps.splice(index, 1);
                    return;
                }

                this.formData.enableSteps.push(payload.id);

                if (this.formData.enableSteps.length) {
                    this.$refs.editPlanForm.clearError('enableSteps');
                }
            },

            handleSumbit () {
                this.isSubmitLoading = true;
                this.$refs.editPlanForm.validate()
                    .then(() => TaskPlanService.planUpdate(this.formData)
                        .then((data) => {
                            this.$bkMessage({
                                theme: 'success',
                                message: I18n.t('template.操作成功'),
                            });
                            this.$router.push({
                                name: 'debugPlan',
                                params: {
                                    id: this.formData.templateId,
                                },
                            });
                        }))
                    .finally(() => {
                        this.isSubmitLoading = false;
                    });
            },

            handleCancle () {
                this.routerBack();
            },

            routerBack () {
                this.$router.push({
                    name: 'debugPlan',
                    params: {
                        id: this.formData.templateId,
                    },
                });
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .edit-execute-plan {
        .info {
            margin-bottom: 20px;
        }
    }
</style>
