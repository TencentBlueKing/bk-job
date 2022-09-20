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
    <comoponent
        :is="com"
        class="global-variable-batch-edit-render"
        :data="data"
        v-bind="$attrs"
        v-on="$listeners" />
</template>
<script>
    import GlobalVariableModel from '@model/task/global-variable';

    import HostType from './host';
    import PasswordType from './password';
    import TextType from './text';

    export default {
        name: '',
        props: {
            data: {
                type: Object,
                required: true,
            },
        },
        data () {
            return {};
        },
        computed: {
            com () {
                const comMap = {
                    [GlobalVariableModel.TYPE_STRING]: TextType,
                    [GlobalVariableModel.TYPE_NAMESPACE]: TextType,
                    [GlobalVariableModel.TYPE_PASSWORD]: PasswordType,
                    [GlobalVariableModel.TYPE_RELATE_ARRAY]: TextType,
                    [GlobalVariableModel.TYPE_INDEX_ARRAY]: TextType,
                    [GlobalVariableModel.TYPE_HOST]: HostType,
                };
                return comMap[this.data.type];
            },
        },
    };
</script>
<style lang='postcss'>
    .global-variable-batch-edit-render {
        margin-bottom: 20px;

        &:last-child {
            margin-bottom: 0;
        }

        &:hover {
            .remove-flag {
                display: inline;
            }
        }

        .name {
            position: relative;
            margin-bottom: 10px;
            font-size: 14px;
            line-height: 19px;
            color: #63656e;
        }

        .remove-flag {
            position: absolute;
            top: 0;
            left: -5px;
            display: none;
            font-size: 14px;
            color: #c4c6cc;
            cursor: pointer;
            transform: translateX(-100%);

            &:hover {
                color: #ea3636;
            }
        }
    }
</style>
