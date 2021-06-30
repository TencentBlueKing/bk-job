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
    <div ref="smartActionWraper">
        <div>
            <slot />
        </div>
        <div
            ref="action"
            :style="actionPlaceholderStyles"
            role="action">
            <div v-show="isActionRender" :class="dymaicClasses" :style="dymaicStyles" role="dynamic">
                <div :style="actionContentstyles">
                    <slot name="action" />
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import _ from 'lodash';

    export default {
        props: {
            offsetTarget: {
                type: String,
            },
            fill: {
                type: Number,
                default: 0,
            },
        },
        data () {
            return {
                isActionRender: false,
                isFixed: false,
                paddingLeft: 0,
                offsetLeft: 0,
            };
        },
        computed: {
            /**
             * @desc 操作按钮站位区域的样式
             * @returns {Object}
            */
            actionPlaceholderStyles () {
                return {
                    height: this.isFixed ? '50px' : 'auto',
                };
            },
            /**
             * @desc 动态定位块的 class
             * @returns {String}
            */
            dymaicClasses () {
                return this.isFixed ? 'smart' : '';
            },
            /**
             * @desc 动态定位块的 style
             * @returns {Object}
            */
            dymaicStyles () {
                return {
                    'padding-left': this.isFixed ? `${this.paddingLeft}px` : '0',
                };
            },
            /**
             * @desc 内容区的 style
             * @returns {Object}
            */
            actionContentstyles () {
                return {
                    'padding-left': `${this.offsetLeft + this.fill}px`,
                };
            },
        },
        mounted () {
            window.addEventListener('resize', this.smartPosition);
            const observer = new MutationObserver(() => {
                this.calcOffsetLeft();
                this.smartPosition();
            });
            observer.observe(document.querySelector('body'), {
                subtree: true,
                childList: true,
                attributeName: true,
                characterData: true,
            });
            this.$once('hook:beforeDestroy', () => {
                observer.takeRecords();
                observer.disconnect();
                window.removeEventListener('resize', this.smartPosition);
            });
            this.calcOffsetLeft();
            this.smartPosition();
        },
        methods: {
            /**
             * @desc action 内容区相对 offsetTarget 的偏移位置
            */
            calcOffsetLeft: _.debounce(function () {
                if (!this.offsetTarget || !this.$refs.action) {
                    return;
                }
                const $target = document.querySelector(`.${this.offsetTarget}`);
                if (!$target) {
                    return;
                }
                const actionPositionLeft = this.$refs.action.getBoundingClientRect().left;
                const offsetTargetLeft = $target.getBoundingClientRect().left;
                this.offsetLeft = offsetTargetLeft - actionPositionLeft;
            }, 50),
            /**
             * @desc 当 action 块是 fixed 效果时，修正左边位置的 paddingLeft
            */
            smartPosition: _.debounce(function () {
                if (!this.$refs.action) {
                    return;
                }
                const windowHeight = window.innerHeight;
                const { height, top, left } = this.$refs.action.getBoundingClientRect();
                this.isFixed = height + top + 20 > windowHeight;
                this.paddingLeft = left;
                setTimeout(() => {
                    this.isActionRender = true;
                });
            }, 300),
        },
    };
</script>
<style lang="postcss" scoped>
    .smart {
        position: fixed;
        right: 0;
        bottom: 0;
        left: 0;
        z-index: 998;
        display: flex;
        height: 52px;
        background: #fff;
        box-shadow: 0 -2px 4px 0 rgba(0, 0, 0, 0.06);
        align-items: center;
    }
</style>
