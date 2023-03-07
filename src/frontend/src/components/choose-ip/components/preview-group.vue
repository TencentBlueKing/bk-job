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
  <sideslider-box
    class="server-group-host-preview"
    :value="value"
    @change="handleClose">
    <div slot="title">
      {{ $t('分组预览') }}——{{ data.name }}
    </div>
    <div slot="desc">
      <statistics-text :data="statisticsData" />
      <action-extend
        copyable
        :list="list" />
    </div>
    <div
      v-bkloading="{ isLoading }"
      class="preview-wraper">
      <host-table :list="list" />
    </div>
  </sideslider-box>
</template>
<script>
  import HostManageService from '@service/host-manage';

  import {
    sortHost,
    statisticsHost,
  } from '../components/utils';

  import ActionExtend from './action-extend';
  import HostTable from './host-table';
  import SidesliderBox from './sideslider-box';
  import StatisticsText from './statistics-text';

  export default {
    name: '',
    components: {
      HostTable,
      SidesliderBox,
      StatisticsText,
      ActionExtend,
    },
    props: {
      value: {
        type: Boolean,
        default: false,
      },
      data: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        statisticsData: {},
        list: [],
      };
    },
    created() {
      if (this.data.id) {
        this.fetchDynamicGroup();
      }
    },
    methods: {
      fetchDynamicGroup() {
        this.isLoading = true;
        HostManageService.fetchHostOfDynamicGroup({
          id: this.data.id,
        }).then((data) => {
          if (data.length < 1) {
            this.isError = true;
            return;
          }
          this.statisticsData = statisticsHost(data[0].ipListStatus);
          this.list = Object.freeze(sortHost(data[0].ipListStatus));
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handleClose() {
        this.list = [];
        this.statisticsData = {};
        this.$emit('input', false);
        this.$emit('change', false);
      },
    },
  };
</script>
<style lang='postcss'>
  .server-group-host-preview {
    .choose-ip-host-table {
      tr {
        td {
          border-bottom: 1px solid #e7e8ed !important;
        }
      }
    }
  }
</style>
