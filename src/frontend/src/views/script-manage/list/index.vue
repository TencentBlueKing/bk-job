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
  <layout>
    <template #tag>
      <tag-panel
        ref="tagPanelRef"
        @on-change="handleTagPlanChange" />
    </template>
    <list-action-layout>
      <auth-button
        auth="script/create"
        class="w120"
        theme="primary"
        @click="handleCreate">
        {{ $t('script.新建') }}
      </auth-button>
      <bk-button
        :disabled="isBatchEditTagDisabled"
        @click="handleBatchEditTag">
        {{ $t('script.编辑标签') }}
      </bk-button>
      <template #right>
        <jb-search-select
          ref="search"
          :data="searchSelect"
          :placeholder="$t('script.搜索脚本名称，类型，场景标签，更新人...')"
          style="width: 420px;"
          @on-change="handleSearch" />
      </template>
    </list-action-layout>
    <render-list
      ref="list"
      :data-source="getScriptList"
      :search-control="() => $refs.search"
      selectable
      :size="tableSize"
      @on-selection-change="handleSelection">
      <bk-table-column
        v-if="allRenderColumnMap.id"
        key="id"
        align="left"
        label="ID"
        prop="id"
        width="300" />
      <bk-table-column
        key="name"
        align="left"
        :label="$t('script.脚本名称.colHead')"
        :min-width="300"
        prop="name"
        sortable="custom">
        <template slot-scope="{ row }">
          <auth-component
            auth="script/view"
            :permission="row.canView"
            :resource-id="row.id">
            <jb-edit-input
              :key="row.id"
              v-slot="{ value }"
              field="scriptName"
              :remote-hander="val => handleUpdateScript(row.id, val)"
              :rules="rules.name"
              :value="row.name">
              <span
                style="color: #3a84ff; cursor: pointer;"
                @click="handleVersion(row)">
                {{ value }}
              </span>
            </jb-edit-input>
            <span slot="forbid">{{ row.name }}</span>
          </auth-component>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.type"
        key="type"
        align="left"
        :label="$t('script.脚本语言')"
        prop="type"
        sortable="custom"
        width="120">
        <template slot-scope="{ row }">
          <span>{{ row.typeName }}</span>
        </template>
      </bk-table-column>
      <bk-table-column
        key="tags"
        align="left"
        class-name="edit-tag-column"
        :label="$t('script.场景标签.colHead')"
        prop="tags"
        sortable="custom"
        width="200">
        <template slot-scope="{ row }">
          <auth-component
            auth="script/edit"
            :permission="row.canManage"
            :resource-id="row.id">
            <jb-edit-tag
              :key="row.id"
              field="scriptTags"
              :remote-hander="val => handleUpdateScript(row.id, val)"
              shortcurt
              :value="row.tags" />
            <div slot="forbid">
              {{ row.tagsText }}
            </div>
          </auth-component>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.related"
        key="related"
        align="right"
        :label="$t('script.被引用.colHead')"
        prop="related"
        :render-header="renderHeader"
        width="120">
        <template slot-scope="{ row }">
          <bk-button
            v-bk-tooltips.right.allowHtml="`
                                    <div>${$t('script.作业模板引用')}: ${row.relatedTaskTemplateNum}</div>
                                    <div>${$t('script.执行方案引用')}: ${row.relatedTaskPlanNum}</div>`"
            class="mr20"
            text
            @click="handleShowRelated(row)">
            <span>{{ row.relatedTaskTemplateNum }}</span>
            <span> / </span>
            <span>{{ row.relatedTaskPlanNum }}</span>
          </bk-button>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.version"
        key="version"
        align="left"
        :label="$t('script.线上版本')"
        prop="version"
        show-overflow-tooltip
        width="140">
        <template slot-scope="{ row }">
          <span> {{ row.version || '--' }} </span>
        </template>
      </bk-table-column>
      <bk-table-column
        v-if="allRenderColumnMap.creator"
        key="creator"
        align="left"
        :label="$t('script.创建人.colHead')"
        prop="creator"
        show-overflow-tooltip
        sortable="custom"
        width="140" />
      <bk-table-column
        v-if="allRenderColumnMap.createTime"
        key="createTime"
        align="left"
        :label="$t('script.创建时间')"
        prop="createTime"
        width="180" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyUser"
        key="lastModifyUser"
        align="left"
        :label="$t('script.更新人.colHead')"
        prop="lastModifyUser"
        width="160" />
      <bk-table-column
        v-if="allRenderColumnMap.lastModifyTime"
        key="lastModifyTime"
        align="left"
        :label="$t('script.更新时间')"
        prop="lastModifyTime"
        width="180" />
      <bk-table-column
        key="action"
        align="left"
        fixed="right"
        :label="$t('script.操作')"
        :resizable="false"
        width="170">
        <template slot-scope="{ row }">
          <auth-button
            auth="script/view"
            class="mr10"
            :permission="row.canView"
            :resource-id="row.id"
            text
            @click="handleVersion(row)">
            {{ $t('script.版本管理') }}
          </auth-button>
          <span
            class="mr10"
            :tippy-tips="row.isExecuteDisable ? $t('script.该脚本没有 “线上版本” 可执行，请前往版本管理内设置。') : ''">
            <auth-button
              auth="script/execute"
              :disabled="row.isExecuteDisable"
              :permission="row.canView"
              :resource-id="row.id"
              text
              @click="handleExec(row)">
              {{ $t('script.去执行') }}
            </auth-button>
          </span>
          <jb-popover-confirm
            :confirm-handler="() => handleDelete(row)"
            :content="$t('script.注意！脚本内的所有版本也将被清除')"
            :title="$t('script.确定删除该脚本？')">
            <auth-button
              auth="script/delete"
              :permission="row.canManage"
              :resource-id="row.id"
              text>
              {{ $t('script.删除') }}
            </auth-button>
          </jb-popover-confirm>
        </template>
      </bk-table-column>
      <bk-table-column type="setting">
        <bk-table-setting-content
          :fields="tableColumn"
          :selected="selectedTableColumn"
          :size="tableSize"
          @setting-change="handleSettingChange" />
      </bk-table-column>
    </render-list>
    <jb-dialog
      v-model="isShowBatchEditTag"
      header-position="left"
      :ok-text="$t('script.确定')"
      :title="$t('script.编辑标签')"
      :width="480">
      <batch-edit-tag
        v-if="isShowBatchEditTag"
        :script-list="listSelect"
        @on-change="handleBatchEditChange" />
    </jb-dialog>
    <jb-sideslider
      :is-show.sync="showRelated"
      quick-close
      :show-footer="false"
      :title="$t('script.被引用.label')"
      :width="695">
      <script-related-info
        from="scriptList"
        :info="relatedScriptInfo" />
    </jb-sideslider>
  </layout>
</template>
<script>
  import NotifyService from '@service/notify';
  import PublicScriptService from '@service/public-script-manage';
  import PublicTagManageService from '@service/public-tag-manage';
  import ScriptService from '@service/script-manage';
  import TagManageService from '@service/tag-manage';

  import { checkPublicScript } from '@utils/assist';
  import { listColumnsCache } from '@utils/cache-helper';
  import { scriptNameRule } from '@utils/validator';

  import JbEditInput from '@components/jb-edit/input';
  import JbEditTag from '@components/jb-edit/tag';
  import JbSearchSelect from '@components/jb-search-select';
  import ListActionLayout from '@components/list-action-layout';
  import RenderList from '@components/render-list';

  import ScriptRelatedInfo from '../common/script-related-info';

  import BatchEditTag from './components/batch-edit-tag';
  import Layout from './components/layout';
  import TagPanel from './components/tag-panel';

  import I18n from '@/i18n';

  const TABLE_COLUMN_CACHE = 'script_list_columns';

  export default {
    name: '',
    components: {
      ListActionLayout,
      RenderList,
      ScriptRelatedInfo,
      JbSearchSelect,
      JbEditInput,
      JbEditTag,
      Layout,
      TagPanel,
      BatchEditTag,
    },
    data() {
      return {
        showRelated: false,
        isShowBatchEditTag: false,
        listSelect: [],
        relatedScriptInfo: {
          id: 0,
        },
        searchParams: {},
        selectedTableColumn: [],
        tableSize: 'small',
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
      isBatchEditTagDisabled() {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.listSelect.length; i++) {
          const current = this.listSelect[i];
          if (!current.canManage) {
            return true;
          }
        }
        return this.listSelect.length < 1;
      },
      allRenderColumnMap() {
        return this.selectedTableColumn.reduce((result, item) => {
          result[item.id] = true;
          return result;
        }, {});
      },
    },
    created() {
      // 公共脚本
      this.isPublicScript = checkPublicScript(this.$route);
      this.serviceHandler = this.isPublicScript ? PublicScriptService : ScriptService;
      this.tagSericeHandler = this.isPublicScript ? PublicTagManageService : TagManageService;
      this.getScriptList = this.serviceHandler.scriptList;

      this.searchClass = {};

      this.searchSelect = [
        {
          name: 'ID',
          id: 'scriptId',
          description: I18n.t('script.将覆盖其它条件'),
        },
        {
          name: I18n.t('script.脚本名称.colHead'),
          id: 'name',
          default: true,
        },
        {
          name: I18n.t('script.脚本语言'),
          id: 'type',
          remoteMethod: PublicScriptService.scriptTypeList,
          remoteExecuteImmediate: true,
        },
        {
          name: I18n.t('script.脚本内容.colHead'),
          id: 'content',
          default: true,
        },
        {
          name: I18n.t('script.场景标签.colHead'),
          id: 'tags',
          remoteMethod: this.tagSericeHandler.fetchTagOfSearch,
          remoteExecuteImmediate: true,
        },
        {
          name: I18n.t('script.创建人.colHead'),
          id: 'creator',
          remoteMethod: NotifyService.fetchUsersOfSearch,
          inputInclude: true,
        },
        {
          name: I18n.t('script.更新人.colHead'),
          id: 'lastModifyUser',
          remoteMethod: NotifyService.fetchUsersOfSearch,
          inputInclude: true,
        },
      ];
      this.tableColumn = [
        {
          id: 'id',
          label: 'ID',
        },
        {
          id: 'name',
          label: I18n.t('script.脚本名称.colHead'),
          disabled: true,
        },
        {
          id: 'tags',
          label: I18n.t('script.场景标签.colHead'),
          disabled: true,
        },
        {
          id: 'type',
          label: I18n.t('script.脚本语言'),
        },
        {
          id: 'related',
          label: I18n.t('script.被引用.colHead'),
        },
        {
          id: 'version',
          label: I18n.t('script.线上版本'),
        },
        {
          id: 'creator',
          label: I18n.t('script.创建人.colHead'),
        },
        {
          id: 'createTime',
          label: I18n.t('script.创建时间'),
        },
        {
          id: 'lastModifyUser',
          label: I18n.t('script.更新人.colHead'),
        },
        {
          id: 'lastModifyTime',
          label: I18n.t('script.更新时间'),
        },
      ];
      const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
      if (columnsCache) {
        this.selectedTableColumn = Object.freeze(columnsCache.columns);
        this.tableSize = columnsCache.size;
      } else {
        this.selectedTableColumn = Object.freeze([
          { id: 'name' },
          { id: 'tags' },
          { id: 'type' },
          { id: 'related' },
          { id: 'version' },
          { id: 'related' },
          { id: 'version' },
          { id: 'lastModifyUser' },
          { id: 'lastModifyTime' },
        ]);
      }

      this.rules = {
        name: [
          {
            required: true,
            message: I18n.t('script.脚本名称必填'),
            trigger: 'blur',
          },
          {
            validator: scriptNameRule.validator,
            message: scriptNameRule.message,
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 获取列表数据
       */
      fetchData() {
        // 合并左侧分类和右侧搜索的查询条件
        const searchParams = { ...this.searchParams };
        if (this.searchClass.panelType) {
          searchParams.panelType = this.searchClass.panelType;
        }
        if (this.searchClass.panelTag) {
          searchParams.panelTag = this.searchClass.panelTag;
        }
        this.$refs.list.$emit('onFetch', {
          ...searchParams,
        });
      },
      /**
       * @desc 切换分类
       * @param {String} searchClass 最新分类
       */
      handleTagPlanChange(searchClass) {
        this.searchClass = searchClass;
        this.fetchData();
      },
      /**
       * @desc 批量编辑 tag
       */
      handleBatchEditTag() {
        this.isShowBatchEditTag = true;
      },
      /**
       * @desc tag 批量编辑完成需要刷新列表和 tag 面板数据
       */
      handleBatchEditChange() {
        this.fetchData();
        this.$refs.tagPanelRef.init();
      },
      /**
       * @desc 列表搜索
       * @param {Object} params 搜索条件
       */
      handleSearch(params) {
        this.searchParams = params;
        this.fetchData();
      },
      /**
       * @desc 新建脚本
       */
      handleCreate() {
        if (this.isPublicScript) {
          this.$router.push({
            name: 'createPublicScript',
          });
          return;
        }
        this.$router.push({
          name: 'createScript',
        });
      },
      /**
       * @desc 更新脚本字段
       * @param {String} id 脚本id
       * @param {Object} payload 字段名和值
       */
      handleUpdateScript(id, payload) {
        return this.serviceHandler.scriptUpdateMeta({
          id,
          ...payload,
          updateField: Object.keys(payload)[0],
        }).then(() => {
          if (this.$refs.tagPanelRef) {
            this.$refs.tagPanelRef.init();
          }
        });
      },
      /**
       * @desc 选择脚本
       * @param {Array} selectScriptList 选择的脚本
       */
      handleSelection(selectScriptList) {
        this.listSelect = Object.freeze(selectScriptList);
      },
      /**
       * @desc 自定义列表配置
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
       * @desc 脚本版本列表
       * @param {Object} scriptData 脚本数据
       */
      handleVersion(scriptData) {
        if (this.isPublicScript) {
          this.$router.push({
            name: 'publicScriptVersion',
            params: {
              id: scriptData.id,
            },
          });
          return;
        }
        this.$router.push({
          name: 'scriptVersion',
          params: {
            id: scriptData.id,
          },
        });
      },
      /**
       * @desc 执行脚本
       * @param {Object} scriptData 脚本数据
       */
      handleExec(scriptData) {
        this.serviceHandler.getOneOnlineScript({
          id: scriptData.id,
          publicScript: this.isPublicScript,
        }).then((script) => {
          if (!script) {
            this.$bkMessage({
              limit: 1,
              theme: 'error',
              message: I18n.t('script.操作失败！请前往「版本管理」设置线上版后重试'),
            });
            return;
          }
          this.$router.push({
            name: 'fastExecuteScript',
            params: {
              taskInstanceId: 0,
              scriptVersionId: script.scriptVersionId,
            },
          });
        });
      },
      /**
       * @desc 删除脚本
       * @param {Object} scriptData 脚本数据
       */
      handleDelete(scriptData) {
        if (!scriptData.isEnableRemove) {
          this.messageError(I18n.t('script.脚本正被作业引用中，无法删除'));
          return false;
        }
        return this.serviceHandler.scriptDelete({
          id: scriptData.id,
        }).then(() => {
          this.fetchData();
          this.$refs.tagPanelRef.init();
          this.messageSuccess(I18n.t('script.删除成功'));
          return true;
        });
      },
      /**
       * @desc 脚本引用数据列表
       * @param {String} mode 引用的模板、执行方案
       * @param {Object} scriptData 脚本数据
       */
      handleShowRelated(scriptData) {
        this.showRelated = true;
        this.relatedScriptInfo = scriptData;
      },
      /**
       * @desc 自定义表头
       */
      renderHeader(h, data) {
        return (
                    <span>
                        <span>{ data.column.label }</span>
                        <bk-popover>
                            <icon
                                type="circle-italics-info"
                                style="margin-left: 8px; font-size: 12px;" />
                            <div slot="content">
                                <div>{ I18n.t('script.显示被作业引用的次数') }</div>
                                <div>{ I18n.t('script.显示被执行方案引用的次数') }</div>
                            </div>
                        </bk-popover>
                    </span>
        );
      },
    },
  };
</script>
