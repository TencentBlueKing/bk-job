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
    <site-frame
        :side-fixed="isFrameSideFixed"
        @on-side-expand="handleSideExpandChange"
        @on-side-fixed="handleSideFixedChnage">
        <template slot="header">
            <Icon
                style="font-size: 28px; color: #96a2b9;"
                svg
                type="job-logo"
                @click="handleRouterChange('home')" />
            <span
                class="site-title"
                @click="handleRouterChange('home')">
                {{ $t('蓝鲸作业平台') }}
            </span>
        </template>
        <template slot="headerCenter">
            <div class="top-menu-box">
                <div
                    v-test="{ type: 'navigation', value: 'home' }"
                    class="top-menu-item"
                    :class="{ active: routerGroup === 'business' }"
                    @click="handleRouterChange('home')">
                    {{ $t('作业管理') }}
                </div>
                <div
                    v-test="{ type: 'navigation', value: 'dashboard' }"
                    class="top-menu-item"
                    :class="{ active: routerGroup === 'operation' }"
                    @click="handleRouterChange('dashboard')">
                    {{ $t('运营分析') }}
                </div>
                <div
                    v-test="{ type: 'navigation', value: 'scriptTemplate' }"
                    class="top-menu-item"
                    :class="{ active: routerGroup === 'personal' }"
                    @click="handleRouterChange('scriptTemplate')">
                    {{ $t('个性化') }}
                </div>
                <div
                    v-if="isAdmin"
                    v-test="{ type: 'navigation', value: 'publicScriptList' }"
                    class="top-menu-item"
                    :class="{ active: routerGroup === 'manage' }"
                    @click="handleRouterChange('publicScriptList')">
                    {{ $t('平台管理') }}
                </div>
            </div>
        </template>
        <template slot="headerRight">
            <slot name="headerRight" />
        </template>
        <template
            v-if="routerGroup === 'business'"
            slot="sideAppendBefore">
            <div class="app-select-box">
                <app-select
                    :show-icon="!isFrameSideFixed && !isSideExpand"
                    style="background: #2d3542;" />
            </div>
        </template>
        <template slot="side">
            <jb-menu
                default-active="fastExecuteScript"
                :flod="!isFrameSideFixed && !isSideExpand"
                @select="handleRouterChange">
                <template v-if="routerGroup === 'business'">
                    <jb-item index="home">
                        <Icon type="job-homepage" />
                        {{ $t('业务概览') }}
                    </jb-item>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('快速执行') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('快速') }}
                        </div>
                        <jb-item index="fastExecuteScript">
                            <Icon type="job-fast-script" />
                            {{ $t('脚本执行') }}
                        </jb-item>
                        <jb-item index="fastPushFile">
                            <Icon type="job-fast-file" />
                            {{ $t('文件分发') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('任务编排') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('任务') }}
                        </div>
                        <jb-item index="taskManage">
                            <Icon type="job-job" />
                            {{ $t('作业') }}
                        </jb-item>
                        <jb-item index="planManage">
                            <Icon type="plan" />
                            {{ $t('执行方案') }}
                        </jb-item>
                        <jb-item index="cronJob">
                            <Icon type="job-timing" />
                            {{ $t('定时') }}
                        </jb-item>
                        <jb-item index="executiveHistory">
                            <Icon type="job-history" />
                            {{ $t('执行历史') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('资源.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('资源.flodTitle') }}
                        </div>
                        <jb-item index="scriptManage">
                            <Icon type="job-script" />
                            {{ $t('脚本') }}
                        </jb-item>
                        <jb-item index="accountManage">
                            <Icon type="job-account" />
                            {{ $t('账号') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group v-if="isEnableFeatureFileManage">
                        <div slot="title">
                            {{ $t('文件源.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('文件') }}
                        </div>
                        <jb-item index="fileManage">
                            <Icon type="file-fill" />
                            {{ $t('文件源.menu') }}
                        </jb-item>
                        <jb-item index="ticketManage">
                            <Icon type="certificate" />
                            {{ $t('凭证') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('管理.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('管理.flodTitle') }}
                        </div>
                        <jb-item index="tagManage">
                            <Icon type="tag" />
                            {{ $t('标签') }}
                        </jb-item>
                        <jb-item index="notifyManage">
                            <Icon type="job-message" />
                            {{ $t('消息通知') }}
                        </jb-item>
                    </jb-item-group>
                </template>
                <template v-if="routerGroup === 'operation'">
                    <jb-item index="dashboard">
                        <Icon type="dashboard" />
                        {{ $t('运营视图') }}
                    </jb-item>
                </template>
                <template v-if="routerGroup === 'personal'">
                    <jb-item index="scriptTemplate">
                        <Icon type="dashboard" />
                        {{ $t('脚本模板') }}
                    </jb-item>
                </template>
                <template v-if="routerGroup === 'manage'">
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('资源.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('资源.flodTitle') }}
                        </div>
                        <jb-item index="publicScript">
                            <Icon type="job-public-script" />
                            {{ $t('公共脚本') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('设置.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('设置.flodTitle') }}
                        </div>
                        <jb-item index="whiteIp">
                            <Icon type="job-white-list" />
                            {{ $t('IP 白名单') }}
                        </jb-item>
                        <jb-item index="globalSetting">
                            <Icon type="job-setting" />
                            {{ $t('全局设置') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('安全.menuGroup') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('安全.flodTitle') }}
                        </div>
                        <jb-item index="dangerousRuleManage">
                            <Icon type="job-white-list" />
                            {{ $t('高危语句规则') }}
                        </jb-item>
                        <jb-item index="detectRecords">
                            <Icon type="job-setting" />
                            {{ $t('检测记录') }}
                        </jb-item>
                    </jb-item-group>
                    <jb-item-group>
                        <div slot="title">
                            {{ $t('视图') }}
                        </div>
                        <div slot="flod-title">
                            {{ $t('视图') }}
                        </div>
                        <jb-item index="service">
                            <Icon type="status-2" />
                            {{ $t('服务状态') }}
                        </jb-item>
                    </jb-item-group>
                </template>
            </jb-menu>
        </template>
        <template slot="contentHeader">
            <slot name="back" />
            <div
                id="sitePageTitle"
                class="page-title">
                <div class="page-title-text">
                    {{ routerTitle }}
                </div>
                <div
                    v-once
                    id="siteHeaderStatusBar" />
            </div>
        </template>
        <div v-test="{ type: 'page', value: $route.name }">
            <slot />
        </div>
    </site-frame>
</template>
<script setup>
    import {
        ref,
        watch,
    } from 'vue';

    import QueryGlobalSettingService from '@service/query-global-setting';

    import AppSelect from '@components/app-select';
    import JbMenu from '@components/jb-menu';
    import JbItem from '@components/jb-menu/item';
    import JbItemGroup from '@components/jb-menu/item-group';
    import SiteFrame from '@components/site-frame';

    import {
        useRoute,
        useRouter,
    } from '@/router';

    const TOGGLE_CACHE = 'navigation_toggle_status';

    let routerName = '';
    const routerGroup = ref('');
    const isFrameSideFixed = ref(localStorage.getItem(TOGGLE_CACHE) !== null);
    const isSideExpand = ref(false);
    const isAdmin = ref(false);
    const routerTitle = ref('');
    const isEnableFeatureFileManage = ref(false);

    const route = useRoute();
    const router = useRouter();

    watch(route, (currentRoute) => {
        routerTitle.value = (currentRoute.meta.title || currentRoute.meta.pageTitle);
        routerName = currentRoute.name;

        // 确认路由分组
        const {
            matched,
        } = currentRoute;
        // eslint-disable-next-line no-plusplus
        for (let i = matched.length - 1; i >= 0; i--) {
            if (matched[i].meta.group) {
                routerGroup.value = matched[i].meta.group;
                break;
            }
        }
    }, {
        immediate: true,
    });
    
    /**
     * @desc 侧导航展开收起
     */
    const handleSideFixedChnage = () => {
        isFrameSideFixed.value = !isFrameSideFixed.value;
        if (isFrameSideFixed.value) {
            localStorage.setItem(TOGGLE_CACHE, isFrameSideFixed.value);
        } else {
            localStorage.removeItem(TOGGLE_CACHE);
        }
    };
    const handleSideExpandChange = (sideExpand) => {
        isSideExpand.value = sideExpand;
    };
    /**
     * @desc 跳转路由
     * @param {String} routerName 跳转的路由名
     */
    const handleRouterChange = (localtionRouterName) => {
        if (routerName === localtionRouterName) {
            return;
        }
        routerName = localtionRouterName;
        router.push({
            name: routerName,
        });
    };
    /**
     * @desc 获取是否是admin用户
     */
    QueryGlobalSettingService.fetchAdminIdentity()
        .then((result) => {
            isAdmin.value = result;
        });
    /**
     * @desc 获取系统基本配置
     */
    QueryGlobalSettingService.fetchJobConfig()
        .then((data) => {
            isEnableFeatureFileManage.value = data.ENABLE_FEATURE_FILE_MANAGE;
        });
            
</script>
<style lang="postcss">
    #app {
        .site-title {
            padding-left: 16px;
            font-size: 18px;
            color: #96a2b9;
        }

        .top-menu-box {
            display: flex;
            padding: 0 4px;

            .top-menu-item {
                padding: 0 20px;
                cursor: pointer;
                transition: all 0.15s;

                &.active {
                    color: #fff;
                }

                &:hover {
                    color: #d3d9e4;
                }
            }
        }

        .app-select-box {
            padding: 0 12px 10px;
            margin-bottom: 10px;
            border-bottom: 1px solid #2f3847;
        }

        .page-title {
            display: flex;
            flex: 1;
            align-items: center;
        }
    }

    #siteHeaderStatusBar {
        flex: 1;
    }
</style>
