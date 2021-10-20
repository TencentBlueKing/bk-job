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
    <div id="templateStepRender">
        <div class="task-step-container" :class="stepContainerClasses">
            <template v-for="(step, index) in steps">
                <components
                    v-if="step.delete !== 1"
                    :is="stepBoxCom"
                    :list="[step]"
                    :group="{
                        name: 'step',
                        pull: 'clone',
                        put: dragStartIndex !== index,
                    }"
                    :key="`${index}_${step.id}`"
                    @start="handleDragStart(index)"
                    :move="handleDragMove"
                    @add="handleDragAdd(index)"
                    :index="index"
                    class="step-drag-box"
                    :class="{
                        sort: isOperation && dragStartIndex > -1,
                    }">
                    <div
                        class="step-wraper"
                        :class="{
                            'step-mode-diff': isDiff,
                            'select': isSelect,
                            'not-select': isSelect && !selectValue.includes(step.id),
                            active: index === currentIndex,
                        }"
                        :order="genOrder()">
                        <div
                            class="render-task-step"
                            :class="[diff[step.id] && diff[step.id].type]">
                            <div class="step-content" @click="handleStepClick(index)">
                                <div class="step-icon">
                                    <Icon :type="step.icon" />
                                </div>
                                <div class="step-name">
                                    <div class="step-name-text">{{ step.name || '--' }}</div>
                                    <!-- 执行脚本，引用脚本的状态 -->
                                    <div v-html="step.scriptStatusHtml" />
                                </div>
                                <Icon v-if="isOperation" type="move" class="draggable-flag" />
                            </div>
                            <div v-if="isOperation" class="step-operation">
                                <Icon
                                    type="plus-circle"
                                    class="operation"
                                    @click="handleShowCreate(index)" />
                                <Icon
                                    type="minus-circle"
                                    class="operation"
                                    @click="handleDel(index)" />
                                <Icon
                                    type="edit-2"
                                    class="operation"
                                    @click="handleShowEdit(index)" />
                                <Icon
                                    type="step-copy"
                                    class="operation"
                                    :tippy-tips="$t('template.克隆步骤')"
                                    @mouseover="handleCloneStepHover(index, true)"
                                    @mouseout="handleCloneStepHover(index, false)"
                                    @click="handleCloneStep(index)" />
                            </div>
                            <div v-if="isSelect" class="select-flag" @click="handleStepSelect(step)">
                                <div class="select-checked" />
                            </div>
                            <template v-if="isEdit">
                                <!-- 编辑状态的新建步骤需要标记出来 -->
                                <div v-if="!step.id" class="step-new-flag">new</div>
                            </template>
                            <!-- 本地验证不通过标记 -->
                            <div v-if="step.localValidator === false" class="need-validate-falg">
                                {{ $t('template.待补全') }}
                            </div>
                        </div>
                        <!-- 执行方案同步diff标记 -->
                        <div class="diff-order" v-if="diff[step.id] && diff[step.id].type === 'move'">
                            <div v-if="diff[step.id].value !== 0" class="order-change">
                                <Icon :type="`${diff[step.id].value < 0 ? 'down-arrow' : 'up-arrow'}`" />
                                {{ Math.abs(diff[step.id].value) }}
                            </div>
                            <span v-else class="order-normal">-</span>
                        </div>
                    </div>
                </components>
            </template>
            <div
                v-if="isOperation"
                class="step-create-btn"
                key="create"
                v-test="{ type: 'button', value: 'create_step' }"
                @click="handleShowCreate(-1)">
                <Icon type="plus" class="action-flag" />
                {{ $t('template.作业步骤.add') }}
            </div>
        </div>
        <lower-component v-if="isView">
            <jb-sideslider
                :is-show.sync="isShowDetail"
                :title="$t('template.查看作业步骤')"
                :show-footer="false"
                :quick-close="true"
                :width="896"
                :media="mediaQueryMap">
                <task-step-view
                    v-if="isShowDetail"
                    ref="stepViewRef"
                    :variable="hostVariables"
                    :data="detailInfo" />
            </jb-sideslider>
        </lower-component>
        <lower-component v-if="isOperation">
            <jb-sideslider
                id="taskStepOperationSideslider"
                :is-show.sync="isShowOperation"
                v-bind="operationSideSliderInfo"
                :width="916"
                :media="mediaQueryMap">
                <task-step-operation
                    v-if="isShowOperation"
                    ref="taskStep"
                    :variable="hostVariables"
                    :script-variables="scriptVariables"
                    :data="operationData"
                    @on-change="handleTaskStepSubmit" />
            </jb-sideslider>
        </lower-component>
    </div>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import Draggable from 'vuedraggable';
    import TaskStepModel from '@model/task/task-step';
    import JbSideslider from '@components/jb-sideslider';
    import TaskStepOperation from './task-step';
    import TaskStepView from './task-step-view';

    export default {
        name: 'RenderTaskStep',
        components: {
            Draggable,
            JbSideslider,
            TaskStepOperation,
            TaskStepView,
        },
        props: {
            list: {
                type: Array,
                required: true,
            },
            /*
             * @value '': 仅可查看详情
             * @value 'operate': 可编辑可新建可删除
             * @value 'select': 选择模式
             * @value 'diff': diff场景
             */
            mode: {
                type: String,
                default: '',
            },
            selectValue: {
                type: Array,
                default: () => [],
            },
            // 全局变量 list
            variable: {
                type: Array,
                default: () => [],
            },
            diff: {
                type: Object,
                default: () => ({}),
            },
        },
        data () {
            return {
                steps: [],
                isShowOperation: false,
                isShowDetail: false,
                isShowClone: false,
                operationType: '',
                currentIndex: -1,
                operationData: {},
                detailInfo: {},
                dragStartIndex: -1,
                mediaQueryMap: [1000, 1100, 1200, 1300],
            };
        },
        computed: {
            /**
             * @desc 步骤有id说明是编辑状态
             * @return {Boolean}
             */
            isEdit () {
                // eslint-disable-next-line no-plusplus
                for (let i = 0; i < this.list.length; i++) {
                    if (this.list[i].id) {
                        return true;
                    }
                }
                return false;
            },
            /**
             * @desc 操作模板步骤（新建、编辑）
             * @return {Boolean}
             */
            isOperation () {
                return this.mode === 'operation';
            },
            /**
             * @desc 选择模板步骤（用于编辑执行的场景）
             * @return {Boolean}
             */
            isSelect () {
                return this.mode === 'select';
            },
            /**
             * @desc 查看作业模板步骤
             * @return {Boolean}
             */
            isView () {
                return !this.mode || this.mode === 'select';
            },
            /**
             * @desc 作业模板步骤同比查看差异
             * @return {Boolean}
             */
            isDiff () {
                return this.mode === 'diff';
            },
            /**
             * @desc 作业模板步骤可以编辑时需要能拖动排序
             * @return {String}
             */
            stepBoxCom () {
                if (this.isOperation) {
                    return 'draggable';
                }
                return 'div';
            },
            /**
             * @desc 步骤编辑的样式 class
             * @return {Object}
             */
            stepContainerClasses () {
                const classes = {};
                if (!this.isShowOperation && !this.isShowDetail && !this.isShowClone) {
                    return classes;
                }
                classes[this.operationType] = true;
                return classes;
            },
            /**
             * @desc 步骤的执行目标只能选中主机变量
             * @return {Array}
             */
            hostVariables () {
                return this.variable.filter(item => !item.delete && item.isHost);
            },
            /**
             * @desc 脚本步骤中脚本内容可使用非主机变量
             * @return {Array}
             */
            scriptVariables () {
                return this.variable.filter(item => !item.delete && !item.isHost);
            },
            /**
             * @desc 步骤操作弹层的信息
             * @return {Object}
             */
            operationSideSliderInfo () {
                if (this.operationType === 'create') {
                    return {
                        title: I18n.t('template.新建作业步骤'),
                        okText: I18n.t('template.提交'),
                    };
                }
                return {
                    title: I18n.t('template.编辑作业步骤'),
                    okText: I18n.t('template.保存'),
                };
            },
        },
        watch: {
            list: {
                handler (list) {
                    if (this.isOperation) {
                        if (list.length < 1) {
                            this.steps = [];
                            return;
                        }
                    }
                    this.steps = Object.freeze([...list]);
                },
                immediate: true,
            },
            steps: {
                handler () {
                    this.order = 0;
                },
                immediate: true,
            },
        },
        methods: {
            /**
             * @desc 外部调用——点击指定 index 的步骤
             */
            clickStepByIndex (index) {
                this.handleStepClick(index);
            },
            genOrder () {
                this.order += 1;
                return this.order;
            },
            /**
             * @desc 鼠标点击某个步骤
             * @param {Object} payload 点击的模板步骤数据
             * @param {Number} index 点击的模板步骤索引
             */
            handleStepClick (index) {
                if (this.isOperation) {
                    // 编辑步骤
                    this.handleShowEdit(index);
                    return;
                }
                // 查看步骤详情
                this.operationType = 'detail';
                this.detailInfo = this.steps[index];
                this.isShowDetail = true;
            },
            /**
             * @desc 操作执行方案步骤时，选择作业模板的步骤
             * @param {Object} step 点击的模板步骤数据
             */
            handleStepSelect (step) {
                this.$emit('on-select', step);
            },
            /**
             * @desc 显示编辑作业模板步骤的弹层
             * @param {Number} index 点击的模板步骤索引
             */
            handleShowEdit (index) {
                this.operationType = 'edit';
                this.currentIndex = index;
                this.operationData = this.steps[index];
                this.isShowOperation = true;
            },
            /**
             * @desc 鼠标hover克隆按钮时需要显示克隆步骤的placeholder
             * @param {Number} index 点击的模板步骤索引
             * @param {Boolean} isHover 鼠标的hover状态
             */
            handleCloneStepHover (index, isHover) {
                if (isHover) {
                    this.currentIndex = index;
                    this.operationType = 'clone';
                    this.isShowClone = true;
                } else {
                    this.currentIndex = -1;
                    this.operationType = '';
                    this.isShowClone = false;
                }
            },
            /**
             * @desc 克隆作业模板步骤
             * @param {Number} index 点击的模板步骤索引
             */
            handleCloneStep (index) {
                const steps = [...this.steps];

                let newStep = {
                    ...steps[index],
                };
                newStep.id = 0;
                newStep.name = `${newStep.name}_copy`;
                newStep = new TaskStepModel(newStep, true);
                steps.splice(index + 1, 0, newStep);

                this.steps = Object.freeze(steps);
                this.handleCloneStepHover(-1, false);
                this.$emit('on-change', steps);
            },
            /**
             * @desc 点击作业模板新建按钮在对应步骤后面追加一个新步骤
             * @param {Number} index 点击的模板步骤索引
             */
            handleShowCreate (index) {
                this.operationType = 'create';
                this.currentIndex = index;
                this.operationData = {};
                this.isShowOperation = true;
            },
            /**
             * @desc 删除作业模板新建
             * @param {Number} index 将要的模板步骤索引
             */
            handleDel (index) {
                this.$bkInfo({
                    title: I18n.t('template.确定删除该步骤？'),
                    subTitle: I18n.t('template.删除之后不可恢复，请谨慎操作！'),
                    confirmFn: () => {
                        const steps = [...this.steps];

                        const currentStep = steps[index];
                        if (currentStep.id > 0) {
                            // 删除已存在的步骤
                            //  —设置delete
                            currentStep.delete = 1;
                        } else {
                            // 删除新建的步骤
                            //  —直接删除
                            steps.splice(index, 1);
                        }

                        this.steps = Object.freeze(steps);
                        this.$emit('on-change', steps);
                    },
                });
            },
            /**
             * @desc 提交作业模板步骤的操作
             * @param {Number} payload 将要的模板步骤索引
             * @param {Boolean} localValidator 表单验证结果
             */
            handleTaskStepSubmit (payload, localValidator) {
                const operationStep = new TaskStepModel(payload);
                const steps = [...this.steps];

                // 重要！！！
                // 新建脚本过程中不做验证，但要给出验证不通过的标记
                operationStep.localValidator = localValidator;

                if (this.operationType === 'create') {
                    if (this.currentIndex === -1) {
                        steps.push(operationStep);
                    } else {
                        steps.splice(this.currentIndex + 1, 0, operationStep);
                    }
                } else {
                    steps.splice(this.currentIndex, 1, operationStep);
                }

                this.steps = Object.freeze(steps);
                this.$emit('on-change', steps);
            },
            /**
             * @desc 选中拖动的作业模板步骤
             * @param {Number} index 拖动的模板步骤索引
             */
            handleDragStart (index) {
                this.dragStartIndex = index;
            },
            /**
             * @desc 拖动作业模板步骤
             * @param {Object} event 拖动事件
             * @return {Boolean}
             */
            handleDragMove: _.throttle(function (event) {
                this.dragFutureIndex = event.draggedContext.futureIndex;
                return true;
            }, 30),
            /**
             * @desc 拖动结束
             * @param {Number} index 拖动的模板步骤索引
             */
            handleDragAdd (index) {
                if (this.dragStartIndex === index) {
                    return;
                }
                if (this.dragStartIndex - index === 1) {
                    if (this.dragFutureIndex === 1) {
                        return;
                    }
                }
                if (this.dragStartIndex - index === -1) {
                    if (this.dragFutureIndex === 0) {
                        return;
                    }
                }
                const steps = [...this.steps];

                const startStep = steps[this.dragStartIndex];
                steps.splice(this.dragStartIndex, 1);
                steps.splice(index, 0, startStep);

                this.dragStartIndex = -1;
                this.steps = Object.freeze(steps);
                this.$emit('on-change', steps);
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import '@/css/mixins/media';

    @define-mixin step-active {
        .step-content {
            color: #3a84ff;
            background: #e1ecff;
            border-color: #3a84ff;
        }

        .step-icon {
            color: #3a84ff;
            background: #e1ecff;
            border-color: transparent !important;
        }

        .draggable-flag {
            display: block;
        }
    }

    .task-step-container {
        display: flex;
        flex-direction: column;
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

        &.edit,
        &.create {
            .step-wraper {
                &.active {
                    .render-task-step {
                        @mixin step-active;
                    }
                }
            }
        }

        &.create,
        &.clone {
            .step-wraper {
                &.active {
                    &::after {
                        display: block;
                        height: 42px;
                        margin-top: 10px;
                        background: #e1ecff;
                        border-radius: 2px;
                        content: '';
                    }
                }
            }
        }
    }

    .step-drag-box {
        position: relative;

        &:hover,
        &.sort {
            .step-wraper {
                &::before {
                    content: attr(order);
                }
            }
        }

        &:hover {
            .step-wraper {
                &::before {
                    color: #3a84ff;
                    background: #e1ecff;
                    border-color: #3a84ff;
                }
            }
        }

        & ~ .step-drag-box {
            margin-top: 10px;
        }

        .step-wraper {
            position: relative;

            &::before {
                position: absolute;
                top: 0;
                left: -2px;
                height: 16px;
                padding: 0 5px;
                font-size: 12px;
                font-weight: bold;
                line-height: 16px;
                color: #63656e;
                background: #f0f1f5;
                border: 1px solid #c4c6cc;
                border-radius: 2px;
                transform: translateX(-100%);
            }

            &.sortable-ghost {
                height: 42px;
                background: #e1ecff;

                &::before {
                    content: none !important;
                }

                .render-task-step {
                    display: none;
                }
            }
        }
    }

    .step-wraper {
        cursor: pointer;

        &.is-hide {
            display: none;
        }

        &.select {
            .step-content,
            .step-icon {
                border-color: #3a84ff;
            }

            .step-icon {
                color: #3a84ff;
                background: #e1ecff;
            }

            .select-flag {
                position: absolute;
                top: 0;
                right: 0;
                bottom: 0;
                display: flex;
                align-items: center;
                padding-right: 22px;
                padding-left: 20px;

                &::before {
                    position: absolute;
                    width: 1px;
                    height: 22px;
                    margin-left: -22px;
                    background: #d8d8d8;
                    content: '';
                }

                .select-checked {
                    position: relative;
                    display: block;
                    width: 18px;
                    height: 18px;
                    background: #3a84ff;
                    border: 1px solid #3a84ff;
                    border-radius: 50%;

                    &::after {
                        position: absolute;
                        top: 1px;
                        left: 5px;
                        width: 4px;
                        height: 8px;
                        border: 2px solid #fff;
                        border-top: 0;
                        border-left: 0;
                        content: "";
                        transform: rotate(45deg) scale(1);
                        transition: all 0.15s;
                        transform-origin: center;
                    }
                }
            }
        }

        &.not-select {
            .step-content {
                color: #979ba5;
                background: transparent;
                border-color: #dcdee5;
            }

            .step-icon {
                color: #c4c6cc;
                background: #f0f1f5;
                border-color: #dcdee5;
            }

            .select-flag {
                .select-checked {
                    background: transparent;
                    border-color: #c4c6cc;

                    &::after {
                        transform: scale(0);
                    }
                }
            }

            &:hover {
                .step-content {
                    color: #63656e;
                    background: #e1ecff;
                    border-color: #3a84ff;
                }

                .select-flag {
                    border-color: #3a84ff;
                }

                .step-icon {
                    color: #3a84ff;
                    background: #e1ecff;
                    border-color: transparent;
                }
            }
        }
    }

    .render-task-step {
        position: relative;

        &:hover {
            .step-operation {
                display: flex;
            }

            .need-validate-falg {
                display: none;
            }

            @mixin step-active;
        }

        &:last-child {
            margin-bottom: 0;
        }

        .step-content {
            display: flex;
            height: 42px;
            color: #63656e;
            background: #fff;
            border: 1px solid #c4c6cc;
            border-radius: 2px;
            box-sizing: border-box;

            &.active {
                background: #e1ecff;
                border-color: #3a84ff;

                .step-icon {
                    background: inherit;
                    border-color: transparent;
                }
            }
        }

        .step-icon {
            display: flex;
            height: 100%;
            font-size: 16px;
            background: #f0f1f5;
            border-right: 1px solid #c4c6cc;
            align-items: center;
            justify-content: center;
            flex: 0 0 42px;
        }

        .draggable-flag {
            position: absolute;
            top: 50%;
            right: 0;
            display: none;
            font-size: 33px;
            color: #3a84ff;
            transform: translateY(-50%);
        }

        .step-name {
            position: relative;
            display: flex;
            align-items: center;
            padding-left: 9px;

            .step-name-text {
                max-width: 400px;
                overflow: hidden;
                font-size: 14px;
                text-overflow: ellipsis;
                white-space: nowrap;
            }

            &:hover {
                color: #3a84ff;
            }
        }

        .step-update {
            position: absolute;
            right: -14px;
            width: 6px;
            height: 6px;
            background: #ff5656;
            border-radius: 50%;
        }

        .step-operation {
            position: absolute;
            top: 50%;
            right: 0;
            display: none;
            align-items: center;
            height: 40px;
            padding-left: 12px;
            font-size: 18px;
            color: #c4c6cc;
            transform: translateX(100%) translateY(-50%);

            .operation {
                padding: 12px 8px 12px 0;
                cursor: pointer;

                &.active,
                &:hover {
                    color: #3a84ff;
                }
            }
        }

        .select-flag {
            display: none;
        }

        .step-new-flag {
            position: absolute;
            top: -7px;
            right: -8px;
            display: flex;
            width: 32px;
            height: 16px;
            font-size: 12px;
            color: #fff;
            text-align: center;
            background: #3a84ff;
            border-radius: 2px;
            align-items: center;
            justify-content: center;
        }

        .need-validate-falg {
            position: absolute;
            top: 50%;
            right: 0;
            height: 18px;
            padding: 0 4px;
            margin-right: -12px;
            font-size: 12px;
            line-height: 18px;
            color: #ea3636;
            background: #fdd;
            transform: translateY(-50%) translateX(100%);

            &::before {
                position: absolute;
                width: 0;
                height: 0;
                margin-left: -22px;
                border: 9px solid transparent;
                border-right-color: #fdd;
                content: '';
            }

            &::after {
                position: absolute;
                top: 50%;
                left: -4px;
                width: 4px;
                height: 4px;
                background: #fff;
                border-radius: 50%;
                content: '';
                transform: translateY(-50%);
            }
        }
    }

    .step-create-btn {
        display: flex;
        width: 100%;
        height: 42px;
        margin-top: 10px;
        font-size: 14px;
        color: #979ba5;
        cursor: pointer;
        border: 1px dashed #c4c6cc;
        align-items: center;
        justify-content: center;

        &:hover {
            color: #3a84ff;
            border-color: #3a84ff;

            .action-flag {
                color: #3a84ff;
            }
        }

        .action-flag {
            margin-right: 5px;
            color: #c4c6cc;
        }
    }

    /* diff 对比的样式 */
    .step-mode-diff {
        position: relative;

        &::after {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            content: '';
        }

        .render-task-step {
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
                    content: 'new';
                }
            }

            &.new,
            &.move,
            &.different {
                position: relative;

                .step-content {
                    border-color: #f9c9a9;
                }

                .step-icon {
                    color: #f5c09e;
                    background: #ffefe4;
                    border-right-color: #f9c9a9;
                }
            }

            &.delete {
                .step-content {
                    color: #c4c6cc;
                    border-color: #dcdee5;
                }

                .step-icon {
                    color: #dcdee5;
                    background: #f5f7fa;
                    border-right-color: #dcdee5;
                }

                .step-name-text {
                    text-decoration: line-through;
                }
            }
        }

        .diff-order {
            position: absolute;
            top: 50%;
            right: 0;
            width: 50px;
            padding-left: 15px;
            font-size: 14px;
            transform: translate(100%, -50%);

            .order-change {
                color: #f98d45;
            }

            .order-normal {
                color: #63656e;
            }
        }
    }
</style>
