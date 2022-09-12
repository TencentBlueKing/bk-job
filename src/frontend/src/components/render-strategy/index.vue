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
    <div
        class="strategy-item"
        :style="{ width: `${width}px` }">
        <div
            class="triangle"
            :style="{ left: `${left}px` }" />
        <div>
            <slot />
        </div>
    </div>
</template>
<script>
    export default {
        props: {
            width: String,
            left: String,
        },
    };
</script>
<style lang="postcss">
    .strategy-item {
        position: relative;
        padding: 16px 20px;
        margin-top: 10px;
        background: #f0f1f5;

        .triangle {
            position: absolute;
            top: -7px;
            left: 84px;
            z-index: 1;
            width: 0;
            height: 0;
            border-right: 7px solid transparent;
            border-bottom: 7px solid #f0f1f5;
            border-left: 7px solid transparent;

            span {
                position: absolute;
                top: 2px;
                left: -9px;
                display: block;
                width: 0;
                height: 0;
                border-right: 6px solid transparent;
                border-bottom: 6px solid #fafbfd;
                border-left: 6px solid transparent;
            }
        }
    }
</style>
