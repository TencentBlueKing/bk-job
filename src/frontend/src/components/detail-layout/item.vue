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
        class="detail-item"
        :class="`design-layout-${layout}`">
        <div class="detail-label">
            <span>{{ label }}</span>
        </div>
        <div class="detail-content">
            <slot />
        </div>
    </div>
</template>
<script>
    export default {
        name: '',
        props: {
            label: {
                type: String,
                default: '',
            },
            layout: {
                type: String,
                default: '', // horizontal, vertical
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .detail-item {
        display: flex;
        font-size: 14px;
        line-height: 32px;
        color: #313238;
        align-items: flex-start;
        justify-content: flex-end;

        &.design-layout-horizontal {
            flex-direction: row !important;
        }

        &.design-layout-vertical {
            flex-direction: column !important;

            .detail-content {
                width: 100%;
            }
        }
    }

    .detail-label {
        display: flex;
        padding-right: 2px;
        color: #63656e;
        text-align: right;
        word-break: keep-all;
        white-space: pre;
        align-items: center;
        justify-content: flex-end;
        flex: 0 0 auto;
    }

    .detail-content {
        flex: 1;
        color: #63656e;
        word-break: break-all;
    }
</style>
