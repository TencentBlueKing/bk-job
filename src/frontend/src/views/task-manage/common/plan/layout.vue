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
        <div class="loading-wraper" :class="{ 'loading-hidden': showContent }">
            <div class="loading-content">
                <content-loader
                    :height="455"
                    :width="580"
                    :speed="2"
                    primary-color="#EBECF3"
                    secondary-color="#F6F7FB">
                    <rect x="0" y="6.60804744e-13" width="237" height="21.5454545" rx="2" />
                    <rect x="0" y="84.7727273" width="60" height="20" rx="2" />
                    <rect x="0" y="157.772727" width="60" height="20" rx="2" />
                    <rect x="80" y="434.772727" width="120" height="20" rx="2" />
                    <rect x="80" y="68.7727273" width="160" height="50" rx="2" />
                    <rect x="80" y="148.772727" width="500" height="42" rx="2" />
                    <rect x="80" y="200.772727" width="500" height="42" rx="2" />
                    <rect x="80" y="252.772727" width="500" height="42" rx="2" />
                    <rect x="80" y="304.772727" width="500" height="42" rx="2" />
                    <rect x="80" y="356.772727" width="500" height="42" rx="2" />
                    <rect x="250" y="68.7727273" width="160" height="50" rx="2" />
                    <rect x="420" y="68.7727273" width="160" height="50" rx="2" />
                </content-loader>
            </div>
        </div>
        <div class="layout-title">
            <div class="title-text">{{ title }}</div>
            <slot name="sub-title" />
        </div>
        <div ref="content" class="content-wraper" :style="contentStyles">
            <slot />
        </div>
        <div class="layout-footer" :style="footerStyles">
            <slot name="footer" />
        </div>
        <back-top v-if="showContent" :target="getBackTopTarget" />
    </div>
</template>
<script>
    import _ from 'lodash';
    import {
        ContentLoader,
    } from 'vue-content-loader';
    import {
        getOffset,
    } from '@utils/assist';
    import BackTop from '@components/back-top';

    export default {
        name: '',
        components: {
            ContentLoader,
            BackTop,
        },
        props: {
            title: String,
            mode: String,
            planName: String,
            loading: {
                type: Boolean,
                default: false,
            },
            bottomOffset: {
                type: Number,
                default: 0,
            },
        },
        data () {
            return {
                showContent: !this.loading,
                layoutOffsetTop: 0,
                contentOffsetTop: 0,
                footerOffsetLeft: 0,
                isFooterFixed: false,
            };
        },
        computed: {
            layoutStyles () {
                return {
                    height: `calc(100vh - ${this.layoutOffsetTop}px - ${this.bottomOffset}px)`,
                };
            },
            contentStyles () {
                return {
                    'max-height': `calc(100vh - ${this.contentOffsetTop}px - 60px - ${this.bottomOffset}px)`,
                };
            },
            footerStyles () {
                const styles = {
                    'padding-left': `${this.footerOffsetLeft}px`,
                };
                if (!this.isFooterFixed) {
                    return styles;
                }
                return {
                    ...styles,
                    'box-shadow': '0px -2px 4px 0px rgba(0, 0, 0, 0.06)',
                };
            },
        },
        watch: {
            loading (loading) {
                if (loading) {
                    this.showContent = false;
                    this.time = Date.now();
                    return;
                }
                
                const spaceTime = Date.now() - this.time;
                setTimeout(() => {
                    this.showContent = true;
                }, spaceTime > 800 ? 0 : 1000 - spaceTime);
            },
        },
        created () {
            this.time = Date.now();
        },
        mounted () {
            window.addEventListener('resize', this.init);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.init);
            });
            this.init();
        },
        updated: _.debounce(function () {
            this.init();
        }, 500),
        methods: {
            init () {
                if (!this.$refs.layout || !this.$refs.content || !document.querySelector('#templateStepRender')) {
                    return;
                }
                const offset = getOffset(this.$refs.layout);
                this.layoutOffsetTop = offset.top;
                this.contentOffsetTop = getOffset(this.$refs.content).top;
                setTimeout(() => {
                    const contentScrollHeight = this.$refs.content.scrollHeight;
                    const contentHeight = this.$refs.content.getBoundingClientRect().height;
                    this.isFooterFixed = contentScrollHeight > contentHeight;
                    const layoutOffsetLeft = this.$refs.layout.getBoundingClientRect().left;
                    const offsetTargetOffsetLeft = document.querySelector('#templateStepRender').getBoundingClientRect().left;
                    this.footerOffsetLeft = offsetTargetOffsetLeft - layoutOffsetLeft;
                });
            },
            getBackTopTarget () {
                return this.$refs.content;
            },
        },
    };
</script>
<style lang='postcss' scoped>
    @import '@/css/mixins/scroll';

    .plan-action-layout {
        position: relative;
        padding-left: 40px;
        overflow: hidden;
        background: #fff;

        .loading-wraper {
            position: absolute;
            top: 0;
            left: 0;
            z-index: 999;
            width: 100%;
            height: 100%;
            padding-top: 40px;
            padding-left: 40px;
            background: #fff;
            opacity: 1;
            visibility: visible;

            &.loading-hidden {
                opacity: 0;
                visibility: hidden;
                transition: visibility 0.7s linear, opacity 0.5s linear;
            }

            .loading-content {
                width: 580px;
            }
        }

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
            padding-top: 30px;
            padding-right: 40px;
            margin-right: 2px;
            overflow-y: auto;

            @mixin scroller;
        }

        .layout-footer {
            position: relative;
            display: flex;
            height: 60px;
            margin-right: -40px;
            margin-left: -40px;
            background: #fff;
            align-items: center;
        }
    }
</style>
