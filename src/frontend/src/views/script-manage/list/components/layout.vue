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
        ref="handler"
        class="script-manage-list-layout">
        <div
            v-if="$slots.tag"
            :class="{
                'layout-left': true,
                expended: isOpen,
            }">
            <scroll-faker :style="styles">
                <slot name="tag" />
            </scroll-faker>
        </div>
        <div
            class="layout-right"
            :class="{
                'no-tag': !$slots.tag,
            }">
            <div
                v-if="$slots.tag"
                class="toggle-button"
                @click="handleToggle">
                <Icon
                    class="toggle-arrow"
                    :class="{ open: isOpen }"
                    type="down-small" />
            </div>
            <slot />
        </div>
    </div>
</template>
<script>
    import { getOffset } from '@utils/assist';
    const TASK_TAG_PANEL_TOGGLE = 'script_list_tag_panel_toggle';

    export default {
        data () {
            let isOpen = localStorage.getItem(TASK_TAG_PANEL_TOGGLE);
            if (!isOpen) {
                isOpen = true;
            } else {
                isOpen = JSON.parse(isOpen);
            }
            
            return {
                isOpen,
                styles: {},
            };
        },
        mounted () {
            const {
                top,
            } = getOffset(this.$refs.handler);
            this.styles = {
                height: `calc(100vh  - ${top}px)`,
            };
        },
        methods: {
            handleToggle () {
                this.isOpen = !this.isOpen;
                localStorage.setItem(TASK_TAG_PANEL_TOGGLE, JSON.stringify(this.isOpen));
            },
        },
    };
</script>
<style lang='postcss'>
    @import "@/css/mixins/media";

    .script-manage-list-layout {
        display: flex;

        .layout-left {
            position: relative;
            width: 0;
            background: #fafbfd;
            border-right: 1px solid #ecedf3;
            transition: width 0.2s linear;

            &.expended {
                @media (--small-viewports) {
                    width: 200px;
                }

                @media (--medium-viewports) {
                    width: 240px;
                }

                @media (--large-viewports) {
                    width: 280px;
                }

                @media (--huge-viewports) {
                    width: 280px;
                }
            }
        }

        .layout-right {
            position: relative;
            width: calc(100% - 280px);
            padding: 24px;
            background: #fff;
            box-sizing: border-box;
            flex: 1 0 auto;

            &.no-tag {
                width: 100%;
                padding: 0;
                background: unset;
            }

            .toggle-button {
                position: absolute;
                top: 50%;
                left: -1px;
                display: flex;
                width: 14px;
                height: 64px;
                font-size: 24px;
                color: #fff;
                cursor: pointer;
                background: #dcdee5;
                border-top-right-radius: 6px;
                border-bottom-right-radius: 6px;
                transform: translateY(-50%);
                align-items: center;
                justify-content: center;

                &:hover {
                    background: #c4c6cc;
                }

                .toggle-arrow {
                    transform: rotateZ(-90deg);

                    &.open {
                        transform: rotateZ(90deg);
                    }
                }
            }
        }
    }
</style>
