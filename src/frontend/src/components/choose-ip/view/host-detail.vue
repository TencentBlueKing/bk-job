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
    ref="hostDetail"
    class="choose-ip-host-detail">
    <sideslider-box
      :value="show"
      @change="handleClose">
      <div slot="title">
        {{ data.name }}
      </div>
      <div slot="desc">
        <statistics-text
          slot="desc"
          :data="data" />
        <action-extend
          copyable
          :list="list" />
      </div>
      <host-table :list="list" />
    </sideslider-box>
  </div>
</template>
<script>
  import ActionExtend from '../components/action-extend';
  import HostTable from '../components/host-table';
  import SidesliderBox from '../components/sideslider-box';
  import StatisticsText from '../components/statistics-text';
  import {
    sortHost,
  } from '../components/utils';

  export default {
    name: 'HostDetail',
    components: {
      ActionExtend,
      SidesliderBox,
      StatisticsText,
      HostTable,
    },
    model: {
      prop: 'show',
      event: 'input',
    },
    props: {
      append: {
        type: Function,
        required: true,
      },
      show: {
        type: Boolean,
        default: false,
      },
      data: {
        type: Object,
        default: () => ({
          host: [],
        }),
      },
    },
    data() {
      return {
        list: [],
      };
    },
    watch: {
      data: {
        handler(data) {
          if (!data.host) {
            this.list = [];
            return;
          }
          this.list = Object.freeze(sortHost(data.host));
        },
        immediate: true,
      },
    },
    mounted() {
      this.append().appendChild(this.$refs.hostDetail);
      this.$once('hook:beforeDestroy', () => {
        const $target = this.append();
        if (this.append()) {
          $target.removeChild(this.$refs.hostDetail);
        }
      });
    },
    methods: {
      handleClose() {
        this.currentPage = 1;
        this.$emit('input', false);
        this.$emit('change', false);
      },
    },
  };
</script>
<style lang="postcss">
  .choose-ip-host-detail {
    .choose-ip-host-table {
      tr {
        td {
          border-bottom: 1px solid #e7e8ed !important;
        }
      }
    }
  }
</style>
