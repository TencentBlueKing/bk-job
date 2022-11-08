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
    <global-variable-layout type="vertical">
        <global-variable
            v-for="variable in usedList"
            :key="variable.id"
            ref="used"
            :data="variable"
            :type="variable.type" />
        <Empty
            v-if="usedList.length < 1"
            key="empty"
            style="height: 160px; max-width: 960px; background-color: #f0f1f5;"
            :title="$t('template.暂无引用的变量')" />
        <toggle-display
            v-if="unusedList.length > 0"
            :count="unusedList.length"
            style="margin-top: 20px;">
            <div style="margin-top: 20px;">
                <global-variable
                    v-for="variable in unusedList"
                    :key="variable.id"
                    ref="unused"
                    :data="variable"
                    :type="variable.type" />
            </div>
        </toggle-display>
    </global-variable-layout>
</template>
<script>
    import GlobalVariable from '@components/global-variable/edit';
    import GlobalVariableLayout from '@components/global-variable/layout';
    import ToggleDisplay from '@components/global-variable/toggle-display';

    export default {
        name: '',
        components: {
            ToggleDisplay,
            GlobalVariableLayout,
            GlobalVariable,
        },
        props: {
            variableList: Array,
            selectedList: {
                type: Array,
                default: () => [],
            },
        },
        created () {
            const selectedNameMap = this.selectedList.reduce((result, name) => {
                result[name] = true;
                return result;
            }, {});
            // 执行方案中的步骤使用了得变量
            this.usedList = [];
            // 未被使用的变量
            this.unusedList = [];
            this.variableList.forEach((variable) => {
                if (selectedNameMap[variable.name]) {
                    this.usedList.push(variable);
                } else {
                    this.unusedList.push(variable);
                }
            });
        },
        methods: {
            submit () {
                const validateQueue = [];
                if (this.$refs.used) {
                    this.$refs.used.forEach((instance) => {
                        validateQueue.push(instance.validate());
                    });
                }
                if (this.$refs.unused) {
                    this.$refs.unused.forEach((instance) => {
                        validateQueue.push(instance.validate());
                    });
                }
                return Promise.all(validateQueue)
                    .then((editVariableList) => {
                        // 全局变量的最新值
                        const editVariableMap = editVariableList.reduce((result, variable) => {
                            const { value, targetValue } = variable;
                            result[variable.id] = {
                                defaultValue: value,
                                defaultTargetValue: targetValue,
                            };
                            return result;
                        }, {});
                        // 更新执行方案中全局变量的值
                        const variableList = this.variableList.reduce((result, variable) => {
                            if (editVariableMap[variable.id]) {
                                result.push(Object.assign({}, variable, editVariableMap[variable.id]));
                            }
                            return result;
                        }, []);
                        this.$emit('on-change', variableList);
                    });
            },
        },
    };
</script>
