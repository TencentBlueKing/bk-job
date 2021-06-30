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
    <div class="batch-edit-plan-global-variable">
        <div class="action-target-info">
            <span class="label">{{ $t('template.更新范围') }}</span>
            <span class="content-split">:</span>
            <span>
                <span>{{ $t('template.已选') }}</span>
                <span class="number strong">{{ planList.length }}</span>
                <span>{{ $t('template.个执行方案，') }}{{ $t('template.来自') }}</span>
                <span class="number strong">{{ relatedTemplateNums }}</span>
                <span>{{ $t('template.个作业模版') }}</span>
            </span>
        </div>
        <div class="action-target-info" style="margin-top: 10px;">
            <span class="label">{{ $t('template.选择变量') }}</span>
            <span class="content-split" style="color: #ea3636;">*</span>
            <span>{{ selectResultText }}，</span>
            <bk-button v-if="isHasSelectedAll" text @click="handleCancleSelectAll">{{ $t('template.取消全选') }}</bk-button>
            <bk-button v-else text @click="handleSelectAll">{{ $t('template.全选') }}</bk-button>
        </div>
        <div class="global-variable-list">
            <div
                v-for="(variableData, key) in globalVariableMap"
                :key="key"
                class="variable-item"
                :class="{ active: !!selectVariableMap[key] }"
                @click="handleVariableSelect(key)">
                <div class="variable-type">
                    <Icon :type="variableData.icon" />
                </div>
                <div v-bk-overflow-tips class="variable-name">{{ variableData.name }}</div>
                <div class="select-checked" />
            </div>
        </div>
        <div v-if="isSelectNotEmpty" class="global-variable-value">
            <template v-for="(variableData, key) in globalVariableMap">
                <render-global-variable
                    v-if="selectVariableMap[key]"
                    :key="key"
                    :value="selectVariableValueMap[key]"
                    :data="variableData"
                    @on-change="value => handleValueChange(key, value)"
                    @on-remove="handleRemoveSelect(key)" />
            </template>
        </div>
    </div>
</template>
<script>
    import I18n from '@/i18n';
    import TaskHostNodeModel from '@model/task-host-node';
    import RenderGlobalVariable from './components/render-global-variable';
    import {
        genGlobalVariableKey,
    } from '../utils';

    export default {
        name: '',
        components: {
            RenderGlobalVariable,
        },
        inheritAttrs: false,
        props: {
            planList: {
                type: Array,
                required: true,
            },
        },
        data () {
            return {
                relatedTemplateNums: 0,
                globalVariableMap: {},
                selectVariableMap: {},
                selectVariableValueMap: {},
            };
        },
        computed: {
            isHasSelectedAll () {
                const selectNums = Object.values(this.selectVariableMap).length;
                if (selectNums < 1) {
                    return false;
                }
                return selectNums === Object.values(this.globalVariableMap).length;
            },
            selectResultText () {
                return `${I18n.t('template.已选')} ${Object.values(this.selectVariableMap).length} / ${Object.values(this.globalVariableMap).length}`;
            },
            isSelectNotEmpty () {
                return Object.values(this.selectVariableMap).length > 0;
            },
        },
        created () {
            this.traverPlanList();
        },
        methods: {
            /**
             * @desc 遍历执行方案全局变量
             */
            traverPlanList () {
                const templateIdMap = {};
                const globalVariableMap = {};
                this.planList.forEach((planData) => {
                    templateIdMap[planData.templateId] = true;
                    planData.variableList.forEach((variableData) => {
                        globalVariableMap[genGlobalVariableKey(variableData)] = variableData;
                    });
                });
               
                this.relatedTemplateNums = Object.values(templateIdMap).length;
                this.globalVariableMap = Object.freeze(globalVariableMap);
            },
            /**
             * @desc 编辑全局变量值更新
             */
            triggerChange () {
                window.changeAlert = true;
                this.$emit('on-edit-change', Object.assign({}, this.selectVariableValueMap));
            },
            /**
             * @desc 外部调用，获取需要更新的执行方案
             * @return {Object} 需要更新的执行方案列表
             */
            getEditValue () {
                return Object.assign({}, this.selectVariableValueMap);
            },
            /**
             * @desc 单次选中全局变量
             * @param {String} key 全局变量的key
             */
            handleVariableSelect (key) {
                const selectVariableMap = Object.assign({}, this.selectVariableMap);
                const selectVariableValueMap = Object.assign({}, this.selectVariableValueMap);
                if (selectVariableMap[key]) {
                    // 删除选择
                    delete selectVariableMap[key];
                    // 删除值
                    delete selectVariableValueMap[key];
                } else {
                    // 选中变量
                    selectVariableMap[key] = true;
                    // 初始化选中变量值
                    if (this.globalVariableMap[key].isHost) {
                        selectVariableValueMap[key] = new TaskHostNodeModel({});
                    } else {
                        selectVariableValueMap[key] = '';
                    }
                }
                this.selectVariableMap = Object.freeze(selectVariableMap);
                this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
                this.triggerChange();
            },
            /**
             * @desc 全选全局变量
             */
            handleSelectAll () {
                const selectVariableMap = {};
                const selectVariableValueMap = {};
                for (const key in this.globalVariableMap) {
                    // 选中变量
                    selectVariableMap[key] = true;
                    // 初始化选中变量值
                    if (this.globalVariableMap[key].isHost) {
                        selectVariableValueMap[key] = new TaskHostNodeModel({});
                    } else {
                        selectVariableValueMap[key] = '';
                    }
                }
                this.selectVariableMap = Object.freeze(selectVariableMap);
                this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
                this.triggerChange();
            },
            /**
             * @desc 取消全选全局变量
             */
            handleCancleSelectAll () {
                // 删除选择
                this.selectVariableMap = {};
                // 删除值
                this.selectVariableValueMap = {};
                this.triggerChange();
            },
            /**
             * @desc 取消选择单个全局变量
             * @param {String} key 全局变量的key
             *
             * 取消选择时需要清除已编辑的值
             */
            handleRemoveSelect (key) {
                // 删除选择
                const selectVariableMap = Object.assign({}, this.selectVariableMap);
                delete selectVariableMap[key];
                this.selectVariableMap = Object.freeze(selectVariableMap);
                // 删除值
                const selectVariableValueMap = Object.assign({}, this.selectVariableValueMap);
                delete selectVariableValueMap[key];
                this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
                this.triggerChange();
            },
            /**
             * @desc 编辑单个全局变量的值
             * @param {String} key 全局变量的key
             * @param {Any} value 全局变量的value
             */
            handleValueChange (key, value) {
                this.selectVariableValueMap = Object.freeze(Object.assign({}, this.selectVariableValueMap, {
                    [key]: value,
                }));
                this.triggerChange();
            },
        },
    };
</script>
<style lang='postcss'>
    .batch-edit-plan-global-variable {
        .action-target-info {
            display: flex;
            padding-top: 20px;
            font-size: 14px;
            line-height: 20px;
            color: #63656e;

            .label {
                color: #313238;
            }

            .content-split {
                width: 18px;
                text-align: left;
            }
        }

        .global-variable-list {
            display: flex;
            flex-wrap: wrap;
            padding-top: 4px;
            margin: 0 -5px;

            .variable-item {
                position: relative;
                display: flex;
                width: 160px;
                height: 32px;
                padding-right: 26px;
                margin-top: 12px;
                margin-right: 5px;
                margin-left: 5px;
                overflow: hidden;
                color: #63656e;
                cursor: pointer;
                border: 1px solid #dcdee5;
                border-radius: 2px;
                transition: all 0.1s;
                align-items: center;

                .variable-type {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    flex: 0 0 32px;
                    height: 100%;
                    margin-left: -1px;
                    font-size: 17px;
                    color: #fff;
                    background: #d3d5db;
                    transition: all 0.1s;
                }

                .select-checked {
                    position: absolute;
                    top: 9px;
                    right: 6px;
                    bottom: 0;
                    width: 14px;
                    height: 14px;
                    background: #fff;
                    border: 1px solid #c4c6cc;
                    border-radius: 50%;
                }

                &.active {
                    border-color: #3a84ff;

                    .variable-type {
                        background: #3a84ff;
                    }

                    .select-checked {
                        background: #3a84ff;
                        border-color: #3a84ff;

                        &::after {
                            position: absolute;
                            top: 2px;
                            left: 4px;
                            width: 3px;
                            height: 6px;
                            border: 1px solid #fff;
                            border-top: 0;
                            border-left: 0;
                            content: "";
                            transform: rotate(45deg) scale(1);
                            transition: all 0.1s;
                            transform-origin: center;
                        }
                    }
                }
            }

            .variable-name {
                padding-left: 6px;
                overflow: hidden;
                font-size: 13px;
                text-overflow: ellipsis;
                white-space: nowrap;
                flex: 0 1 auto;
                align-items: center;
            }
        }

        .global-variable-value {
            margin-top: 30px;
        }
    }
</style>
