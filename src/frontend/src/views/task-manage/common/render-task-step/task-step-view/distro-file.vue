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
    v-bkloading="{ isLoading }"
    class="distro-file-view"
    :class="{ loading: isLoading }">
    <detail-item :label="$t('template.超时时长：')">
      {{ stepInfo.timeout }} (s)
    </detail-item>
    <detail-item :label="$t('template.错误处理：')">
      {{ stepInfo.ignoreErrorText }}
    </detail-item>
    <detail-item :label="$t('template.上传限速：')">
      {{ stepInfo.uploadSpeedLimitText }}
    </detail-item>
    <detail-item :label="$t('template.下载限速：')">
      {{ stepInfo.downloadSpeedLimitText }}
    </detail-item>
    <detail-item
      :label="$t('template.文件来源：')"
      layout="vertical">
      <render-source-file
        v-if="!isLoading"
        :account="account"
        :data="stepInfo.fileSourceList"
        :variable="variable" />
    </detail-item>
    <detail-item :label="$t('template.目标路径：')">
      {{ stepInfo.fileDestination.path }}
    </detail-item>
    <detail-item :label="$t('template.传输模式：')">
      {{ stepInfo.transferModeText }}
    </detail-item>
    <detail-item :label="$t('template.执行帐号：')">
      {{ executeAccountText }}
    </detail-item>
    <detail-item
      v-if="stepInfo.fileDestination.server.variable"
      :label="$t('template.执行目标：')">
      <render-global-variable
        :data="variable"
        :name="stepInfo.fileDestination.server.variable"
        :type="$t('template.执行目标')" />
    </detail-item>
    <detail-item
      v-else
      :label="$t('template.执行目标：')"
      layout="vertical">
      <!-- <server-panel
                detail-fullscreen
                :host-node-info="stepInfo.fileDestination.server.hostNodeInfo" /> -->
      <ip-selector
        readonly
        show-view
        :value="stepInfo.fileDestination.server.hostNodeInfo" />
    </detail-item>
    <slot />
  </div>
</template>
<script>
  import AccountManageService from '@service/account-manage';

  import DetailItem from '@components/detail-layout/item';

  import RenderGlobalVariable from './components/render-global-variable';
  // import ServerPanel from '@components/choose-ip/server-panel';
  import RenderSourceFile from './components/render-source-file';

  export default {
    name: '',
    components: {
      // ServerPanel,
      RenderSourceFile,
      RenderGlobalVariable,
      DetailItem,
    },
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
      variable: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        isLoading: true,
        stepInfo: {},
        executeAccountText: '',
        account: [],
      };
    },
    created() {
      this.stepInfo = Object.freeze(this.data.fileStepInfo);
      this.fetchAccount();
    },
    methods: {
      fetchAccount() {
        this.isLoading = true;
        AccountManageService.fetchAccountWhole()
          .then((data) => {
            this.account = data;
            const accountData = data.find(item => item.id === this.stepInfo.fileDestination.account);
            if (accountData) {
              this.executeAccountText = accountData.alias;
            } else {
              this.executeAccountText = '--';
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .distro-file-view {
    &.loading {
      height: calc(100vh - 100px);
    }

    .detail-item {
      margin-bottom: 0;
    }
  }
</style>
