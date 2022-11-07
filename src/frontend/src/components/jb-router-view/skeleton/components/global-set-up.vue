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
            :height="439"
            :width="maxWidth"
            v-bind="$attrs">
            <rect
                height="40"
                rx="2"
                width="100"
                x="0"
                y="0" />
            <rect
                height="40"
                rx="2"
                width="100"
                x="110"
                y="0" />
            <rect
                height="40"
                rx="2"
                width="100"
                x="220"
                y="0" />
            <rect
                height="40"
                rx="2"
                width="100"
                x="330"
                y="0" />
            <rect
                height="40"
                rx="2"
                width="100"
                x="440"
                y="0" />
            <rect
                height="32"
                rx="2"
                width="68"
                x="558"
                y="589" />
            <rect
                height="20"
                rx="2"
                width="152"
                :x="xPosition"
                y="78" />
            <rect
                height="20"
                rx="2"
                width="152"
                :x="xPosition"
                y="498" />
            <rect
                height="32"
                rx="2"
                width="120"
                :x="xPosition"
                y="589" />
            <rect
                height="60"
                rx="2"
                width="960"
                :x="xPosition"
                y="130" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="222" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="261" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="304" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="347" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="388" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="431" />
            <rect
                height="20"
                rx="2"
                width="960"
                :x="xPosition"
                y="537" />
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
            xPosition () {
                return (this.maxWidth - 960) / 2;
            },
        },
    };
</script>
