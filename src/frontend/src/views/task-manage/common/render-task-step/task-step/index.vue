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
        <div class="left" :style="leftStyles">
            <scroll-faker>
                <div class="container">
                    <jb-form fixed :label-width="formMarginLeftWidth">
                        <bk-form-item :label="$t('template.步骤类型')" required>
                            <bk-select
                                v-model="taskStepType"
                                :clearable="false"
                                :disabled="isStepTypeReadOnly"
                                class="form-item-content">
                                <bk-option id="1" :name="$t('template.执行脚本')" />
                                <bk-option id="2" :name="$t('template.分发文件')" />
                                <bk-option id="3" :name="$t('template.人工确认')" />
                            </bk-select>
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
                变量使用指引
            </bk-button>
        </div>
        <div v-if="isShowVariableGuide" class="right">
            <variable-use-guide @on-close="handleHideVariableGuide" />
        </div>
    </div>
</template>
<script>
    import StepDistroFile from './distro-file';
    import StepExecScript from './exec-script';
    import StepArificial from './artificial';
    import VariableUseGuide from './variable-use-guide.vue';

    export default {
        components: {
            StepDistroFile,
            StepExecScript,
            StepArificial,
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
                    3: StepArificial,
                };
                if (!Object.prototype.hasOwnProperty.call(taskStepMap, this.taskStepType)) {
                    return 'div';
                }
                return taskStepMap[this.taskStepType];
            },
            /**
             * @desc 步骤数据
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
            leftStyles () {
                const styles = {
                    width: '100%',
                };
                if (this.isShowVariableGuide) {
                    styles.width = 'calc(100% - 366px)';
                }
                return styles;
            },
            formMarginLeftWidth () {
                return this.$i18n.locale === 'en-US' && this.taskStepType === 2 ? 140 : 110;
            },
        },
        watch: {
            data: {
                handler (value) {
                    if (Object.keys(value).length) {
                        this.taskStepType = this.data.type;
                        return;
                    }
                    this.taskStepType = 1;
                },
                immediate: true,
            },
        },
        created () {
            this.originWraperWidth = 0;
        },
        mounted () {
            const $targetSideslider = document.querySelector('#taskStepOperationSideslider');
            $targetSideslider.querySelector('.jb-sideslider-footer').style.paddingLeft = `${this.formMarginLeftWidth + 30}px`;
        },
        methods: {
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

        .left {
            position: relative;
            z-index: 0;

            .container {
                padding-right: 30px;
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

        .right {
            position: absolute;
            top: 0;
            right: 0;
            z-index: 0;
            width: 366px;
            height: calc(100% - 56px);
            border-left: 1px solid #dcdee5;
        }
    }
</style>
