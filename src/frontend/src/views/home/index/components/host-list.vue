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
    class="host-list-wrapper">
    <bk-table
      :data="hostList"
      ext-cls="host-table"
      :pagination="pagination"
      width="100%"
      @page-change="handlePageChange"
      @page-limit-change="handleLimitChange">
      <bk-table-column
        class-name="ip-item"
        label="IP"
        prop="ip" />
      <bk-table-column :label="$t('home.云区域')">
        <template slot-scope="{ row }">
          {{ row.cloudAreaInfo.name || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('home.主机名')">
        <template slot-scope="{ row }">
          {{ row.ipDesc || '--' }}
        </template>
      </bk-table-column>
      <bk-table-column :label="$t('home.操作系统名称')">
        <template slot-scope="{ row }">
          {{ row.os || '--' }}
        </template>
      </bk-table-column>
    </bk-table>
  </div>
</template>

<script>
  import HomeService from '@service/home';

  export default {
    props: {
      statusType: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        isLoading: false,
        hostList: [],
        pagination: {
          count: 30,
          current: 1,
          limit: 10,
        },
      };
    },
    created() {
      this.fetchData();
    },
    methods: {
      fetchData() {
        const pageSize = this.pagination.limit;
        const start = parseInt(this.pagination.current - 1, 10) * pageSize;
        const params = {
          agentStatus: this.statusType === 'fail' ? 0 : 1,
          start,
          pageSize,
        };
        this.$request(HomeService.fetchAgentStatus(params), () => {
          this.isLoading = true;
        }).then((data) => {
          this.hostList = data.data;
          this.pagination = {
            ...this.pagination,
            count: data.total || 0,
          };
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      handlePageChange(value) {
        this.pagination.current = value;
        this.fetchData();
      },
      handleLimitChange(value) {
        this.pagination.current = 1;
        this.pagination.limit = value;
        this.fetchData();
      },
    },
  };
</script>

<style lang="postcss">
  .host-list-wrapper {
    height: 100%;

    .host-table {
      height: 100%;
      overflow: auto;
      border: none;
    }

    .ip-item {
      padding-left: 40px;
    }
  }
</style>
