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
    <div>
        <content-loader
            :height="476"
            :width="maxWidth"
            v-bind="$attrs">
            <rect x="0" y="0" :width="maxWidth" height="96" rx="2" />
            <rect x="0" y="108" width="88" height="32" rx="2" />
            <rect :x="position" y="108" width="480" height="32" rx="2" />
            <rect x="0" y="160" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="202" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="244" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="286" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="328" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="370" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="412" :width="maxWidth" height="22" rx="2" />
            <rect x="0" y="454" :width="maxWidth" height="22" rx="2" />
        </content-loader>
    </div>
</template>
<script>
    import {
        ContentLoader,
    } from 'vue-content-loader';
    export default {
        components: {
            ContentLoader,
        },
        props: {
            maxWidth: {
                type: Number,
                required: true,
            },
        },
        computed: {
            position () {
                return Math.abs(this.maxWidth - 480);
            },
        },
    };
</script>
