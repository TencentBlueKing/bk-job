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
  <div class="related-ticket">
    <jb-search-select
      ref="search"
      class="search-select-style"
      :data="searchSelect"
      :parse-url="false"
      :placeholder="$t('ticket.搜索文件源别名...')"
      @on-change="handleSearch" />
    <render-list
      ref="fileSourcelist"
      :data-source="fetchFileSourceList"
      :search-control="() => $refs.search">
      <bk-table-column width="60">
        <template slot-scope="{ row }">
          <span v-html="row.publicFlagHtml" />
        </template>
      </bk-table-column>
      <bk-table-column
        align="left"
        :label="$t('ticket.文件源别名')"
        show-overflow-tooltip
        sortable="custom">
        <template slot-scope="{ row }">
          <auth-router-link
            v-if="row.isAvailable"
            auth="file_source/view"
            :permission="row.canView"
            :resource-id="row.id"
            target="_blank"
            :to="{
              name: 'fileList',
              query: {
                fileSourceId: row.id,
              },
            }">
            {{ row.alias }}
          </auth-router-link>
          <span
            v-else
            v-bk-tooltips="$t('ticket.接入点异常，暂时不可用')">
            <bk-button
              disabled
              text>{{ row.alias }}</bk-button>
          </span>
        </template>
      </bk-table-column>
      <bk-table-column
        align="left"
        :label="$t('ticket.文件源标识')"
        prop="code"
        show-overflow-tooltip
        sortable="custom"
        width="160" />
      <bk-table-column
        :label="$t('ticket.状态')"
        width="80">
        <template slot-scope="{ row }">
          <icon
            svg
            :type="row.statusIcon" />
          {{ row.statusText }}
        </template>
      </bk-table-column>
      <bk-table-column
        :label="$t('ticket.更新人')"
        prop="lastModifyUser"
        width="120" />
      <bk-table-column
        :label="$t('ticket.更新时间')"
        prop="lastModifyTime"
        width="180" />
    </render-list>
  </div>
</template>
<script>
  import FileManageService from '@service/file-source-manage';

  import JbSearchSelect from '@components/jb-search-select';
  import RenderList from '@components/render-list';

  import I18n from '@/i18n';

  export default {
    name: 'RelatedTicket',
    components: {
      JbSearchSelect,
      RenderList,
    },
    props: {
      credentialId: {
        type: String,
      },
    },
    data() {
      return {
        sourceFileList: [],
      };
    },
    mounted() {
      this.fetchData();
    },
    created() {
      this.fetchFileSourceList = FileManageService.fetchFileSourceList;
      this.searchSelect = [
        {
          name: I18n.t('ticket.文件源别名'),
          id: 'alias',
        },
      ];
    },
    methods: {
      /**
       * @desc 获取被引用文件源列表
       */
      fetchData() {
        this.$refs.fileSourcelist.$emit('onFetch', {
          ...this.searchParams,
          credentialId: this.credentialId,
        });
      },
      /**
       * @desc 过滤表格数据
       * @param {Array} payload 用户输入的过滤数据
       *
       * 重新拉取数据
       */
      handleSearch(payload) {
        this.searchParams = payload;
        this.fetchData();
      },
    },
  };
</script>
<style lang="postcss">
    .related-ticket {
      .search-select-style {
        width: 480px;
        margin-bottom: 20px;
      }
    }
</style>
