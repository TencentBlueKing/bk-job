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
    <div class="list-operation-extend">
        <Icon
            class="icon"
            type="more" />
        <div
            ref="content"
            class="list-operation-extend-wrapper"
            @click="handleClick">
            <slot />
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    const instanceMap = {};

    export default {
        created () {
            this.id = `${_.random(1, 1000)}_${Date.now()}_PopoverRef`;
            this.timer = '';
        },
        mounted () {
            this.init();
        },
        methods: {
            init () {
                instanceMap[this.id] = this.$bkPopover(this.$el, {
                    theme: 'list-operation-extend-popover light',
                    interactive: true,
                    placement: 'bottom',
                    content: this.$refs.content,
                    trigger: 'click',
                    arrow: true,
                    size: 'small',
                    onShow: () => {
                        Object.keys(instanceMap).forEach((key) => {
                            if (key !== this.id) {
                                instanceMap[key].hide();
                            }
                        });
                    },
                });
            },
            handleClick () {
                instanceMap[this.id].hide();
            },
        },
    };
</script>
<style lang='postcss'>
    .list-operation-extend {
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

    .list-operation-extend-wrapper {
        display: flex;
        flex-direction: column;

        .action-item {
            font-size: 14px;
            line-height: 32px;
            color: #868b97;
            text-align: center;
            cursor: pointer;

            &:hover {
                color: #3a84ff;
            }

            &.disabled {
                color: #dcdee5;
                cursor: not-allowed;
            }
        }
    }
</style>
