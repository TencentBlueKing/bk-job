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
    <div id="templateVariableRender">
        <div v-if="showEmpty">
            --
        </div>
        <template v-else>
            <div class="variable-batch-action">
                <bk-button
                    v-if="isOperation"
                    text
                    @click="handleShowBatchOperation">
                    <Icon type="bulk-edit" />
                    {{ $t('template.批量编辑') }}
                </bk-button>
                <bk-button
                    v-if="isEditOfPlan"
                    text
                    @click="handleShowBatchEditOfPlan">
                    <Icon type="bulk-edit" />
                    {{ $t('template.批量编辑变量值') }}
                </bk-button>
            </div>
            <div class="task-global-variable-box">
                <template v-for="(item, index) in variable">
                    <div
                        v-if="item.delete !== 1"
                        :key="`${item.name}_${index}`"
                        class="render-global-variable"
                        :class="[
                            {
                                'step-mode-diff': isDiff,
                            },
                            isSelect ?
                                selectValue.includes(item.name) ? 'selected' : 'not-selected'
                                : '',
                        ]">
                        <div
                            class="global-variable-content"
                            :class="[diff[item.id] && diff[item.id].type]"
                            @click="handlerOperation(item, index)"
                            @mouseenter="handleShowPopoverDetail(item)"
                            @mouseleave="handleHidePopoverDetail">
                            <div
                                :id="`globalVariableWithName_${item.name}`"
                                class="variable-type">
                                <Icon :type="item.icon" />
                            </div>
                            <div class="variable-info">
                                <div class="variable-name">
                                    {{ item.name }}
                                </div>
                                <div class="variable-description">
                                    {{ item.valueText }}
                                </div>
                            </div>
                            <Icon
                                v-if="isOperation"
                                class="variable-delete-btn"
                                type="close"
                                @click.stop="handleDelete(index)" />
                        </div>
                    </div>
                </template>
                <div
                    v-if="isOperation"
                    key="create"
                    class="global-variable-new">
                    <div
                        v-test="{ type: 'button', value: 'create_global_variable' }"
                        class="new-btn"
                        @click="handleCreate">
                        <Icon
                            class="create-flag"
                            type="plus" />
                        <span>{{ $t('template.全局变量.label') }}</span>
                    </div>
                    <div
                        v-bk-tooltips="$t('template.使用指引')"
                        class="use-guide"
                        :class="{
                            active: isShowUseGuide,
                        }"
                        @click.stop="handleUseGuideToggle">
                        <Icon type="help-document-fill" />
                    </div>
                </div>
            </div>
            <popover-detail
                v-if="currentPopoverDetail.name"
                :data="currentPopoverDetail"
                :default-field="defaultField"
                :edit-of-plan="isEditOfPlan"
                :select-value="selectValue" />
            <element-teleport
                v-if="isOperation && isShowUseGuide"
                target="#globalVariableGuide">
                <variable-use-guide
                    @on-close="handleUseGuideClose" />
            </element-teleport>
            <jb-sideslider
                v-if="isView || isEditOfPlan"
                ref="variableView"
                :is-show.sync="isShowDetail"
                :media="detailMedia"
                :show-footer="false"
                :title="$t('template.查看全局变量')">
                <detail
                    v-if="isShowDetail"
                    :data="currentData"
                    :default-field="defaultField" />
            </jb-sideslider>
            <jb-sideslider
                v-if="isEditOfPlan"
                :is-show.sync="isShowEditOfPlan"
                :title="$t('template.编辑全局变量')"
                :width="960">
                <edit-of-plan
                    v-if="isShowEditOfPlan"
                    ref="planGlobalVar"
                    :data="currentData"
                    @on-change="handlePlanEditSubmit" />
            </jb-sideslider>
            <jb-sideslider
                v-if="isEditOfPlan"
                footer-offset-target="variable-value"
                :is-show.sync="isShowBatchEditOfPlan"
                :ok-text="$t('template.确定')"
                :title="$t('template.批量编辑变量值')"
                :width="960">
                <batch-edit-of-plan
                    v-if="isShowBatchEditOfPlan"
                    ref="planGlobalVar"
                    :selected-list="selectValue"
                    :variable-list="variable"
                    @on-change="handleBatchPlanEditSubmit" />
            </jb-sideslider>
            <jb-sideslider
                v-if="isOperation"
                :is-show.sync="isShowOperation"
                v-bind="operationSideSliderInfo"
                :width="960">
                <operation
                    v-if="isShowOperation"
                    ref="globalVar"
                    :data="currentData"
                    :variable="realVariable"
                    @on-change="handleOperationSubmit" />
            </jb-sideslider>
            <jb-sideslider
                v-if="isOperation"
                :is-show.sync="isShowBatchOperation"
                :media="batchOperationMediaQuery"
                :title="$t('template.批量编辑变量')">
                <batch-operation
                    v-if="isShowBatchOperation"
                    :variable="variable"
                    @on-change="handleBatchOperationSubmit" />
            </jb-sideslider>
        </template>
    </div>
</template>
<script>
    import _ from 'lodash';

    import VariableModel from '@model/task/global-variable';

    import BatchEditOfPlan from './batch-edit-of-plan';
    import BatchOperation from './batch-operation';
    import Detail from './detail';
    import EditOfPlan from './edit-of-plan';
    import Operation from './operation';
    import PopoverDetail from './popover-detail';

    import I18n from '@/i18n';
    import VariableUseGuide from '@/views/task-manage/common/variable-use-guide';

    export default {
        name: 'RenderGlobalVar',
        components: {
            VariableUseGuide,
            Operation,
            BatchOperation,
            Detail,
            EditOfPlan,
            BatchEditOfPlan,
            PopoverDetail,
        },
        props: {
            list: {
                type: Array,
                required: true,
            },
            /*
             * @value ''：仅可查看详情
             * @value 'operate': 可编辑可新建可删除
             * @value 'select': 选择模式
             * @value 'editOfPlan'：不可删除，使用执行方案的编辑功能
             * @value 'viewOfPlan': 执行方案的查看功能
             * @value 'diff'：diff场景
             */
            mode: {
                type: String,
                default: '',
            },
            selectValue: {
                type: Array,
                default: () => [],
            },
            defaultField: {
                type: String,
                default: I18n.t('template.初始值'),
            },
            diff: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                isShowDetail: false,
                isShowUseGuide: false,
                isShowOperation: false,
                isShowBatchOperation: false,
                isShowEditOfPlan: false,
                isShowBatchEditOfPlan: false,
                variable: [],
                currentPopoverDetail: {},
                currentData: {},
                currentIndex: -1,
                currentOperation: 'create',
                detailMedia: [],
            };
        },
        computed: {
            /**
             * @desc 展示数据为空时的文本
             * @returns { Boolean }
             */
            showEmpty () {
                if (this.isOperation) {
                    return false;
                }
                return this.variable.length < 1;
            },
            /**
             * @desc 新建、编辑全局变量
             * @returns { Boolean }
             */
            isOperation () {
                return this.mode === 'operate';
            },
            /**
             * @desc 选择全局变量
             * @returns { Boolean }
             */
            isSelect () {
                return this.mode === 'select' || this.mode === 'editOfPlan';
            },
            /**
             * @desc 编辑执行方案中的全局变量
             * @returns { Boolean }
             */
            isEditOfPlan () {
                return this.mode === 'editOfPlan';
            },
            /**
             * @desc 查看全局变量
             * @returns { Boolean }
             */
            isView () {
                return !this.mode;
            },
            /**
             * @desc 插卡全局变量同步对比差异
             * @returns { Boolean }
             */
            isDiff () {
                return this.mode === 'diff';
            },
            /**
             * @desc 展示的全局变量不包含已删除
             * @returns { Array }
             */
            realVariable () {
                // 过滤掉已经删除的变量
                const validVariable = this.variable.filter(item => !item.delete);
                // 编辑操作不包含正在编辑的变量
                if (this.currentOperation === 'edit') {
                    return validVariable.filter(item => item.name !== this.currentData.name);
                }
                return validVariable;
            },
            /**
             * @desc 变量编辑侧栏的展示信息
             * @returns { Object }
             */
            operationSideSliderInfo () {
                if (Object.keys(this.currentData).length < 1) {
                    return {
                        title: I18n.t('template.新建全局变量'),
                        okText: I18n.t('template.提交'),
                    };
                }
                return {
                    title: I18n.t('template.编辑全局变量'),
                    okText: I18n.t('template.保存'),
                };
            },
        },
        watch: {
            list: {
                handler (value) {
                    this.variable = _.cloneDeep(value);
                },
                immediate: true,
            },
        },
        created () {
            this.batchOperationMediaQuery = [1080, 1280, 1520, 1800];
        },
        methods: {
            /**
             * @desc 更新外部数据
             */
            triggerChange () {
                this.$emit('on-change', this.variable);
            },
            /**
             * @desc 显示全局变量详情tips
             * @param {Object} variableInfo 全局变量详情
             */
            handleShowPopoverDetail (variableInfo) {
                this.currentPopoverDetail = variableInfo;
            },
            /**
             * @desc 隐藏全局变量详情tips
             */
            handleHidePopoverDetail () {
                this.currentPopoverDetail = {};
            },
            /**
             * @desc 批量编辑
             */
            handleShowBatchOperation () {
                this.isShowBatchOperation = true;
            },
            /**
             * @desc 批量编辑执行方案中变量值
             */
            handleShowBatchEditOfPlan () {
                this.isShowBatchEditOfPlan = true;
            },
            /**
             * @desc 点击全局变量
             * @param {Object} variableInfo 全局变量详情
             * @param {Index} index 点击变量的索引
             */
            handlerOperation (variableInfo, index) {
                this.currentData = variableInfo;
                if (this.isView) {
                    this.detailMedia = variableInfo.type === VariableModel.TYPE_HOST ? [960] : [600, 660, 720, 780];
                    this.isShowDetail = true;
                    return;
                }
                this.currentOperation = 'edit';
                this.currentIndex = index;
                if (this.isEditOfPlan) {
                    this.isShowEditOfPlan = true;
                    return;
                }
                this.isShowOperation = true;
            },
            /**
             * @desc 删除全局变量
             * @param {Number} index 删除变量的索引
             */
            handleDelete (index) {
                this.$bkInfo({
                    title: I18n.t('template.确定删除该全局变量？'),
                    subTitle: I18n.t('template.若该变量被步骤引用，请及时检查并更新步骤设置'),
                    confirmFn: () => {
                        const currentVar = this.variable[index];
                        if (currentVar.id > 0) {
                            // 删除已存在的变量
                            //  —设置delete
                            currentVar.delete = 1;
                        } else {
                            // 删除新建的变量
                            //  —直接删除
                            this.variable.splice(index, 1);
                        }
                        this.triggerChange();
                    },
                });
            },
            /**
             * @desc 显示新建全局变量弹层
             */
            handleCreate () {
                this.currentOperation = 'create';
                this.currentData = {};
                this.isShowOperation = true;
                this.$refs.globalVar && this.$refs.globalVar.reset();
            },
            /**
             * @desc 执行方案全局变量编辑
             * @param {Object} payload 全局变量数据
             */
            handlePlanEditSubmit (payload) {
                const variable = new VariableModel(payload);
                this.variable.splice(this.currentIndex, 1, variable);
                this.triggerChange();
            },
            /**
             * @desc 批量编辑执行方案全局变量
             * @param {Array} variableList 全局变量列表
             */
            handleBatchPlanEditSubmit (variableList) {
                const variables = variableList.map(item => new VariableModel(item));
                this.variable = variables;
                this.triggerChange();
            },
            /**
             * @desc 全局变量编辑
             * @param {Object} payload 全局变量数据
             */
            handleOperationSubmit (payload) {
                const payloadModel = new VariableModel(payload);
                if (this.currentOperation === 'create') {
                    // 新建变量——追加
                    this.variable.push(payloadModel);
                } else {
                    // 编辑变量——替换
                    this.variable.splice(this.currentIndex, 1, payloadModel);
                }
                this.triggerChange();
                this.currentOperation = '';
            },
            /**
             * @desc 全局变量批量编辑
             * @param {Array} variableList 全局变量数据
             */
            handleBatchOperationSubmit (variableList) {
                this.variable = variableList;
                this.triggerChange();
            },
            handleUseGuideToggle () {
                this.isShowUseGuide = !this.isShowUseGuide;
            },
            handleUseGuideClose () {
                this.isShowUseGuide = false;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import "@/css/mixins/media";

    .task-global-variable-box {
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        margin-top: -10px;

        .render-global-variable {
            margin-top: 10px;
            margin-right: 10px;
            cursor: pointer;
            border-color: #3a84ff;

            &.selected {
                .global-variable-content {
                    border-color: #3a84ff;
                }
            }

            &.not-selected {
                .variable-type {
                    background: #dcdee5;
                }
            }
        }

        .global-variable-content {
            position: relative;
            display: flex;
            width: 160px;
            height: 50px;
            padding: 0 10px;
            background: #fff;
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            box-sizing: border-box;
            transition: all 0.15s;
            align-items: center;

            &:hover {
                background: #e1ecff;
                border-color: #3a84ff;

                .variable-delete-btn {
                    opacity: 100%;
                    visibility: visible;
                    transform: scale(1);
                }
            }
        }

        .variable-type {
            display: flex;
            width: 30px;
            height: 30px;
            font-size: 18px;
            color: #fff;
            background: #3a84ff;
            border-radius: 2px;
            align-items: center;
            justify-content: center;
            flex: 0 0 30px;
            transition: all 0.15s;
        }

        .variable-delete-btn {
            position: absolute;
            top: -9px;
            right: -9px;
            width: 18px;
            height: 18px;
            font-size: 14px;
            line-height: 18px;
            color: #fff;
            cursor: pointer;
            background: #c4c6cc;
            border-radius: 50%;
            opacity: 0%;
            visibility: hidden;
            transform: scale(0.8);
            transition: all 0.25s;
        }

        .variable-info {
            display: flex;
            padding-left: 10px;
            font-size: 12px;
            line-height: 1;
            color: #979ba5;
            flex-direction: column;

            .variable-name {
                height: 16px;
                max-width: 100px;
                margin-bottom: 2px;
                overflow: hidden;
                font-size: 14px;
                color: #63656e;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            .variable-description {
                height: 14px;
                max-width: 138px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
        }

        .global-variable-new {
            position: relative;
            width: 160px;
            margin-top: 10px;

            .new-btn,
            .use-guide {
                cursor: pointer;

                &:hover,
                &.active {
                    color: #3a84ff;
                    border-color: #3a84ff;

                    i {
                        color: #3a84ff;
                    }
                }
            }

            .new-btn {
                display: flex;
                width: 100%;
                height: 50px;
                font-size: 14px;
                color: #979ba5;
                border: 1px dashed #c4c6cc;
                border-radius: 2px;
                box-sizing: border-box;
                align-items: center;
                justify-content: center;

                .create-flag {
                    margin-right: 5px;
                    color: #c4c6cc;
                }
            }

            .use-guide {
                position: absolute;
                top: 50%;
                right: -29px;
                display: flex;
                width: 30px;
                font-size: 14px;
                color: #979ba5;
                justify-content: center;
                align-items: center;
                transform: translateY(-50%);
            }
        }

        .global-variable-content,
        .global-variable-new {
            @media (--small-viewports) {
                width: 160px;
            }

            @media (--medium-viewports) {
                width: 180px;
            }

            @media (--large-viewports) {
                width: 200px;
            }

            @media (--huge-viewports) {
                width: 220px;
            }
        }

        .step-mode-diff {
            cursor: default;

            &::after {
                position: absolute;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                content: "";
            }

            .global-variable-content {
                &.same {
                    border-color: #c4c6cc;

                    .variable-type {
                        background: #979ba5;
                    }
                }

                &.new {
                    &::after {
                        position: absolute;
                        top: -6px;
                        right: -6px;
                        width: 28px;
                        height: 14px;
                        font-size: 12px;
                        line-height: 14px;
                        color: #fff;
                        text-align: center;
                        background: #ffa86e;
                        content: "new";
                    }
                }

                &.new,
                &.different {
                    position: relative;
                    border-color: #f9c9a9;

                    .variable-type {
                        background: #f5c09e;
                    }
                }

                &.delete {
                    border-color: #dcdee5;

                    .variable-type {
                        background: #dcdee5;
                    }

                    .variable-info {
                        .variable-name,
                        .variable-description {
                            color: #c4c6cc;
                            text-decoration: line-through;
                        }
                    }
                }
            }
        }
    }
</style>
