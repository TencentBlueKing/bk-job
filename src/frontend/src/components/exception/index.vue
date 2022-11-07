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
        v-show="show"
        class="bk-exception bk-exception-center">
        <img :src="image">
        <template v-if="$slots.message">
            <slot name="message" />
        </template>
        <template v-else>
            <h2 class="exception-text">
                {{ message }}
            </h2>
        </template>
    </div>
</template>

<script>
/**
     * @desc  app-exception
     *  @desc 异常页面
     *  @param type {String} - 异常类型，有：404（找不到）、403（权限不足）、500（服务器问题）、building（建设中）
     *  @param delay {Number} - 延时显示
     *  @param text {String} - 显示的文案，默认：有：404（页面找不到了！）、403（Sorry，您的权限不足）、500（）、building(功能正在建设中···)
     *  @example1 <app-exception type="404"></app-exception>
     */

    export default {
        name: 'AppException',
        props: {
            type: {
                type: String,
                default: '404',
            },
            delay: {
                type: Number,
                default: 0,
            },
            text: {
                type: String,
                default: '',
            },
        },
        data () {
            let message = '';
            let image = '';

            switch (this.type) {
                case '403':
                    image = '/static/images/403.png';
                    message = 'Sorry，您的权限不足！';
                    break;

                case '404':
                    image = '/static/images/404.png';
                    message = '页面找不到了！';
                    break;

                case '500':
                    image = '/static/images/500.png';
                    message = '服务器维护中，请稍后重试!';
                    break;

                case 'building':
                    image = '/static/images/building.png';
                    message = '功能正在建设中···';
                    break;
            }

            if (this.text) {
                message = this.text;
            }

            return {
                show: false,
                message,
                image,
            };
        },
        created () {
            setTimeout(() => {
                this.show = true;
            }, this.delay);
        },
    };
</script>
