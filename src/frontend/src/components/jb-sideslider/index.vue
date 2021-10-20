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
    <bk-sideslider
        class="jb-sideslider"
        ref="bkSideslider"
        v-bind="$attrs"
        :is-show="isShow"
        @update:isShow="close"
        quick-close
        transfer
        :width="mediaWidth"
        :before-close="beforeClose">
        <template slot="header">
            <slot name="header">
                {{ title }}
            </slot>
        </template>
        <template v-if="isRender">
            <template slot="content">
                <div class="jb-sideslider-content">
                    <slot />
                </div>
            </template>
            <template slot="footer" v-if="showFooter">
                <div ref="footer" class="jb-sideslider-footer" :style="footerStyles">
                    <slot name="footer">
                        <bk-button
                            theme="primary"
                            class="mr10"
                            :loading="isSubmiting"
                            @click="handleSubmit">
                            {{ okText }}
                        </bk-button>
                        <bk-button @click="handleCancel">{{ cancelText }}</bk-button>
                    </slot>
                </div>
            </template>
        </template>
    </bk-sideslider>
</template>
<script>
    import _ from 'lodash';
    import I18n from '@/i18n';
    import {
        leaveConfirm,
    } from '@utils/assist';

    export default {
        name: 'JbSideslider',
        inheritAttrs: false,
        props: {
            isShow: {
                type: Boolean,
                required: true,
            },
            title: String,
            showFooter: {
                type: Boolean,
                default: true,
            },
            width: {
                type: Number,
            },
            media: {
                type: Array,
                default: () => [],
            },
            okText: {
                type: String,
                default: I18n.t('保存'),
            },
            cancelText: {
                type: String,
                default: I18n.t('取消'),
            },
            footerOffsetTarget: {
                type: String,
            },
        },
        data () {
            return {
                isRender: false,
                isSubmiting: false,
                isFooterFixed: false,
                offsetLeft: 0,
                mediaWidth: this.width,
            };
        },
        computed: {
            footerStyles () {
                const styles = {};
                if (this.offsetLeft > 0) {
                    styles['padding-left'] = `${this.offsetLeft}px`;
                }
                if (this.isFooterFixed) {
                    styles['box-shadow'] = '0px -2px 4px 0px rgba(0, 0, 0, 0.06)';
                }
                return styles;
            },
        },
        watch: {
            /**
             * @desc 处理bk-sideslider中有路由跳转在某些情况下不关闭的bug
             */
            '$route' () {
                this.$refs.bkSideslider.isShow = false;
            },
            isShow: {
                handler (val) {
                    // settimeout 解决 bk-sideslider 默认显示没有遮罩的 bug
                    setTimeout(() => {
                        const observer = new MutationObserver(() => {
                            this.checkFooterPosition();
                        });
                        this.$once('hook:beforeDestroy', () => {
                            observer.takeRecords();
                            observer.disconnect();
                        });
                        if (val) {
                            this.isRender = true;
                            // 当页面可以进行编辑时，其中一项是通过sideslider来编辑的，需要先记录页面的编辑状态
                            this.pageChangeAlertMemo = window.changeAlert;
                            window.changeAlert = 'dialog';
                            this.getMediaWidth();
                            this.$nextTick(() => {
                                observer.observe(this.$refs.bkSideslider.$el, {
                                    subtree: true,
                                    childList: true,
                                    attributeName: true,
                                    characterData: true,
                                });
                            });
                            this.checkFooterPosition();
                        } else {
                            if (observer) {
                                observer.takeRecords();
                                observer.disconnect();
                            }
                        }
                    });
                },
                immediate: true,
            },
        },
        created () {
            this.pageChangeAlertMemo = false;
        },
        beforeDestroy () {
            // 解决 bk-sideslider 在其内部有路由跳转时bk-sideslider的dom没有移出的bug
            const { $el } = this.$refs.bkSideslider;
            setTimeout(() => {
                if ($el && $el.parentNode) {
                    $el.parentNode.removeChild($el);
                }
            });
        },
        mounted () {
            window.addEventListener('resize', this.getMediaWidth);
            this.$once('hook:beforeDestroy', () => {
                window.removeEventListener('resize', this.getMediaWidth);
            });
        },
        methods: {
            /**
             * @desc 计算响应式宽度
             */
            getMediaWidth: _.throttle(function () {
                if (!this.media.length) {
                    return;
                }
                
                const queryRange = [
                    // 1366,
                    1680,
                    1920,
                    2560,
                ];
                const windowHeight = window.innerWidth;
                let index = 0;
                
                queryRange.forEach((mediaWidth) => {
                    if (mediaWidth < windowHeight) {
                        index = index + 1;
                    }
                });
                index = Math.min(index, this.media.length - 1);
                this.mediaWidth = this.media[index];
            }, 60),
            /**
             * @desc 计算footer的位置
             */
            checkFooterPosition: _.debounce(function () {
                this.$nextTick(() => {
                    if (!this.$refs.footer) {
                        return;
                    }
                    // 计算footer是否固定在底部
                    const { bottom } = this.$refs.footer.getBoundingClientRect();
                    const winHeight = window.innerHeight;
                    this.isFooterFixed = bottom + 5 >= winHeight;
                    // 计算button偏移量
                    const $target = document.querySelector(`.${this.footerOffsetTarget}`);
                    if ($target) {
                        const offsetTargetLeft = $target.getBoundingClientRect().left;
                        const footerLeft = this.$refs.footer.getBoundingClientRect().left;
                        this.offsetLeft = offsetTargetLeft - footerLeft;
                    } else {
                        this.offsetLeft = 0;
                    }
                });
            }, 100),
            /**
             * @desc 检测提供给dialog的交互组件目标
             *
             * 判断条件为有没有提供submit方法
             */
            checkHandle () {
                // 可以绑定子组件的唯一判断条件——子组件有提供submit methods
                const handle = {
                    submit: () => Promise.resolve(),
                    reset: () => Promise.resolve(),
                };
                const [{ $children }] = this.$children;
                $children.forEach((child) => {
                    if (typeof child.submit === 'function') {
                        handle.submit = child.submit;
                        if (typeof child.reset === 'function') {
                            handle.reset = child.reset;
                        }
                    }
                });
                return handle;
            },
            beforeClose () {
                return leaveConfirm();
            },
            /**
             * @desc 关闭弹层
             */
            close () {
                window.changeAlert = this.pageChangeAlertMemo;
                this.$emit('update:isShow', false);
            },
            /**
             * @desc 弹框的确认操作如果子组件有配置submit方案就执行
             */
            handleSubmit () {
                this.isSubmiting = true;
                Promise.resolve(this.checkHandle().submit())
                    .then(() => {
                        window.changeAlert = false;
                        this.close();
                    })
                    .finally(() => {
                        this.isSubmiting = false;
                    });
            },
            /**
             * @desc 关闭弹框时如果子组件有配置reset方案就执行
             */
            handleCancel () {
                leaveConfirm()
                    .then(() => this.checkHandle().reset())
                    .then(() => this.close())
                    .catch(_ => _);
            },
        },
    };
</script>
<style lang="postcss">
    .jb-sideslider {
        .jb-sideslider-content {
            padding: 20px 30px;
        }

        .bk-sideslider-footer {
            background: #fff !important;
        }

        .jb-sideslider-footer {
            display: flex;
            align-items: center;
            width: 100%;
            height: 100%;
            padding: 0 30px;
        }
    }
</style>
