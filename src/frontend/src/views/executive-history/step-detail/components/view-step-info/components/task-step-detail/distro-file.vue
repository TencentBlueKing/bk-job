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
    class="history-detail-distro-file-view"
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
    <detail-item :label="$t('template.执行账号：')">
      {{ executeAccountText }}
    </detail-item>
    <detail-item
      :label="$t('template.执行目标：')"
      layout="vertical">
      <ip-selector
        :complete-container-list="containerDetail(stepInfo.fileDestination.server.executeObjectsInfo.containerList)"
        :complete-host-list="hostsDetails(stepInfo.fileDestination.server.executeObjectsInfo.hostList)"
        readonly
        show-view />
    </detail-item>
    <slot />
  </div>
</template>
<script setup>
  import {
    ref,
    shallowRef,
  } from 'vue';

  import AccountManageService from '@service/account-manage';

  import DetailItem from '@components/detail-layout/item';

  import {
    containerDetail,
    hostsDetails,
  } from '@blueking/ip-selector/dist/adapter';

  import RenderSourceFile from './components/render-source-file';

  const props = defineProps({
    data: {
      type: Object,
      default: () => ({}),
    },
    variable: {
      type: Array,
      default: () => [],
    },
  });

  const isLoading = ref(true);

  const executeAccountText = ref('');
  const account = shallowRef([]);
  const stepInfo = shallowRef(props.data.fileStepInfo);

  AccountManageService.fetchAccountWhole()
    .then((data) => {
      account.value = data;
      const accountData = data.find(item => item.id === stepInfo.value.fileDestination.account);
      if (accountData) {
        executeAccountText.value = accountData.alias;
      } else {
        executeAccountText.value = '--';
      }
    })
    .finally(() => {
      isLoading.value = false;
    });
</script>
<style lang="postcss" scoped>
  .history-detail-distro-file-view {
    &.loading {
      height: calc(100vh - 100px);
    }

    .detail-item {
      margin-bottom: 0;
    }
  }
</style>
