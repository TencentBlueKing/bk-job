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
    <content-loader
        :height="716"
        :width="maxWidth"
        v-bind="$attrs">
        <rect
            height="32"
            rx="2"
            width="960"
            x="0"
            y="22" />
        <rect
            height="32"
            rx="2"
            width="960"
            x="0"
            y="106" />
        <rect
            height="32"
            rx="2"
            width="960"
            x="0"
            y="190" />
        <rect
            height="32"
            rx="2"
            width="960"
            x="0"
            y="274" />
        <rect
            height="16"
            rx="1"
            width="105"
            x="0"
            y="0" />
        <rect
            height="16"
            rx="1"
            width="207"
            x="0"
            y="84" />
        <rect
            height="16"
            rx="1"
            width="145"
            x="0"
            y="168" />
        <rect
            height="16"
            rx="1"
            width="207"
            x="0"
            y="252" />
    </content-loader>
</template>
<script>
    import { ContentLoader } from 'vue-content-loader';
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
    };
</script>
