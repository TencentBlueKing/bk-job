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
        v-bkloading="{ isLoading: loading }"
        class="sync-plan-layout">
        <div
            class="wraper"
            :class="{ loading }">
            <Empty
                v-if="empty"
                class="layout-empty">
                <div class="empty-text">
                    {{ $t('template.暂无关联的定时任务') }}
                </div>
                <div>
                    <slot name="empty" />
                </div>
            </Empty>
            <template v-else>
                <slot />
            </template>
            <div class="layout-footer">
                <slot name="footer" />
            </div>
        </div>
    </div>
</template>
<script>
    import Empty from '@components/empty';

    export default {
        name: '',
        components: {
            Empty,
        },
        props: {
            loading: {
                type: Boolean,
                default: false,
            },
            empty: {
                type: Boolean,
                default: false,
            },
        },
    };
</script>
<style lang='postcss'>
    .sync-plan-layout {
        height: calc(100vh - 158px);

        .wraper {
            opacity: 100%;
            visibility: visible;
            transition: all 1s;

            &.loading {
                opacity: 0%;
                visibility: hidden;
            }
        }

        .layout-empty {
            display: flex;
            flex-direction: column;
            align-items: center;
            height: 100%;
            font-size: 20px;
            line-height: 1;
            color: #63656e;
            background: #fff;

            .empty-flag {
                width: 200px;
            }

            .empty-text {
                padding-top: 18px;
                padding-bottom: 10px;
            }
        }

        .layout-footer {
            position: fixed;
            bottom: 0;
            left: 0;
            display: flex;
            width: 100%;
            height: 52px;
            padding-right: 24px;
            background: #fff;
            border-top: 1px solid #e2e2e2;
            align-items: center;
            justify-content: flex-end;

            .bk-button {
                margin-left: 10px;
            }
        }
    }
</style>
