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
    <div ref="layout" class="plan-action-layout" :style="layoutStyles">
        <div v-if="title" class="layout-title">
            <div class="title-text">{{ title }}</div>
            <slot name="sub-title" />
        </div>
        <div ref="content" class="content-wraper" :style="contentStyles">
            <slot />
        </div>
        <div class="layout-footer" :style="footerStyles">
            <slot name="footer" />
        </div>
    </div>
</template>
<script>
    import {
        getOffset,
    } from '@utils/assist';

    export default {
        name: '',
        props: {
            title: String,
            offsetTarget: String,
            offsetBottom: {
                type: Number,
                default: 20,
            },
        },
        data () {
            return {
                showContent: !this.loading,
                layoutOffsetTop: 0,
                offsetLeft: 0,
                contentOffsetTop: 0,
                isFooterFixed: false,
            };
        },
        computed: {
            layoutStyles () {
                return {
                    height: `calc(100vh - ${this.layoutOffsetTop}px - ${this.offsetBottom}px)`,
                };
            },
            contentStyles () {
                return {
                    height: `calc(100vh - ${this.contentOffsetTop}px - 60px - ${this.offsetBottom}px)`,
                };
            },
            footerStyles () {
                return {
                    'padding-left': `${this.offsetLeft}px`,
                    'box-shadow': '0px -2px 4px 0px rgba(0, 0, 0, 0.06)',
                };
            },
        },
        mounted () {
            const observer = new MutationObserver((payload) => {
                this.init();
            });
            observer.observe(document.querySelector('body'), {
                subtree: true,
                childList: true,
                attributeName: true,
                characterData: true,
            });
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                observer.takeRecords();
                observer.disconnect();
                window.removeEventListener('resize', this.init);
            });
            this.init();
        },
        methods: {
            init () {
                if (this.offsetTarget) {
                    const $offsetTarget = document.querySelector(`.${this.offsetTarget}`);
                    if ($offsetTarget) {
                        const layoutLeft = this.$refs.layout.getBoundingClientRect().left;
                        const targetLeft = $offsetTarget.getBoundingClientRect().left;
                        this.offsetLeft = targetLeft - layoutLeft;
                    }
                }
                const offset = getOffset(this.$refs.layout);
                this.layoutOffsetTop = offset.top;
                this.contentOffsetTop = getOffset(this.$refs.content).top;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import '@/css/mixins/scroll';

    .plan-action-layout {
        position: relative;
        height: 100vh;
        padding-left: 40px;
        overflow: hidden;
        background: #fff;

        .layout-title {
            padding-top: 30px;
            padding-right: 40px;
            padding-bottom: 16px;
            color: #000;
            border-bottom: 1px solid #f0f1f5;

            .title-text {
                padding-bottom: 14px;
                font-size: 18px;
                line-height: 1;
            }
        }

        .content-wraper {
            padding-right: 40px;
            padding-bottom: 20px;
            margin-top: 30px;
            margin-right: 2px;
            overflow-y: auto;

            @mixin scroller;
        }

        .layout-footer {
            position: relative;
            display: flex;
            height: 60px;
            padding-right: 40px;
            padding-left: 126px;
            margin-left: -40px;
            background: #fff;
            align-items: center;
        }
    }
</style>
