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
    <div class="task-step-operation-wraper">
        <div class="step-wrapper" :style="styleWrapperStyles">
            <scroll-faker>
                <div class="step-wrapper-container">
                    <jb-form
                        fixed
                        :label-width="formMarginLeftWidth">
                        <bk-form-item
                            :label="$t('template.步骤类型')"
                            required>
                            <bk-radio-group
                                v-model="taskStepType"
                                class="step-type-radio form-item-content">
                                <bk-radio-button
                                    :value="1"
                                    :disabled="isStepTypeReadOnly">
                                    {{ $t('template.执行脚本') }}
                                </bk-radio-button>
                                <bk-radio-button
                                    :value="2"
                                    :disabled="isStepTypeReadOnly">
                                    {{ $t('template.分发文件') }}
                                </bk-radio-button>
                                <bk-radio-button
                                    :value="3"
                                    :disabled="isStepTypeReadOnly">
                                    {{ $t('template.人工确认') }}
                                </bk-radio-button>
                            </bk-radio-group>
                        </bk-form-item>
                    </jb-form>
                    <component
                        ref="handler"
                        :is="stepCom"
                        :data="stepData"
                        v-bind="$attrs"
                        v-on="$listeners" />
                </div>
            </scroll-faker>
            <bk-button
                text
                class="variable-guide-btn"
                @click="handleShowVariableGuide">
                <Icon type="book" />
                {{ $t('template.变量使用指引') }}
            </bk-button>
        </div>
        <div v-if="isShowVariableGuide" class="guide-right">
            <variable-use-guide @on-close="handleHideVariableGuide" />
        </div>
    </div>
</template>
<script>
    import TaskStepModel from '@model/task/task-step';
    import VariableUseGuide from '@/views/task-manage/common/variable-use-guide';
    import StepDistroFile from './components/distro-file';
    import StepExecScript from './components/exec-script';
    import StepApproval from './components/approval';

    export default {
        components: {
            StepDistroFile,
            StepExecScript,
            StepApproval,
            VariableUseGuide,
        },
        inheritAttrs: false,
        props: {
            data: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                taskStepType: '',
                isShowVariableGuide: false,
            };
        },
        computed: {
            /**
             * @desc 步骤渲染组件
             * @returns { Object }
             */
            stepCom () {
                const taskStepMap = {
                    1: StepExecScript,
                    2: StepDistroFile,
                    3: StepApproval,
                };
                if (!Object.prototype.hasOwnProperty.call(taskStepMap, this.taskStepType)) {
                    return 'div';
                }
                return taskStepMap[this.taskStepType];
            },
            /**
             * @desc 具体的步骤数据
             * @returns { Object }
             */
            stepData () {
                const stepDataMap = {
                    1: 'scriptStepInfo',
                    2: 'fileStepInfo',
                    3: 'approvalStepInfo',
                };
                const currentKey = stepDataMap[this.taskStepType];
                if (!Object.prototype.hasOwnProperty.call(this.data, currentKey)
                    || !Object.keys(this.data[currentKey]).length) {
                    return {};
                }
                const { name, id } = this.data;
                return {
                    name,
                    id,
                    ...this.data[currentKey],
                };
            },
            /**
             * @desc 步骤类型不可编辑
             * @returns { Boolean }
             */
            isStepTypeReadOnly () {
                return !!Object.keys(this.data).length;
            },
            styleWrapperStyles () {
                const styles = {
                    width: '100%',
                };
                if (this.isShowVariableGuide) {
                    styles.width = 'calc(100% - 366px)';
                }
                return styles;
            },
            formMarginLeftWidth () {
                return this.$i18n.locale === 'en-US'
                    && this.taskStepType === TaskStepModel.TYPE_FILE
                    ? 140
                    : 110;
            },
        },
        watch: {
            data: {
                handler (data) {
                    this.taskStepType = data.type;
                },
                immediate: true,
            },
        },
        created () {
            this.originWraperWidth = 0;
        },
        mounted () {
            const $targetSideslider = document.querySelector('#taskStepOperationSideslider');
            $targetSideslider
                .querySelector('.jb-sideslider-footer')
                .style
                .paddingLeft = `${this.formMarginLeftWidth + 30}px`;
        },
        methods: {
            /**
             * @desc 显示变量指引
             */
            handleShowVariableGuide () {
                if (this.isShowVariableGuide) {
                    return;
                }
                const $targetSideslider = document.querySelector('#taskStepOperationSideslider');
                const $wraper = $targetSideslider.querySelector('.bk-sideslider-wrapper');
                $wraper.style.transition = 'width 0.1s';
                setTimeout(() => {
                    const {
                        width,
                    } = $wraper.getBoundingClientRect();
                    this.originWraperWidth = width;
                    $wraper.style.width = `${width + 366}px`;
                    this.isShowVariableGuide = true;
                });
            },
            /**
             * @desc 关闭变量指引
             */
            handleHideVariableGuide () {
                const $targetSideslider = document.querySelector('#taskStepOperationSideslider');
                const $wraper = $targetSideslider.querySelector('.bk-sideslider-wrapper');
                $wraper.style.width = `${this.originWraperWidth}px`;
                this.isShowVariableGuide = false;
            },
            submit () {
                return this.$refs.handler.submit && this.$refs.handler.submit();
            },
            reset () {
                this.taskStepType = '';
                return this.$refs.handler.reset && this.$refs.handler.reset();
            },
        },
    };
</script>
<style lang="postcss">
    .task-step-operation-wraper {
        max-height: calc(100vh - 155px);
        margin-right: -30px;

        .step-wrapper {
            position: relative;
            z-index: 0;

            .step-wrapper-container {
                padding-right: 30px;
            }

            .step-type-radio {
                display: flex;

                .bk-form-radio-button {
                    flex: 1;

                    .bk-radio-button-text {
                        width: 100%;
                    }
                }
            }

            .form-item-content {
                width: 495px;
            }

            .variable-guide-btn {
                position: absolute;
                top: -10px;
                right: 30px;
            }
        }

        .guide-right {
            position: absolute;
            top: 0;
            right: 0;
            z-index: 0;
            width: 366px;
            height: calc(100% - 56px);
        }
    }
</style>
