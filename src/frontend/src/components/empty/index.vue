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
    <div class="job-empty">
        <template v-if="type === 'search'">
            <img
                src="/static/images/search-empty.svg"
                :style="styles">
            <div class="job-empty-text">
                <slot>{{ $t('搜索结果为空') }}</slot>
            </div>
        </template>
        <template v-else>
            <img
                src="/static/images/empty.svg"
                :style="styles">
            <div class="job-empty-text">
                <slot>{{ title }}</slot>
                <slot name="desc" />
            </div>
        </template>
    </div>
</template>
<script>
    import I18n from '@/i18n';

    export default {
        name: 'Empty',
        props: {
            title: {
                type: String,
                default: I18n.t('数据为空'),
            },
            width: {
                type: Number,
                default: 120,
            },
            type: {
                type: String,
                default: 'empty',
            },
        },
        computed: {
            styles () {
                if (this.width > 0) {
                    return {
                        width: `${this.width}px`,
                    };
                }
                return {};
            },
        },
    };
</script>
<style lang='postcss'>
    .job-empty {
        display: flex;
        font-size: 12px;
        line-height: 1;
        color: #979ba5;
        text-align: center;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .job-empty-text {
            margin-top: 20px;

            * {
                font-size: 12px !important;
            }
        }
    }
</style>
