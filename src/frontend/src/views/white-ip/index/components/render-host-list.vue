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
  <div>
    <div
      class="render-host-list"
      @click="handleShowIp">
      <span class="number strong">{{ data.length }}</span>
      {{ $t('whiteIP.台主机') }}
    </div>
    <bk-dialog
      v-model="showHostList"
      :draggable="false"
      header-position="left"
      :title="$t('whiteIP.IP 预览')"
      :width="940">
      <ip-selector
        v-if="showHostList"
        readonly
        show-view
        v-bind="ipSelectorConfig"
        :value="{hostList: data}" />
      <template #footer>
        <bk-button @click="handleHideHostList">
          {{ $t('whiteIP.关闭') }}
        </bk-button>
      </template>
    </bk-dialog>
  </div>
</template>
<script>
  import HostAllManageService from '@service/host-all-manage';

  export default {
    name: '',
    props: {
      data: {
        type: Array,
      },
    },
    data() {
      return {
        showHostList: false,
      };
    },
    created() {
      this.ipSelectorConfig = {
        service: {
          fetchTopologyHostCount: HostAllManageService.fetchTopologyWithCount,
          fetchTopologyHostsNodes: HostAllManageService.fetchTopologyHost,
          fetchTopologyHostIdsNodes: HostAllManageService.fetchTopogyHostIdList,
          fetchHostsDetails: HostAllManageService.fetchHostInfoByHostId,
          fetchHostCheck: HostAllManageService.fetchInputParseHostList,
        },
      };
    },
    methods: {
      handleShowIp() {
        this.showHostList = true;
      },
      handleHideHostList() {
        this.showHostList = false;
      },
    },
  };
</script>
<style lang="postcss" scoped>
  .render-host-list {
    cursor: pointer;
  }
</style>
