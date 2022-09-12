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
    <div class="dashboard-card-layout">
        <div class="card-title">
            <span
                class="card-title-text"
                :class="{
                    'hover-tips': needRenderTips,
                }"
                :tippy-tips="titleTips">
                {{ title }}
            </span>
        </div>
        <div class="card-content">
            <slot />
        </div>
        <div class="card-extend">
            <slot name="extend" />
        </div>
    </div>
</template>
<script>
    import I18n from '@/i18n';

    export default {
        name: '',
        props: {
            title: {
                type: String,
                default: I18n.t('dashboard.接入业务量'),
            },
            titleTips: {
                type: String,
            },
        },
        data () {
            return {};
        },
        computed: {
            needRenderTips () {
                return !!this.titleTips;
            },
        },
        methods: {},
    };
</script>
<style lang='postcss' scoped>
    .dashboard-card-layout {
        position: relative;
        display: flex;
        flex-direction: column;
        padding: 16px 20px 20px;
        background: #fff;

        .card-title {
            margin-bottom: 16px;
            font-size: 13px;
            line-height: 18px;
            color: #63656e;
        }

        .card-title-text {
            position: relative;

            &.hover-tips {
                cursor: pointer;

                &::after {
                    position: absolute;
                    bottom: 0;
                    left: 0;
                    width: 100%;
                    margin-bottom: -3px;
                    border-bottom: 1px dashed #dcdee5;
                    content: "";
                }
            }
        }

        .card-extend {
            position: absolute;
            top: 18px;
            right: 20px;
            display: flex;
            color: #c4c6cc;

            & > * {
                cursor: pointer;
            }

            .job-icon {
                &:hover {
                    color: #3a84ff;
                }

                & ~ .job-icon {
                    margin-left: 10px;
                }
            }
        }
    }
</style>
