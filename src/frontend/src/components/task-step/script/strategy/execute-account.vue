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
    <jb-form-item
        :label="$t('执行账号')"
        :property="field"
        required
        :rules="rules">
        <account-select
            class="form-item-content"
            :type="accountType"
            :value="formData[field]"
            @change="handleChange" />
    </jb-form-item>
</template>
<script>
    import {
        formatScriptTypeValue,
    } from '@utils/assist';

    import AccountSelect from '@components/account-select';

    import I18n from '@/i18n';

    export default {
        components: {
            AccountSelect,
        },
        props: {
            field: {
                type: String,
                required: true,
            },
            scriptLanguageField: {
                type: String,
            },
            formData: {
                type: Object,
                required: true,
            },
        },
        computed: {
            accountType () {
                if (formatScriptTypeValue(this.formData[this.scriptLanguageField]) === 'SQL') {
                    return 'db';
                }
                return 'system';
            },
        },
        created () {
            this.rules = [
                {
                    required: true,
                    message: I18n.t('执行账号必填'),
                    trigger: 'blur',
                },
            ];
        },
        methods: {
            handleChange (value) {
                this.$emit('on-change', this.field, value);
            },
        },
    };
</script>
