<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
  <layout
    class="job-site"
    :class="{ loading }">
    <template #back>
      <router-back />
    </template>
    <template slot="headerRight">
      <bk-popover
        :arrow="false"
        style="margin-right: 8px;"
        theme="light site-header-dropdown"
        :tippy-options="{ hideOnClick: false }">
        <div class="flag-box">
          <icon
            id="siteLocal"
            class="lang-flag"
            :type="currentLangType" />
        </div>
        <div slot="content">
          <div
            class="item"
            :class="{ active: currentLangType === 'lang-zh' }"
            @click="handleToggleLang('zh-CN')">
            <icon
              class="lang-flag"
              type="lang-zh" />
            <span>中文</span>
          </div>
          <div
            class="item"
            :class="{ active: currentLangType === 'lang-en' }"
            @click="handleToggleLang('en')">
            <icon
              class="lang-flag"
              type="lang-en" />
            <span>English</span>
          </div>
        </div>
      </bk-popover>
      <bk-popover
        :arrow="false"
        style="margin-right: 14px;"
        theme="light site-header-dropdown"
        :tippy-options="{ hideOnClick: false }">
        <div class="flag-box">
          <icon
            id="siteHelp"
            type="help-document-fill" />
        </div>
        <div slot="content">
          <div
            class="item"
            @click="handleLocationDocument">
            {{ $t('产品文档') }}
          </div>
          <div
            class="item"
            @click="handleShowSystemLog">
            {{ $t('版本日志') }}
          </div>
          <div
            class="item"
            @click="handleLocationFeedback">
            {{ $t('问题反馈') }}
          </div>
          <div
            class="item"
            @click="handleLocationOpenSource">
            {{ $t('开源社区') }}
          </div>
        </div>
      </bk-popover>
      <bk-popover
        :arrow="false"
        theme="light site-header-dropdown"
        :tippy-options="{ hideOnClick: false }">
        <div class="user-flag">
          <span style="margin-right: 5px;">{{ currentUser.username }}</span>
          <i class="bk-icon icon-down-shape" />
        </div>
        <template slot="content">
          <div
            class="item"
            @click="handleLogout">
            {{ $t('退出登录') }}
          </div>
        </template>
      </bk-popover>
    </template>
    <router-view />
    <system-log v-model="showSystemLog" />
    <jb-ai v-if="isAiEnable" />
  </layout>
</template>
<script>
  import Cookie from 'js-cookie';

  import AiService from '@service/ai';
  import EnvService from '@service/env';
  import LogoutService from '@service/logout';
  import QueryGlobalSettingService from '@service/query-global-setting';
  import UserService from '@service/user';

  import { jsonp } from '@utils/assist';
  import EventBus from '@utils/event-bus';

  import RouterBack from '@components/router-back';
  import SystemLog from '@components/system-log';

  import { getPlatformConfig, setDocumentTitle, setShortcutIcon } from '@blueking/platform-config';

  import I18n, { setLocale } from '@/i18n';

  import Layout from './layout-new';

  export default {
    name: 'App',
    components: {
      Layout,
      RouterBack,
      SystemLog,
    },

    data() {
      return {
        loading: true,
        currentUser: {},
        appList: [],
        showSystemLog: false,
        relatedSystemUrls: {},
        envConfig: {
          'esb.url': '',
          bkDomain: '',
        },
        isAiEnable: false,
      };
    },
    computed: {
      currentLangType() {
        if (this.$i18n.locale === 'en-US') {
          return 'lang-en';
        }
        return 'lang-zh';
      },
    },
    watch: {
      '$route'() {
        this.updateDocumentTitle();
      },
    },
    /**
     * @desc 获取系统信息
     */
    created() {
      this.fetchUserInfo();
      this.fetchRelatedSystemUrls();
      this.fetchEnv();
      this.fetchAiConfig();
    },
    /**
     * @desc 页面渲染完成
     *
     * loading用于控制页面切换效果
     */
    mounted() {
      setTimeout(() => {
        this.loading = false;
      }, 100);
    },
    methods: {
      /**
       * @desc 获取登录用户信息
       */
      fetchUserInfo() {
        UserService.fetchUserInfo()
          .then((data) => {
            this.currentUser = Object.freeze(data);
          });
      },
      /**
       * @desc 获取系统关联的外链
       */
      fetchRelatedSystemUrls() {
        QueryGlobalSettingService.fetchRelatedSystemUrls()
          .then((data) => {
            this.relatedSystemUrls = Object.freeze(data);

            return getPlatformConfig(data.BK_SHARED_RES_BASE_JS_URL, {
              name: '作业平台',
              nameEn: 'JOB',
              appLogo: '/static/images/logo.png',
              brandName: '蓝鲸智云',
              brandNameEn: 'Tencent BlueKing',
              favicon: '/static/images/favicon.icon',
              productName: '作业平台', // 产品名称，展示在logo区域 1.0.5版本新增
              productNameEn: 'JOB',
              version: process.env.JOB_VERSION,
            }).then((data) => {
              window.PROJECT_CONFIG.HELPER_CONTACT_LINK = data.helperLink;
              this.$store.commit('platformConfig/update', data);
            });
          })
          .finally(() => {
            this.updateDocumentTitle();
          });
      },
      fetchEnv() {
        EnvService.fetchProperties()
          .then((data) => {
            this.envConfig = data;
          });
      },
      fetchAiConfig() {
        AiService.fetchConfig()
          .then((data) => {
            this.isAiEnable = data.enabled;
          });
      },
      /**
       * @desc 更新网站title
       */
      updateDocumentTitle() {
        const routeMatchStack = [];
        this.$route.matched.forEach((matcheRoute) => {
          if (matcheRoute.meta.title) {
            routeMatchStack.push(matcheRoute.meta.title);
          }
        });

        setDocumentTitle(this.$store.state.platformConfig.i18n, routeMatchStack);
        setShortcutIcon(this.$store.state.platformConfig.favicon);
      },
      /**
       * @desc 切换语言
       * @param {String} lang 语言类型
       */
      handleToggleLang(lang) {
        Cookie.remove('blueking_language', { path: '' });
        Cookie.set('blueking_language', lang.toLocaleLowerCase(), {
          expires: 365,
          domain: this.envConfig.bkDomain,
        });
        setLocale(lang);
        jsonp(`${this.envConfig['esb.url']}/api/c/compapi/v2/usermanage/fe_update_user_language/?language=${lang.toLocaleLowerCase()}`);
        window.location.reload();
      },
      /**
       * @desc 显示版本更新日志
       */
      handleShowSystemLog() {
        this.showSystemLog = true;
      },
      /**
       * @desc 打开产品文档
       */
      handleLocationDocument() {
        if (!this.relatedSystemUrls.BK_DOC_JOB_ROOT_URL) {
          this.messageError(I18n.t('网络错误，请刷新页面重试'));
          return;
        }
        window.open(`${this.relatedSystemUrls.BK_DOC_JOB_ROOT_URL}/UserGuide/Introduction/What-is-Job.md`);
      },
      /**
       * @desc 打开问题反馈
       */
      handleLocationFeedback() {
        if (!this.relatedSystemUrls.BK_FEED_BACK_ROOT_URL) {
          this.messageError(I18n.t('网络错误，请刷新页面重试'));
          return;
        }
        window.open(this.relatedSystemUrls.BK_FEED_BACK_ROOT_URL);
      },
      handleLocationOpenSource() {
        window.open('https://github.com/TencentBlueKing/bk-job');
      },
      /**
       * @desc 退出登录
       */
      handleLogout() {
        this.$bkInfo({
          title: I18n.t('确认退出登录？'),
          confirmFn: () => {
            EventBus.$emit('logout');
            LogoutService.logout();
          },
        });
      },
    },
  };
</script>
<style lang="postcss">
  .job-site {
    opacity: 100%;

    &.loading {
      opacity: 0%;
    }

    .flag-box {
      position: relative;
      display: inline-flex;
      width: 32px;
      height: 32px;
      font-size: 16px;
      color: #979ba5;
      cursor: pointer;
      border-radius: 50%;
      transition: background 0.15s;
      align-items: center;
      justify-content: center;

      &:hover {
        z-index: 1;
        color: #3a84ff;
        background: #f0f1f5;
      }
    }

    .user-flag {
      display: flex;
      height: 32px;
      font-size: 12px;
      color: #c4c6cc;
      cursor: pointer;
      align-items: center;

      &:hover {
        color: #3a84ff;
      }
    }
  }

  .site-header-dropdown-theme {
    padding-right: 0 !important;
    padding-left: 0 !important;

    .item {
      display: flex;
      height: 32px;
      padding: 0 16px;
      font-size: 12px;
      color: #63656e;
      cursor: pointer;
      align-items: center;

      &.active,
      &:hover {
        color: #3a84ff;
        background-color: #eaf3ff;
      }
    }

    .lang-flag {
      margin-right: 4px;
      font-size: 20px;
    }
  }
</style>
