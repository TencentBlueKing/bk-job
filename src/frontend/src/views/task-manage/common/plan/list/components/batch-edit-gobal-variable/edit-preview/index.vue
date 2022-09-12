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
    <div class="batch-preview-plan-global-variable">
        <render-related-info
            v-for="(list, key) in globalVariableRelatePlanMap"
            :key="key"
            ref="relate"
            :latest-value="latestValueMap[key]"
            :relate-list="list" />
    </div>
</template>
<script>
    import {
        genGlobalVariableKey,
    } from '../utils';

    import RenderRelatedInfo from './components/render-related-info';
    
    export default {
        name: 'EditPreview',
        components: {
            RenderRelatedInfo,
        },
        props: {
            planList: {
                type: Array,
                required: true,
            },
            latestValueMap: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {
                globalVariableRelatePlanMap: {},
            };
        },
        created () {
            this.relatePlanMap = {};
            this.traverPlanList();
        },
        methods: {
            /**
             * @desc 遍历执行方案全局变量
             */
            traverPlanList () {
                const globalVariableRelatePlanMap = {};
                this.planList.forEach((planData) => {
                    planData.variableList.forEach((variableData) => {
                        const variableKey = genGlobalVariableKey(variableData);
                        const relateData = {
                            plan: planData,
                            globalVariable: variableData,
                        };
                        if (Object.prototype.hasOwnProperty.call(this.latestValueMap, variableKey)) {
                            if (globalVariableRelatePlanMap[variableKey]) {
                                globalVariableRelatePlanMap[variableKey].push(relateData);
                            } else {
                                globalVariableRelatePlanMap[variableKey] = [
                                    relateData,
                                ];
                            }
                            this.relatePlanMap[planData.id] = planData;
                        }
                    });
                });
                
                this.globalVariableRelatePlanMap = Object.freeze(globalVariableRelatePlanMap);
                // 默认站看第一个全局变量的信息
                this.$nextTick(() => {
                    this.$refs.relate[0].handleToggle();
                });
            },
            /**
             * @desc 外部调用，获取需要更新的执行方案
             * @return {Array} 需要更新的执行方案列表
             */
            getRelatePlanList () {
                return Object.values(this.relatePlanMap);
            },
        },
    };
</script>
<style lang='postcss'>
    .batch-preview-plan-global-variable {
        padding-top: 6px;
    }
</style>
