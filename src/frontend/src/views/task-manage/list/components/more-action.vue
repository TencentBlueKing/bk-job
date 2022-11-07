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
    <bk-popover
        :ref="id"
        :placement="placement"
        :theme="theme"
        :trigger="trigger">
        <div :class="['job-more-action', triggerCls]">
            <Icon
                class="icon"
                :type="icon" />
        </div>
        <div
            slot="content"
            :class="['job-action-wrapper', actionCls]">
            <slot />
        </div>
    </bk-popover>
</template>
<script>
    import _ from 'lodash';

    export default {
        name: '',
        props: {
            displayKey: {
                type: String,
                default: 'name',
            },
            data: {
                type: Object,
                default () {
                    return {};
                },
            },
            icon: {
                type: String,
                default: 'more',
            },
            placement: {
                type: String,
                default: 'bottom',
            },
            theme: {
                type: String,
                default: 'light',
                validator (value) {
                    if (['light', 'dark'].indexOf(value) < 0) {
                        console.error(`theme property is not valid: '${value}'`);
                        return false;
                    }
                    return true;
                },
            },
            trigger: {
                type: String,
                default: 'click',
            },
            triggerCls: {
                type: String,
                default: '',
            },
            actionCls: {
                type: String,
                default: '',
            },
        },
        created () {
            this.id = `${_.random(1, 1000)}_${Date.now()}_PopoverRef`;
        },
        methods: {
            handleMoreAction (actionId, payload) {
                if (this.trigger === 'click') {
                    this.$refs[`${this.id}PopoverRef`].instance.hide();
                }
            },
        },
    };
</script>
<style lang='postcss'>
    .job-more-action {
        display: inline-block;
        width: 32px;
        height: 32px;
        margin-left: -14px;
        line-height: 30px;
        color: #868b97;
        text-align: center;
        cursor: pointer;
        background: transparent;
        border-radius: 50%;

        .icon {
            font-size: 14px;
        }

        &:hover {
            background: #dcdee5;

            .icon {
                color: #3a84ff;
            }
        }
    }

    .job-action-wrapper {
        .job-action-item {
            width: 35px;
            font-size: 14px;
            line-height: 32px;
            color: #868b97;
            text-align: center;
            cursor: pointer;

            &:hover {
                color: #3a84ff;
            }
        }
    }
</style>
