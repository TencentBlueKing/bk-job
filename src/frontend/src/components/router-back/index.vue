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
    <Icon
        v-if="showBack"
        class="jb-router-back"
        svg
        type="back1"
        @click="handleGoBack" />
</template>
<script>
    import _ from 'lodash';

    import {
        taskExport,
        taskImport,
    } from '@utils/cache-helper';

    export default {
        name: 'RouterBack',
        props: {
            /**
             * @desc 通过浏览器直接访问时总是显示路由回退按钮，通过iframe访问系统时需要根据场景显示路由回退按钮。
             */
            mode: {
                type: String,
                default: 'window',
            },
        },
        data () {
            return {
                showBack: false,
            };
        },
        watch: {
            '$route' () {
                this.check();
                const { name } = this.$route;
                // 路由变更清空作业导入缓存
                if (name !== 'taskImport') {
                    taskImport.clearItem();
                }
                // 路由变更清空作业导入数据
                if (name !== 'taskExport') {
                    taskExport.clearItem();
                }
            },
        },
        created () {
            this.timer = '';
            this.$once('hook:beforeDestroy', () => {
                clearTimeout(this.timer);
            });
        },
        mounted () {
            this.check();
        },
        methods: {
            check () {
                // iframe 嵌入的方式有两种场景
                // 1，paas 桌面的方式需要显示完整的导航
                // 2，第三方系统嵌入的方式不需要显示
                if (window.frames.length !== parent.frames.length
                    && this.mode !== 'window') {
                    // iframe 访问路由需要做定制处理
                    this.showBack = false;
                    // 在作业执行详情页面访问步骤执行详情页，显示路由回退按钮
                    if (this.$route.name === 'historyStep'
                        && this.$route.query.from === 'historyTask') {
                        this.showBack = true;
                    }
                    return;
                }
                this.timer = setTimeout(() => {
                    try {
                        const viewInstance = _.last(this.$route.matched).instances.default || {};
                        this.showBack = Object.prototype.hasOwnProperty.call(viewInstance, 'routerBack');
                    } catch (error) {
                        this.check();
                    }
                }, 100);
            },
            handleGoBack () {
                window.routerFlashBack = true;
                const viewInstance = _.last(this.$route.matched).instances.default || {};
                if (typeof viewInstance.routerBack === 'function') {
                    viewInstance.routerBack();
                }
            },
        },
    };
</script>
<style lang='postcss' scoped>
    .jb-router-back {
        position: relative;
        z-index: 1999;
        flex: 0 0 auto;
        padding: 16px 8px 16px 24px;
        margin-left: -24px;
        font-size: 14px;
        color: #3a84ff;
        cursor: pointer;
    }
</style>
