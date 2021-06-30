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
    <jb-form :model="formData" :rules="rules" ref="varStringForm">
        <jb-form-item :label="$t('template.变量名称')" required property="name">
            <jb-input
                v-model="formData.name"
                :placeholder="$t('template.变量名仅支持大小写英文字母或下划线 [必填]')"
                :maxlength="30" />
        </jb-form-item>
        <jb-form-item
            ref="defaultValue"
            :label="$t('template.初始值')"
            :desc="$t('template.仅作用于创建执行方案时的初始变量值，后续更改不会同步到执行方案')"
            property="defaultValue">
            <bk-input
                class="var-default-value"
                :placeholder="$t('template.请输入变量的初始值 [可选]')"
                v-model="formData.defaultValue" />
        </jb-form-item>
        <jb-form-item :label="$t('template.变量描述')">
            <bk-input
                v-model="formData.description"
                type="textarea"
                :placeholder="$t('template.这里可以备注变量的用途、使用说明等信息 [可选]')"
                maxlength="100" />
        </jb-form-item>
        <jb-form-item ext-cls="changeable-cls">
            <bk-checkbox
                v-model="formData.changeable"
                :true-value="1"
                :false-value="0"
                v-bk-tooltips.right="$t('template.变量的值在执行中可变')">
                {{ $t('template.赋值可变') }}
            </bk-checkbox>
        </jb-form-item>
        <jb-form-item>
            <bk-checkbox
                v-model="formData.required"
                :true-value="1"
                :false-value="0">
                {{ $t('template.执行时必填') }}
            </bk-checkbox>
        </jb-form-item>
    </jb-form>
</template>
<script>
    import I18n from '@/i18n';
    import {
        globalVariableNameRule,
    } from '@utils/validator';
    import JbInput from '@components/jb-input';

    const getDefaultData = () => ({
        id: 0,
        delete: 0,
        // 变量名
        name: '',
        // 初始值
        defaultValue: '',
        // 变量描述
        description: '',
        // 赋值可变 0-不可变 1-可变
        changeable: 0,
        // 必填 0-非必填 1-必填
        required: 0,
    });

    export default {
        name: 'VarString',
        components: {
            JbInput,
        },
        props: {
            variable: {
                type: Array,
                default () {
                    return [];
                },
            },
            data: {
                type: Object,
                default () {
                    return {};
                },
            },
        },
        data () {
            return {
                formData: getDefaultData(),
            };
        },
        watch: {
            data: {
                handler (data) {
                    if (data.name) {
                        const { name, defaultValue, description, changeable, required, id } = data;
                        this.formData = {
                            id,
                            name,
                            defaultValue,
                            description,
                            changeable,
                            required,
                            delete: data.delete,
                        };
                    }
                },
                immediate: true,
            },
            
        },
        created () {
            this.rules = {
                name: [
                    {
                        required: true,
                        message: I18n.t('template.变量名称必填'),
                        trigger: 'blur',
                    },
                    {
                        validator: globalVariableNameRule.validator,
                        message: globalVariableNameRule.message,
                        trigger: 'blur',
                    },
                    {
                        validator: val => !this.variable.some(item => item.name === val),
                        message: I18n.t('template.变量名称已存在，请重新输入'),
                        trigger: 'blur',
                    },
                ],
            };
        },
        methods: {
            submit () {
                return this.$refs.varStringForm.validate()
                    .then((validator) => {
                        this.$emit('on-change', {
                            ...this.formData,
                            type: 1,
                        });
                    }, validator => Promise.reject(validator.content));
            },
        },
    };
</script>
<style lang='postcss'>
    .changeable-cls {
        .bk-form-checkbox .bk-checkbox-text {
            padding-bottom: 2px;
            border-bottom: 1px dashed #c4c6cc;
        }
    }
</style>
