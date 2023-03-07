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
    class="file-manage-source-file">
    <list-action-layout>
      <auth-button
        auth="file_source/create"
        class="w120"
        theme="primary"
        @click="handleCreate">
        {{ $t('file.新建') }}
      </auth-button>
      <template #right>
        <jb-search-select
          ref="search"
          :data="searchSelect"
          :placeholder="$t('file.请输入文件源名称...')"
          style="width: 480px;"
          @on-change="handleSearch" />
      </template>
    </list-action-layout>
    <render-list
      ref="fileSourcelist"
      :data-source="fetchFileSourceList"
      :search-control="() => $refs.search"
      :size="tableSize">
      <bk-table-column
        key="publicFlag"
        width="40">
        <template slot-scope="{ row }">
          <span v-html="row.publicFlagHtml" />
        </template>
      </bk-table-column>
      <bk-table-column
        key="alias"
        align="left"
        :label="$t('file.文件源别名.colHead')"
        prop="alias"
        show-overflow-tooltip
        sortable>
        <template slot-scope="{ row }">
          <auth-router-link
            v-if="row.isAvailable"
            auth="file_source/view"
            :permission="row.canView"
            :resource-id="row.id"
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
            v-bk-tooltips="$t('file.接入点异常，暂时不可用')">
            <bk-button
              disabled
              text>{{ row.alias }}</bk-button>
          </span>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.code"
        key="code"
        align="left"
        :label="$t('file.文件源标识.colHead')"
        prop="code"
        show-overflow-tooltip
        sortable />
      <bk-table-column
        v-if="allRenderColumnMap.status"
        key="status"
        :label="$t('file.状态')"
        prop="status">
        <template slot-scope="{ row }">
          <icon
            style="vertical-align: middle;"
            svg
            :type="row.statusIcon" />
          <span style="vertical-align: middle;">{{ row.statusText }}</span>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.type"
        key="type"
        :label="$t('file.类型.colHead')"
        prop="storageTypeText" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyUser"
        key="lastModifyUser"
        :label="$t('file.更新人')"
        prop="lastModifyUser" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyTime"
        key="lastModifyTime"
        :label="$t('file.更新时间')"
        prop="lastModifyTime" />
      <bk-table-column
        key="action"
        :label="$t('file.操作')"
        width="160">
        <div
          slot-scope="{ row }"
          class="action-box">
          <auth-component
            auth="file_source/edit"
            class="mr10"
            :permission="row.canManage"
            :resource-id="row.id">
            <bk-switcher
              class="table-enable-switch"
              size="small"
              theme="primary"
              :value="row.enable"
              @change="value => hanldeToggleEnable(value, row)" />
            <bk-switcher
              slot="forbid"
              disabled
              size="small"
              theme="primary"
              :value="row.enable" />
          </auth-component>
          <auth-button
            auth="file_source/edit"
            class="mr10"
            :permission="row.canManage"
            :resource-id="row.id"
            text
            @click="handleEdit(row)">
            {{ $t('file.配置更改') }}
          </auth-button>
          <jb-popover-confirm
            :confirm-handler="() => handleDelete(row)"
            :content="$t('file.该操作只涉及在作业平台的文件源配置，不影响其本体的内容')"
            :title="$t('file.确定删除该文件源？')">
            <auth-button
              auth="file_source/delete"
              :permission="row.canManage"
              :resource-id="row.id"
              text>
              {{ $t('file.删除') }}
            </auth-button>
          </jb-popover-confirm>
        </div>
      </bk-table-column>
      <bk-table-column type="setting">
        <bk-table-setting-content
          :fields="tableColumn"
          :selected="selectedTableColumn"
          :size="tableSize"
          @setting-change="handleSettingChange" />
      </bk-table-column>
    </render-list>
    <jb-sideslider
      v-bind="operationSidesliderInfo"
      footer-offset-target="bk-form-content"
      :is-show.sync="isShowSideslider"
      :width="700">
      <file-opertion
        :data="fileSourceDetailInfo"
        @on-change="handleFileSourceChange" />
    </jb-sideslider>
  </div>
</template>
<script>
  import FileManageService from '@service/file-source-manage';

  import {
    listColumnsCache,
  } from '@utils/cache-helper';

  import JbPopoverConfirm from '@components/jb-popover-confirm';
  import JbSearchSelect from '@components/jb-search-select';
  import ListActionLayout from '@components/list-action-layout';
  import RenderList from '@components/render-list';

  import FileOpertion from './components/opertion';

  import I18n from '@/i18n';

  const TABLE_COLUMN_CACHE = 'file_source_list_columns';

  export default {
    name: 'SourceFile',
    components: {
      RenderList,
      ListActionLayout,
      JbSearchSelect,
      FileOpertion,
      JbPopoverConfirm,
    },
    data() {
      return {
        isLoading: false,
        tableSize: 'small',
        searchParams: [],
        selectedTableColumn: [],
        fileSourceDetailInfo: {},
        isShowSideslider: false,
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.fileSourcelist.isLoading;
      },
      operationSidesliderInfo() {
        if (this.fileSourceDetailInfo.id) {
          return {
            title: I18n.t('file.编辑文件源'),
            okText: I18n.t('file.保存'),
          };
        }
        return {
          title: I18n.t('file.新建文件源'),
          okText: I18n.t('file.提交'),
        };
      },
      allRenderColumnMap() {
        return this.selectedTableColumn.reduce((result, item) => {
          result[item.id] = true;
          return result;
        }, {});
      },
    },
    created() {
      this.fetchFileSourceList = FileManageService.fetchFileSourceList;
      this.searchSelect = [
        {
          id: 'alias',
          name: I18n.t('file.文件源别名.colHead'),
          default: true,
        },
      ];
      this.tableColumn = [
        {
          id: 'alias',
          label: I18n.t('file.文件源别名.colHead'),
          disabled: true,
        },
        {
          id: 'code',
          label: I18n.t('file.文件源标识.colHead'),
        },
        {
          id: 'status',
          label: I18n.t('file.状态'),
        },
        {
          id: 'type',
          label: I18n.t('file.类型.colHead'),
          disabled: true,
        },
        {
          id: 'lastModifyUser',
          label: I18n.t('file.更新人'),
        },
        {
          id: 'lastModifyTime',
          label: I18n.t('file.更新时间'),
        },
      ];

      const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
      if (columnsCache) {
        this.selectedTableColumn = Object.freeze(columnsCache.columns);
        this.tableSize = columnsCache.size;
      } else {
        this.selectedTableColumn = Object.freeze([
          { id: 'alias' },
          { id: 'code' },
          { id: 'status' },
          { id: 'type' },
          { id: 'lastModifyUser' },
          { id: 'lastModifyTime' },
        ]);
      }
    },
    methods: {
      /**
       * @desc 获取文件夹数据
       */
      fetchData() {
        this.$refs.fileSourcelist.$emit('onFetch', {
          ...this.searchParams,
        });
      },

      /**
       * @desc 新建文件源
       *
       * 显示新建文件源模板
       */
      handleCreate() {
        this.fileSourceDetailInfo = {};
        this.isShowSideslider = true;
      },

      /**
       * @desc 编辑文件源
       * @param {Object} payload 选中文件源的详细数据
       *
       * 显示编辑文件源模板
       */
      handleEdit(payload) {
        this.fileSourceDetailInfo = payload;
        this.isShowSideslider = true;
      },

      /**
       * @desc 新建、编辑文件源成功
       *
       * 重新拉取数据
       */
      handleFileSourceChange() {
        this.fetchData();
      },

      /**
       * @desc 文件源是否开启状态切换
       * @param {Boolean} value 是否开启
       * @param {Object} row 文件源详情数据
       */
      hanldeToggleEnable(value, row) {
        const enableMemo = row.enable;
        FileManageService.toggleSourceEnable({
          flag: value,
          id: row.id,
        }).then((res) => {
          this.messageSuccess(value ? I18n.t('file.开启成功') : I18n.t('file.关闭成功'));
        })
          .catch(() => {
            row.enable = enableMemo;
          });
      },

      /**
       * @desc 设置表格显示列/表格size
       */
      handleSettingChange({ fields, size }) {
        this.selectedTableColumn = Object.freeze(fields);
        this.tableSize = size;
        listColumnsCache.setItem(TABLE_COLUMN_CACHE, {
          columns: fields,
          size,
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

      /**
       * @desc 删除文件源
       * @param {Number} id 文件源id
       *
       * 删除成功重新拉取数据
       */
      handleDelete(id) {
        FileManageService.removeSource(id)
          .then((res) => {
            this.messageSuccess(I18n.t('file.删除成功'));
            this.fetchData();
          });
      },

      /**
       * @desc 跳转到bucket存储桶列表
       * @param {Object} row 文件源详情数据
       */
      handelGoBucket(row) {
        this.$router.push({
          name: 'bucketList',
          query: {
            fileSourceId: row.id,
            sourceAlias: row.alias,
          },
        });
      },
    },
  };
</script>
<style lang="postcss">
  .file-manage-source-file {
    .action-box {
      display: flex;
      align-items: center;
    }
  }
</style>
