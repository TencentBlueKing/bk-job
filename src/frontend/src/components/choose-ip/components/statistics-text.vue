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
    <span class="statistics-text">
        <template v-if="data.success > 0">
            {{ $t('正常') }}:<span class="success number">{{ data.success }}</span>
        </template>
        <template v-if="data.fail > 0">
            <span
                v-if="data.success > 0"
                style="padding-left: 16px;" />
            {{ $t('异常') }}:<span class="error number">{{ data.fail }}</span>
        </template>
    </span>
</template>
<script>
    export default {
        props: {
            data: {
                type: Object,
                require: true,
            },
        },
    };
</script>
<style lang='postcss'>
    .statistics-text {
        display: flex;
        font-size: 12px;
        font-weight: normal;
        color: #63656e;

        .strong {
            color: #3a84ff;
        }

        .error {
            color: #ea3636;
        }

        .success {
            color: #3fc06d;
        }

        .number {
            padding: 0 4px;
            font-weight: bold;
        }
    }
</style>
