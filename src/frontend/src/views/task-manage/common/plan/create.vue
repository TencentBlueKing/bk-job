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
        class="task-plan-create-box"
        v-bind="$attrs"
        :loading="isLoading">
        <jb-form
            ref="titleForm"
            slot="title"
            :model="formData"
            style="width: 100%;">
            <jb-form-item
                error-display-type="tooltips"
                property="name"
                :rules="rules.name"
                style="margin-bottom: 0;">
                <jb-input
                    v-model="formData.name"
                    behavior="simplicity"
                    class="name-input"
                    :maxlength="60"
                    :native-attributes="{
                        spellcheck: false,
                        autofocus: true,
                    }"
                    :placeholder="$t('template.推荐按照该执行方案提供的使用场景来取名...')"
                    @change="handleNameChange" />
            </jb-form-item>
        </jb-form>
        <jb-form
            ref="createPlanForm"
            form-type="vertical"
            :model="formData">
            <jb-form-item style="margin-bottom: 40px;">
                <div class="section-title">
                    <span>{{ $t('template.全局变量.label') }}</span>
                    <span>（ {{ selectedVariable.length }} / {{ globalVariableList.length }} ）</span>
                </div>
                <render-global-var
                    :key="templateId"
                    :default-field="$t('template.变量值')"
                    :list="globalVariableList"
                    mode="editOfPlan"
                    :select-value="selectedVariable"
                    @on-change="handleVariableChange" />
            </jb-form-item>
            <jb-form-item
                property="enableSteps"
                :rules="rules.enableSteps">
                <div class="task-step-selection">
                    <div class="section-title">
                        <span>{{ $t('template.选择执行步骤') }}</span>
                        <span>（ {{ formData.enableSteps.length }} / {{ taskStepList.length }} ）</span>
                    </div>
                    <div class="step-check">
                        <bk-button
                            v-if="hasSelectAll"
                            text
                            @click="handleDeselectAll">
                            {{ $t('template.取消全选') }}
                        </bk-button>
                        <bk-button
                            v-else
                            text
                            @click="handleSelectAll">
                            {{ $t('template.全选') }}
                        </bk-button>
                    </div>
                </div>
                <render-task-step
                    :key="templateId"
                    :list="taskStepList"
                    mode="select"
                    :select-value="formData.enableSteps"
                    :variable="globalVariableList"
                    @on-select="handleSelectStep" />
            </jb-form-item>
        </jb-form>
        <template #footer>
            <span v-bk-tooltips="isSubmitDisable ? $t('template.请至少勾选一个执行步骤') : ''">
                <bk-button
                    v-test="{ type: 'button', value: 'createPlanSubmit' }"
                    class="w120 mr10"
                    :disabled="isSubmitDisable"
                    :loading="submitLoading"
                    theme="primary"
                    @click="handleSumbit">
                    {{ $t('template.提交') }}
                </bk-button>
            </span>
            <bk-button
                v-test="{ type: 'button', value: 'createPlanReset' }"
                @click="handleReset">
                {{ $t('template.重置') }}
            </bk-button>
        </template>
    </layout>
</template>
<script>
    import TaskManageService from '@service/task-manage';
    import ExecPlanService from '@service/task-plan';

    import {
        findUsedVariable,
        genDefaultName,
    } from '@utils/assist';
    import { planNameRule } from '@utils/validator';

    import JbForm from '@components/jb-form';

    import RenderGlobalVar from '../../common/render-global-var';
    import RenderTaskStep from '../../common/render-task-step';

    import Layout from './components/layout';

    import I18n from '@/i18n';
    
    const getDefaultData = () => ({
        id: 0,
        name: genDefaultName(I18n.t('template.执行方案.label')),
        enableSteps: [],
        templateId: 0,
        variables: [],
    });
    
    export default {
        name: '',
        components: {
            JbForm,
            Layout,
            RenderGlobalVar,
            RenderTaskStep,
        },
        props: {
            templateId: {
                type: [
                    Number,
                    String,
                ],
                required: true,
            },
            firstPlan: {
                type: Boolean,
                default: true,
            },
        },
        data () {
            return {
                formData: getDefaultData(),
                globalVariableList: [],
                taskStepList: [],
                isLoading: true,
                submitLoading: false,
            };
        },
        computed: {
            /**
             * @desc 查找步骤中已使用的变量
             * @return {Array}
             */
            selectedVariable () {
                const selectedSteps = this.taskStepList.filter(step => this.formData.enableSteps.includes(step.id));
                return findUsedVariable(selectedSteps);
            },
            /**
             * @desc 已选中所有步骤
             * @return {Boolean}
             */
            hasSelectAll () {
                return this.formData.enableSteps.length >= this.taskStepList.length;
            },
            /**
             * @desc 禁用提交按钮
             * @returns { Boolean }
             */
            isSubmitDisable () {
                return this.formData.enableSteps.length < 1;
            },
        },
        created () {
            this.rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('template.方案名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: planNameRule.validator,
                        message: planNameRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: name => ExecPlanService.planCheckName({
                            templateId: this.formData.templateId,
                            planId: this.formData.id,
                            name,
                        }),
                        message: I18n.t('template.方案名称已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
                enableSteps: [
                    {
                        validator: () => this.formData.enableSteps.length > 0,
                        message: I18n.t('template.执行步骤必填'),
                        trigger: 'blur',
                    },
                ],
            };
            this.formData.templateId = this.templateId;
            this.generatorFormData = () => getDefaultData();
            this.fetchData();
        },
        methods: {
            /**
             * @desc 获取选中模板的信息
             *
             * 如果模板关联的执行方案为空，初始化执行方案的 name 为作业模板的 name
             */
            fetchData () {
                TaskManageService.taskDetail({
                    id: this.formData.templateId,
                }).then((data) => {
                    const { variables, stepList, name } = data;
                    this.globalVariableList = Object.freeze(variables);
                    this.taskStepList = Object.freeze(stepList);
                    
                    // 新建执行方案默认值处理
                    let planName = genDefaultName(I18n.t('template.执行方案.label'));
                    if (this.firstPlan) {
                        // 第一个执行方案名默认和模板名相同
                        planName = name;
                    }
                    this.generatorFormData = () => ({
                        id: 0,
                        name: planName,
                        templateId: this.formData.templateId,
                        enableSteps: stepList.map(item => item.id),
                        variables: [
                            ...variables,
                        ],
                    });
                    this.$emit('on-name-change', planName);
                    // 初始化新建执行方案
                    this.formData = this.generatorFormData();
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            /**
             * @desc 外部调用——刷新接口数据
             */
            refresh () {
                this.fetchData();
            },
            /**
             * @desc 执行方案名更新
             * @param {String} name
             */
            handleNameChange (name) {
                this.formData.name = name;
                this.$emit('on-name-change', name);
            },
            /**
             * @desc 选中所有步骤
             */
            handleSelectAll () {
                this.formData.enableSteps = this.taskStepList.map(item => item.id);
            },
            /**
             * @desc 清空步骤的选中状态
             */
            handleDeselectAll () {
                this.formData.enableSteps = [];
            },
            /**
             * @desc 编辑执行方案的变量
             * @param {Array} variables
             */
            handleVariableChange (variables) {
                this.formData.variables = variables;
            },
            /**
             * @desc 选中步骤
             * @param {Object} stepData 步骤数据
             */
            handleSelectStep (stepData) {
                const index = this.formData.enableSteps.findIndex(item => item === stepData.id);

                if (index > -1) {
                    this.formData.enableSteps.splice(index, 1);
                    return;
                }

                this.formData.enableSteps.push(stepData.id);

                if (this.formData.enableSteps.length) {
                    this.$refs.createPlanForm.clearError('enableSteps');
                }
            },
            /**
             * @desc 提交新建执行方案
             */
            handleSumbit () {
                this.submitLoading = true;
                Promise.all([
                    this.$refs.titleForm.validate(),
                    this.$refs.createPlanForm.validate(),
                ]).then(() => ExecPlanService.planUpdate(this.formData)
                    .then((data) => {
                        window.changeConfirm = false;
                        this.$bkMessage({
                            theme: 'success',
                            message: I18n.t('template.操作成功'),
                        });
                        this.$emit('on-create', data);
                    }))
                    .finally(() => {
                        this.submitLoading = false;
                    });
            },
            /**
             * @desc 重置表单数据
             */
            handleReset () {
                this.formData = this.generatorFormData();
                this.$refs.createPlanForm.clearError();
            },
        },
    };
</script>
<style lang="postcss">
    @import "@/css/mixins/media";

    .task-plan-create-box {
        .variable-batch-action {
            margin: 4px 0;
        }

        .layout-title {
            padding-bottom: 0 !important;
            border-bottom-color: transparent !important;

            .name-input {
                .bk-form-input {
                    font-size: 18px;
                    color: #313238;
                }

                .only-bottom-border {
                    padding-top: 9px;
                    padding-bottom: 16px;
                    padding-left: 0;
                }
            }
        }

        .section-title {
            font-size: 14px;
            line-height: 19px;
            color: #313238;
        }

        .task-step-selection {
            display: flex;
            width: 500px;
            margin-bottom: 14px;
            font-size: 16px;
            line-height: 21px;
            color: #313238;

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

            .step-check {
                margin-left: auto;
            }
        }
    }
</style>
