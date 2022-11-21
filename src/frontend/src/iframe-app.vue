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
        class="iframe-navigation-container"
        :class="{ loading }">
        <div class="container-header">
            <div
                id="sitePageTitle"
                class="container-header-title">
                <router-back mode="iframe" />
                <span>{{ routerTitle }}</span>
                <div id="siteHeaderStatusBar" />
            </div>
        </div>
        <div class="container-content">
            <router-view />
        </div>
    </div>
</template>
<script>
    import RouterBack from '@components/router-back';

    export default {
        name: 'App',
        components: {
            RouterBack,
        },
        data () {
            return {
                loading: true,
                routerTitle: '',
            };
        },
        watch: {
            $route: {
                handler (route) {
                    this.routerTitle = (route.meta.title || route.meta.pageTitle);
                },
                immediate: true,
            },
        },
        /**
         * @desc 页面渲染完成
         *
         * loading用于控制页面切换效果
         */
        mounted () {
            setTimeout(() => {
                this.loading = false;
            }, 100);
        },
    };
</script>
<style lang="postcss" scoped>
    @import "@/css/mixins/scroll";

    .iframe-navigation-container {
        .container-header {
            position: relative;
            z-index: 1999;
            display: flex;
            align-items: center;
            height: 52px;
            padding-left: 20px;
            box-shadow: 0 2px 3px 0 rgb(99 101 110 / 10%);

            .container-header-title {
                display: flex;
                height: 21px;
                font-size: 16px;
                line-height: 21px;
                color: #313238;
                align-items: center;
                flex: 1;
            }
        }

        .container-content {
            position: relative;
            z-index: 1;
            max-height: calc(100vh - 52px);
            padding: 20px 24px 0;
            overflow: auto;
            background: #f5f6fa;

            @mixin scroller;
        }
    }

    #siteHeaderStatusBar {
        display: flex;
    }
</style>
