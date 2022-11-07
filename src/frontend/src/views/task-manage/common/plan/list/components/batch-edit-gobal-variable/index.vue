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
    <jb-sideslider
        ref="root"
        :is-show="isShow"
        :show-footer="!isLoading && isGlobalVariableNotEmpty"
        :title="$t('template.批量编辑变量值')"
        :width="900"
        @update:isShow="handleCancel">
        <div
            v-if="isLoading"
            v-bkloading="{ isLoading }"
            style="min-height: 100px;" />
        <div v-else>
            <div
                v-if="isGlobalVariableNotEmpty"
                class="batch-edit-global-variable-box">
                <div class="edit-header">
                    <bk-steps
                        :cur-step.sync="curStep"
                        :steps="steps"
                        style="width: 300px;" />
                </div>
                <keep-alive exclude="editPreview">
                    <component
                        :is="stepCom"
                        v-if="planList.length > 0"
                        ref="handler"
                        :latest-value-map="globalVariableValueMap"
                        :plan-list="planList"
                        @on-edit-change="handleEditChange" />
                </keep-alive>
            </div>
            <Empty
                v-else
                style="margin-top: 126px;">
                <div style="font-size: 20px; line-height: 26px; color: #63656e;">
                    {{ $t('template.暂无全局变量') }}
                </div>
                <div
                    slot="desc"
                    style="margin-top: 12px; font-size: 14px; line-height: 19px; color: #979ba5;">
                    {{ $t('template.所选执行方案，无变量值可编辑') }}
                </div>
            </Empty>
        </div>
        <div slot="footer">
            <template v-if="curStep === 1">
                <bk-button
                    class="mr10"
                    :disabled="isPreviewDisabled"
                    theme="primary"
                    @click="handlePreview">
                    {{ $t('template.下一步') }}
                </bk-button>
            </template>
            <template v-if="curStep === 2">
                <jb-popover-confirm
                    v-if="hasEmptyValueVariable"
                    :confirm-handler="handleSubmit"
                    :content="$t('template.请注意！变量值填框留空，即代表设置目标变量为空值。')"
                    :title="$t('template.确定立即批量更新？')">
                    <bk-button
                        class="mr10"
                        theme="primary">
                        {{ $t('template.提交') }}
                    </bk-button>
                </jb-popover-confirm>
                <bk-button
                    v-else
                    class="mr10"
                    theme="primary"
                    @click="handleSubmit">
                    {{ $t('template.提交') }}
                </bk-button>
                <bk-button
                    class="mr10"
                    theme="primary"
                    @click="handleChangeStep(1)">
                    {{ $t('template.上一步') }}
                </bk-button>
            </template>
            <bk-button @click="handleCancel">
                {{ $t('template.取消') }}
            </bk-button>
        </div>
    </jb-sideslider>
</template>
<script>
    import _ from 'lodash';

    import TaskPlanService from '@service/task-plan';

    import { leaveConfirm } from '@utils/assist';

    import EditPreview from './edit-preview';
    import EditValue from './edit-value';
    import { genGlobalVariableKey } from './utils';

    import I18n from '@/i18n';

    export default {
        name: '',
        components: {
            EditValue,
            EditPreview,
        },
        model: {
            prop: 'isShow',
            event: 'input',
        },
        props: {
            isShow: {
                type: Boolean,
                default: false,
            },
            data: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                isLoading: true,
                curStep: 1,
                isGlobalVariableNotEmpty: false,
                planList: [],
                isPreviewDisabled: true, // 选择的变量为空不能进行下一步
                globalVariableValueMap: {}, // 全局变量编辑的值缓存
            };
        },
        computed: {
            stepCom () {
                const comMap = {
                    1: EditValue,
                    2: EditPreview,
                };
                return comMap[this.curStep];
            },
            /**
             * @desc 编辑的变量中是否有值为空的变量
             * @return {Boolean}
            */
            hasEmptyValueVariable () {
                for (const variableKey in this.globalVariableValueMap) {
                    const variableValue = this.globalVariableValueMap[variableKey];
                    if (!variableValue) {
                        return true;
                    }
                    if (_.isObject(variableValue) && variableValue.isEmpty) {
                        return true;
                    }
                }
                return false;
            },
        },
        watch: {
            isShow: {
                handler (isShow) {
                    if (isShow) {
                        this.fetchPlanDetailData();
                    }
                },
                immediate: true,
            },
        },
        created () {
            this.steps = [
                {
                    title: I18n.t('template.编辑变量'),
                    icon: 1,
                },
                {
                    title: I18n.t('template.更新预览'),
                    icon: 2,
                },
            ];
        },
        methods: {
            /**
             * @desc 获取所选执行方案的详情数据
             *
             * 得到执行方案详情数据后需要判断所有的执行方案中是否有全局变量
             */
            fetchPlanDetailData () {
                this.isLoading = true;
                this.isGlobalVariableNotEmpty = false;
                TaskPlanService.fetchBatchPlan({
                    planIds: this.data.map(_ => _.id).join(','),
                }).then((data) => {
                    this.planList = Object.freeze(data);
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < this.planList.length; i++) {
                        if (this.planList[i].variableList.length > 0) {
                            this.isGlobalVariableNotEmpty = true;
                            break;
                        }
                    }
                })
                    .finally(() => {
                        this.isLoading = false;
                    });
            },
            handleEditChange (globalVariableValueMap) {
                this.isPreviewDisabled = Object.values(globalVariableValueMap).length < 1;
            },
            /**
             * @desc 修改预览
             * @param {Number} step 指定的步骤
             *
             * 每次切换步骤时内容区需要滚动到顶部
             */
            handlePreview () {
                this.globalVariableValueMap = Object.freeze(this.$refs.handler.getEditValue());
                this.handleChangeStep(2);
            },
            /**
             * @desc 切换步骤
             * @param {Number} step 指定的步骤
             *
             * 每次切换步骤时内容区需要滚动到顶部
             */
            handleChangeStep (step) {
                this.curStep = step;
                this.$refs.root.$el.querySelector('.bk-sideslider-content').scrollTop = 0;
            },
            /**
             * @desc 提交变量编辑
            */
            handleSubmit () {
                const planList = this.$refs.handler.getRelatePlanList();
                
                const stack = [];
                planList.forEach((currentPlan) => {
                    const variableInfoList = [];
                    const currentData = {
                        planId: currentPlan.id,
                        templateId: currentPlan.templateId,
                        variableInfoList,
                    };
                    currentPlan.variableList.forEach((variableData) => {
                        const variableKey = genGlobalVariableKey(variableData);
                        if (Object.prototype.hasOwnProperty.call(this.globalVariableValueMap, variableKey)) {
                            if (variableData.isHost) {
                                variableInfoList.push({
                                    ...variableData,
                                    defaultTargetValue: this.globalVariableValueMap[variableKey],
                                });
                            } else {
                                variableInfoList.push({
                                    ...variableData,
                                    defaultValue: this.globalVariableValueMap[variableKey],
                                });
                            }
                        } else {
                            variableInfoList.push(variableData);
                        }
                    });
                    stack.push(currentData);
                });
                return TaskPlanService.batchUpdateVariable(stack)
                    .then(() => {
                        window.changeConfirm = false;
                        this.messageSuccess(I18n.t('template.编辑成功'));
                        this.$emit('on-success');
                        // settimeout 保证 jb-popover-confirm能被先关闭
                        setTimeout(() => {
                            this.handleCancel();
                        });
                    });
            },
            /**
             * @desc 取消编辑
             */
            handleCancel () {
                leaveConfirm()
                    .then(() => {
                        this.curStep = 1;
                        this.planList = [];
                        this.globalVariableValueMap = {};
                        this.isPreviewDisabled = true;
                        this.$emit('change', false);
                        this.$emit('input', false);
                    });
            },
        },
    };
</script>
<style lang='postcss'>
    .batch-edit-global-variable-box {
        .edit-header {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 56px;
            margin: -20px -30px 0;
            background: #f0f1f5;
        }
    }
</style>
