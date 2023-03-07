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
  <div class="page-global-set-up">
    <bk-tab
      :active="page"
      :before-toggle="handleTabChange"
      class="page-tab">
      <bk-tab-panel
        :label="$t('setting.通知设置')"
        name="notify" />
      <!-- <bk-tab-panel name="strategy" :label="$t('setting.存储策略')" /> -->
      <bk-tab-panel
        :label="$t('setting.账号命名规则')"
        name="account" />
      <bk-tab-panel
        :label="$t('setting.平台信息')"
        name="platform" />
      <bk-tab-panel
        :label="$t('setting.文件上传设置')"
        name="fileUpload" />
    </bk-tab>
    <div class="set-up-wraper">
      <transition name="slide">
        <component
          :is="pageCom"
          ref="page"
          class="set-up-content" />
      </transition>
    </div>
  </div>
</template>
<script>
  import { leaveConfirm } from '@utils/assist';

  import AccountRule from './pages/account-rule';
  import FileUpload from './pages/file-upload';
  import NotifyManage from './pages/notify-manage';
  import PlatformInfo from './pages/platform-info';
  import StorageStrategy from './pages/storage-strategy';

  export default {
    name: '',
    components: {
      NotifyManage,
      StorageStrategy,
      AccountRule,
      PlatformInfo,
      FileUpload,
    },
    data() {
      return {
        page: 'notify',
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.page.isLoading;
      },
      pageCom() {
        const pageMap = {
          notify: NotifyManage,
          strategy: StorageStrategy,
          account: AccountRule,
          platform: PlatformInfo,
          fileUpload: FileUpload,
        };
        return pageMap[this.page];
      },
    },
    methods: {
      handleTabChange(value) {
        return leaveConfirm().then(() => {
          this.page = value;
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .page-global-set-up {
    .page-tab {
      .bk-tab-section {
        display: none;
      }
    }

    .set-up-wraper {
      width: 100%;
      min-height: 300px;
      overflow: hidden;
      background: #fff;
      border: 1px solid #dcdee5;
      border-top: none;
    }

    .set-up-content {
      position: relative;
      z-index: 2;
      width: 100%;
    }

    .block-title {
      margin-bottom: 14px;
      font-size: 14px;
      font-weight: bold;
      line-height: 1;
      color: #63656e;
    }

    .slide-enter {
      opacity: 0%;
      transform: translateX(200px);
    }

    .slide-enter-active, {
      transition: all 0.5s cubic-bezier(0, 0, 0.19, 1.16);
    }

    .slide-leave {
      display: none;
    }

    .slide-leave-active {
      display: none;
    }
  }
</style>
