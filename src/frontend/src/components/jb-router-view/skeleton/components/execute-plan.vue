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
    <div style="padding: 20px;">
        <content-loader
            :height="716"
            :width="maxWidth"
            v-bind="$attrs">
            <rect x="0" y="0" width="274" height="20" rx="2" />
            <rect x="329" y="31.2272727" width="237" height="21.5454545" rx="2" />
            <rect x="329" y="116" width="60" height="20" rx="2" />
            <rect x="329" y="189" width="60" height="20" rx="2" />
            <rect x="409" y="466" width="120" height="20" rx="2" />
            <rect x="0" y="42" width="274" height="20" rx="2" />
            <rect x="0" y="83" width="274" height="20" rx="2" />
            <rect x="0" y="124" width="274" height="20" rx="2" />
            <rect x="409" y="100" width="160" height="50" rx="2" />
            <rect x="409" y="180" width="500" height="42" rx="2" />
            <rect x="409" y="232" width="500" height="42" rx="2" />
            <rect x="409" y="284" width="500" height="42" rx="2" />
            <rect x="409" y="336" width="500" height="42" rx="2" />
            <rect x="409" y="388" width="500" height="42" rx="2" />
            <rect x="579" y="100" width="160" height="50" rx="2" />
            <rect x="749" y="100" width="160" height="50" rx="2" />
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
