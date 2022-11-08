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
    <div style="padding: 20px 24px;">
        <content-loader
            :height="439"
            :width="maxWidth"
            v-bind="$attrs">
            <rect
                height="20"
                rx="2"
                width="60"
                x="0"
                y="135" />
            <rect
                height="20"
                rx="2"
                width="60"
                x="0"
                y="72" />
            <rect
                height="20"
                rx="2"
                width="60"
                x="0"
                y="36" />
            <rect
                height="20"
                rx="2"
                width="60"
                x="0"
                y="0" />
            <rect
                height="20"
                rx="2"
                width="330"
                x="80"
                y="0" />
            <rect
                height="20"
                rx="2"
                width="330"
                x="80"
                y="36" />
            <rect
                height="20"
                rx="2"
                width="330"
                x="80"
                y="72" />
            <rect
                height="20"
                rx="2"
                width="60"
                x="0"
                y="208" />
            <rect
                height="20"
                rx="2"
                width="120"
                x="80"
                y="419" />
            <rect
                height="20"
                rx="2"
                width="68"
                x="210"
                y="419" />
            <rect
                height="20"
                rx="2"
                width="68"
                x="288"
                y="419" />
            <rect
                height="20"
                rx="2"
                width="68"
                x="512"
                y="419" />
            <rect
                height="50"
                rx="2"
                width="160"
                x="80"
                y="119" />
            <rect
                height="42"
                rx="2"
                width="500"
                x="80"
                y="199" />
            <rect
                height="42"
                rx="2"
                width="500"
                x="80"
                y="251" />
            <rect
                height="42"
                rx="2"
                width="500"
                x="80"
                y="303" />
            <rect
                height="42"
                rx="2"
                width="500"
                x="80"
                y="355" />
            <rect
                height="50"
                rx="2"
                width="160"
                x="250"
                y="119" />
            <rect
                height="50"
                rx="2"
                width="160"
                x="420"
                y="119" />
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
    };
</script>
