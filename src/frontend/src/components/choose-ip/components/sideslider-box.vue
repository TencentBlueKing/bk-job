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
    <transition
        :duration="300"
        name="host-sideslider">
        <div v-if="value">
            <div
                class="sideslider-box"
                :style="styles">
                <div
                    class="container"
                    @click.stop="">
                    <div class="box-header">
                        <div
                            class="toggle-btn"
                            @click="handleClose">
                            <Icon
                                class="btn-flag"
                                type="down-small" />
                        </div>
                        <div class="box-title">
                            <slot name="title" />
                            <div class="desc">
                                <slot name="desc" />
                            </div>
                        </div>
                    </div>
                    <div class="box-content">
                        <scroll-faker>
                            <slot />
                        </scroll-faker>
                    </div>
                    <div class="box-footer">
                        <bk-button
                            theme="primary"
                            @click="handleClose">
                            {{ $t('关闭') }}
                        </bk-button>
                    </div>
                </div>
                <div
                    class="box-mask"
                    @click="handleClose" />
            </div>
        </div>
    </transition>
</template>
<script>
    export default {
        name: '',
        props: {
            value: {
                type: Boolean,
                default: false,
            },
        },
        data () {
            return {
                styles: {},
            };
        },
        watch: {
            value: {
                handler (value) {
                    if (value) {
                        this.styles = {
                            'z-index': window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
                        };
                    }
                },
                immediate: true,
            },
        },
        methods: {
            handleClose () {
                this.$emit('input', false);
                this.$emit('change', false);
            },
        },
    };
</script>
<style lang='postcss'>
    .host-sideslider-enter-active,
    .host-sideslider-leave-active {
        .container,
        .box-mask {
            opacity: 100%;
            transition: all 0.3s ease;
        }
    }

    .host-sideslider-enter,
    .host-sideslider-leave-to {
        .container {
            opacity: 0%;
            transform: translateY(-15px);
        }
    }

    .sideslider-box {
        position: fixed;
        top: 0;
        right: 0;
        bottom: 0;
        left: 0;
        z-index: 2000;
        display: flex;
        overflow: hidden;
        align-items: center;
        justify-content: center;

        .container {
            position: relative;
            z-index: 1;
            display: flex;
            width: 1240px;
            height: 754px;
            background: #fff;
            flex-direction: column;
        }

        .box-header {
            position: relative;
            display: flex;
            height: 42px;
            padding-right: 58px;
            border-bottom: 1px solid #dcdee5;
            align-items: center;

            .toggle-btn {
                display: flex;
                width: 32px;
                height: 100%;
                color: #fff;
                cursor: pointer;
                background: #3a84ff;
                align-items: center;
                justify-content: center;
            }

            .btn-flag {
                font-size: 30px;
                transform: rotateZ(-90deg);
            }
        }

        .box-title {
            display: flex;
            padding-left: 20px;
            font-size: 14px;
            color: #313238;
            flex: 1;

            .desc {
                margin-left: auto;
            }
        }

        .box-content {
            height: calc(100% - 100px);
        }

        .box-footer {
            display: flex;
            height: 60px;
            padding-right: 24px;
            background: #fafbfd;
            align-items: center;
            justify-content: flex-end;
        }

        .box-mask {
            position: absolute;
            top: 0;
            right: 0;
            bottom: 0;
            left: 0;
            cursor: pointer;
            background: rgb(0 0 0 / 60%);
        }
    }

    .choose-ip-dialog {
        .sideslider-box {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            z-index: 9;
            display: flex;
            justify-content: flex-end;
            overflow: hidden;

            &.hide {
                height: 0;
            }

            .container {
                height: 100%;
            }

            .host-content-enter-active,
            .host-content-leave-active {
                transition: all 0.3s ease;
            }

            .host-content-enter,
            .host-content-leave-to {
                transform: translateX(100%);
            }
        }

        .host-sideslider-enter,
        .host-sideslider-leave-to {
            .container {
                transform: translateX(100%);
            }
        }
    }
</style>
