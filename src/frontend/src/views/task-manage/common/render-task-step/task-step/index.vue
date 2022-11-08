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
        <resize-layout
            :right-fixed="true"
            :right-width="366"
            :style="layoutStyles">
            <div
                ref="container"
                class="step-wrapper-container">
                <jb-form
                    fixed
                    :label-width="formMarginLeftWidth">
                    <bk-form-item
                        :label="$t('template.步骤类型')"
                        required>
                        <bk-radio-group
                            class="step-type-radio form-item-content"
                            :value="stepType"
                            @change="handleTypeChange">
                            <bk-radio-button
                                :disabled="isStepTypeReadOnly"
                                :value="1">
                                <Icon type="add-script" />
                                <span>{{ $t('template.执行脚本') }}</span>
                            </bk-radio-button>
                            <bk-radio-button
                                :disabled="isStepTypeReadOnly"
                                :value="2">
                                <Icon type="add-file" />
                                <span>{{ $t('template.分发文件') }}</span>
                            </bk-radio-button>
                            <bk-radio-button
                                :disabled="isStepTypeReadOnly"
                                :value="3">
                                <Icon type="add-approval" />
                                <span>{{ $t('template.人工确认') }}</span>
                            </bk-radio-button>
                        </bk-radio-group>
                    </bk-form-item>
                </jb-form>
                <component
                    :is="stepCom"
                    ref="handler"
                    :data="stepData"
                    v-bind="$attrs"
                    v-on="$listeners" />
                <bk-button
                    class="variable-guide-btn"
                    text
                    @click="handleShowVariableGuide">
                    <Icon type="book" />
                    {{ $t('template.变量使用指引') }}
                </bk-button>
            </div>
            <div slot="right">
                <variable-use-guide
                    v-if="isShowVariableGuide"
                    @on-close="handleHideVariableGuide" />
            </div>
        </resize-layout>
    </div>
</template>
<script>
    import _ from 'lodash';

    import TaskStepModel from '@model/task/task-step';

    import {
        genDefaultName,
    } from '@utils/assist';

    import ResizeLayout from '@components/resize-layout';

    import StepApproval from './components/approval';
    import StepDistroFile from './components/distro-file';
    import StepExecScript from './components/exec-script';

    import I18n from '@/i18n';
    import VariableUseGuide from '@/views/task-manage/common/variable-use-guide';

    const dataFieldMap = {
        1: 'scriptStepInfo',
        2: 'fileStepInfo',
        3: 'approvalStepInfo',
    };

    const genDefaultStepName = (type) => {
        if (type === TaskStepModel.TYPE_SCRIPT) {
            return genDefaultName(I18n.t('template.步骤执行脚本'));
        }
        if (type === TaskStepModel.TYPE_FILE) {
            return genDefaultName(I18n.t('template.步骤分发文件'));
        }
        if (type === TaskStepModel.TYPE_APPROVAL) {
            return genDefaultName(I18n.t('template.步骤人工确认'));
        }
        return genDefaultName();
    };

    export default {
        components: {
            ResizeLayout,
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
                stepType: '',
                stepData: {},
                isShowVariableGuide: false,
                containerHeight: 0,
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
                if (!Object.prototype.hasOwnProperty.call(taskStepMap, this.stepType)) {
                    return 'div';
                }
                return taskStepMap[this.stepType];
            },
            layoutStyles () {
                const windownInnerHeight = window.innerHeight;
                const containerMaxHeight = windownInnerHeight - 114;
                if (this.containerHeight < containerMaxHeight) {
                    return {};
                }
                return {
                    height: `${containerMaxHeight}px`,
                };
            },
            /**
             * @desc 有ID的步骤不可编辑，id大于0已经提交后端保存过的步骤，id小于0本地新建的步骤
             * @returns { Boolean }
             */
            isStepTypeReadOnly () {
                return Boolean(this.data.id);
            },
            formMarginLeftWidth () {
                return this.$i18n.locale === 'en-US'
                    && this.stepType === TaskStepModel.TYPE_FILE
                    ? 140
                    : 110;
            },
        },
        watch: {
            data: {
                handler (data) {
                    this.stepType = data.type;
                    
                    const stepDataField = dataFieldMap[this.stepType];

                    this.defaultStepName = genDefaultStepName(data.type);
                    
                    const {
                        id,
                        name = this.defaultStepName,
                    } = this.data;
                    this.stepData = {
                        name,
                        ...(this.data[stepDataField] || {}),
                    };
                    // 有id透传id
                    if (id) {
                        this.stepData.id = id;
                    }
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
            this.calcContainerHeight();
            const observer = new MutationObserver(() => {
                this.calcContainerHeight();
            });
            observer.observe(this.$refs.container, {
                subtree: true,
                childList: true,
                attributeName: true,
                characterData: true,
            });
            this.$once('hook:beforeDestroy', () => {
                observer.takeRecords();
                observer.disconnect();
            });
        },
        methods: {
            calcContainerHeight: _.debounce(function () {
                if (!this.$refs.container) {
                    return;
                }
                this.containerHeight = this.$refs.container.getBoundingClientRect().height;
            }, 30),
            /**
             * @desc 新建步骤状态切换步骤类型
             * @param { Number } stepType 步骤类型
             * 类型改变时需要判断用户是否编辑过步骤名
             * - 编辑过，切换类型步骤名保持不变
             * - 没有编辑过，切换步骤类型时自动生成新的步骤名
             */
            handleTypeChange (stepType) {
                if (this.defaultStepName === this.$refs.handler.formData.name) {
                    this.defaultStepName = genDefaultStepName(stepType);
                    this.stepData.name = this.defaultStepName;
                } else {
                    this.stepData.name = this.$refs.handler.formData.name;
                }
                this.stepType = stepType;
            },
            /**
             * @desc 显示变量指引
             */
            handleShowVariableGuide () {
                if (this.isShowVariableGuide) {
                    return;
                }
                const $wraper = document
                    .querySelector('#taskStepOperationSideslider')
                    .querySelector('.bk-sideslider-wrapper');
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
                const $wraper = document
                    .querySelector('#taskStepOperationSideslider')
                    .querySelector('.bk-sideslider-wrapper');
                $wraper.style.width = `${this.originWraperWidth}px`;
                this.isShowVariableGuide = false;
            },
            submit () {
                return this.$refs.handler.submit && this.$refs.handler.submit();
            },
            reset () {
                this.stepType = '';
                return this.$refs.handler.reset && this.$refs.handler.reset();
            },
        },
    };
</script>
<style lang="postcss">
    #taskStepOperationSideslider {
        .bk-sideslider-content {
            overflow: unset !important;
        }

        .jb-sideslider-content {
            padding: 0;
        }

        .jb-resize-layout-right {
            background: #fff;

            .right-content-placeholder {
                height: 100vh;
                margin-top: -60px;
            }

            .variable-use-guide {
                height: calc(100vh - 52px);
            }
        }

        .task-step-operation-wraper {
            .step-wrapper-container {
                position: relative;
                padding-top: 20px;
                padding-right: 30px;
                padding-left: 30px;
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
                top: 20px;
                right: 30px;
            }
        }
    }

</style>
