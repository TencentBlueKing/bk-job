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
    <div style="padding: 24px 30px;">
        <content-loader
            :height="684"
            :width="maxWidth"
            v-bind="$attrs">
            <rect
                height="575"
                rx="2"
                :width="blockWidth"
                x="298"
                y="109" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="109" />
            <rect
                height="20"
                rx="2"
                width="82"
                x="78"
                y="5" />
            <rect
                height="20"
                rx="2"
                width="82"
                x="78"
                y="63" />
            <rect
                height="20"
                rx="2"
                width="113"
                :x="xPosition1"
                y="63" />
            <rect
                height="20"
                rx="2"
                width="507"
                :x="xPosition2"
                y="63" />
            <rect
                height="20"
                rx="2"
                width="82"
                x="198"
                y="5" />
            <rect
                height="20"
                rx="2"
                width="82"
                x="318"
                y="5" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="151" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="192" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="233" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="274" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="314" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="355" />
            <rect
                height="20"
                rx="2"
                width="200"
                x="78"
                y="397" />
            <circle
                cx="17"
                cy="17"
                r="17" />
            <circle
                cx="17"
                cy="78"
                r="17" />
            <circle
                cx="17"
                cy="139"
                r="17" />
            <circle
                cx="17"
                cy="200"
                r="17" />
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
            xPosition1 () {
                return this.maxWidth - 113;
            },
            xPosition2 () {
                return this.maxWidth - 630;
            },
            blockWidth () {
                return Math.abs(this.maxWidth - 298);
            },
        },
    };
</script>
