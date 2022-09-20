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
        class="source-file-action-extend"
        @click.stop=""
        @mouseleave="handleHide">
        <Icon type="more" />
        <div
            ref="popoverContent"
            class="source-file-action-content"
            @click="handleWraperClick"
            @mouseleave="handleClose"
            @mouseover="handleShow">
            <slot />
        </div>
    </div>
</template>
<script>
    const instanceMap = {};

    export default {
        name: 'SourceFileExtendAction',
        created () {
            this.id = `action_extend_${Math.random()}_${Date.now()}`;
        },
        mounted () {
            this.init();
        },
        beforeDestroy () {
            instanceMap[this.id].hide();
            delete instanceMap[this.id];
        },
        methods: {
            init () {
                instanceMap[this.id] = this.$bkPopover(this.$el, {
                    theme: 'source-file-action-extend-popover',
                    interactive: true,
                    placement: 'bottom',
                    content: this.$refs.popoverContent,
                    trigger: 'mouseover',
                    arrow: true,
                    onShow: () => {
                        Object.keys(instanceMap).forEach((key) => {
                            if (key !== this.id) {
                                instanceMap[key].hide();
                            }
                        });
                    },
                });
            },
            handleWraperClick () {
                this.handleClose();
            },
            handleHide () {
                this.leaveTimer = setTimeout(() => {
                    this.handleClose();
                }, 3000);
            },
            handleShow () {
                clearTimeout(this.leaveTimer);
            },
            handleClose () {
                instanceMap[this.id] && instanceMap[this.id].hide();
            },
        },
    };
</script>
<style lang="postcss">
    html[lang="en-US"] {
        .source-file-action-content {
            width: 154px;
        }
    }

    .source-file-action-extend {
        position: absolute;
        top: 50%;
        right: 16px;
        z-index: 9;
        display: flex;
        width: 30px;
        height: 30px;
        font-size: 16px;
        font-weight: normal;
        color: #979ba5;
        cursor: pointer;
        border-radius: 50%;
        transform: translateY(-50%);
        user-select: none;
        align-items: center;
        justify-content: center;

        &:hover,
        &.tippy-active {
            z-index: 10;
            color: #3a84ff;
            background: #dcdee5;
        }
    }

    .source-file-action-extend-popover-theme {
        padding: 0 !important;

        .tippy-arrow {
            display: none;
        }

        .source-file-action-content {
            width: 93px;
            font-size: 14px;
            line-height: 32px;
            color: #63656e;
            background: #fff;
            border: 1px solid #f0f1f5;
            box-shadow: 0 2px 1px 0 rgb(185 203 222 / 50%);

            .action-item {
                padding-left: 15px;
                cursor: pointer;

                &:hover {
                    color: #3a84ff;
                    background: #e5efff;
                }
            }
        }
    }

</style>
