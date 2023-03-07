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
  <layout class="template-list-page">
    <template #tag>
      <tag-panel
        ref="tagPanelRef"
        @on-change="handleTagPlanChange" />
    </template>
    <list-action-layout>
      <auth-button
        v-test="{ type: 'button', value: 'templateCreate' }"
        auth="job_template/create"
        class="w120 mr10"
        theme="primary"
        @click="handleCreate">
        {{ $t('template.新建') }}
      </auth-button>
      <bk-badge
        class="mr10"
        theme="#3a84ff"
        :val="backupInfo.importJob.length"
        :visible="backupInfo.importJob.length > 0">
        <span :tippy-tips="backupInfo.importJob.length > 0 ? $t('template.有一项导入任务正在进行中') : ''">
          <auth-button
            v-test="{ type: 'button', value: 'templateImport' }"
            auth="job_template/create"
            @click="handleImport">
            {{ $t('template.导入') }}
          </auth-button>
        </span>
      </bk-badge>
      <bk-badge
        class="mr10"
        theme="#3a84ff"
        :val="backupInfo.exportJob.length"
        :visible="backupInfo.exportJob.length > 0">
        <span :tippy-tips="backupInfo.exportJob.length > 0 ? $t('template.有一项导出任务正在进行中') : ''">
          <bk-button
            v-test="{ type: 'button', value: 'templateExport' }"
            :disabled="isExportJobDisable"
            @click="handleExport">
            {{ $t('template.导出') }}
          </bk-button>
        </span>
      </bk-badge>
      <bk-button
        v-test="{ type: 'button', value: 'templateTagEdit' }"
        :disabled="isBatchEditTagDisabled"
        @click="handleBatchEditTag">
        {{ $t('template.编辑标签') }}
      </bk-button>
      <template #right>
        <jb-search-select
          ref="search"
          :append-value="searchValue"
          :data="searchData"
          :placeholder="$t('template.输入 作业模板名、标签名 或 更新人 进行搜索...')"
          style="width: 420px;"
          @on-change="handleSearch" />
        <bk-button @click="handleMyTask">
          {{ $t('template.我的作业') }}
        </bk-button>
      </template>
    </list-action-layout>
    <div class="task-list-wraper">
      <render-list
        ref="list"
        v-test="{ type: 'list', value: 'template' }"
        :data-source="listDataSource"
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
          width="120" />
        <bk-table-column
          v-if="allRenderColumnMap.name"
          key="name"
          align="left"
          class-name="task-name-column"
          :label="$t('template.作业模板名称')"
          min-width="300"
          prop="name">
          <template slot-scope="{ row }">
            <auth-component
              auth="job_template/view"
              :permission="row.canView"
              :resource-id="row.id">
              <div class="task-name-box">
                <icon
                  class="task-collection"
                  :class="row.favored ? 'favored' : 'unfavored'"
                  type="collection"
                  @click="handleCollection(row)" />
                <router-link
                  v-bk-overflow-tips
                  class="task-name-text"
                  :to="{
                    name: 'templateDetail',
                    params: {
                      id: row.id,
                    },
                  }">
                  {{ row.name }}
                </router-link>
                <router-link
                  :to="{
                    name: 'templateDetail',
                    params: {
                      id: row.id,
                    },
                    query: {
                      mode: 'scriptUpdate',
                    },
                  }">
                  <span v-html="row.scriptStatusHtml" />
                </router-link>
              </div>
              <div
                slot="forbid"
                class="task-name-box">
                <icon
                  class="task-collection"
                  :class="row.favored ? 'favored' : 'unfavored'"
                  type="collection" />
                <span
                  v-bk-overflow-tips
                  class="task-name-text">
                  {{ row.name }}
                </span>
                <span>
                  <span v-html="row.scriptStatusHtml" />
                </span>
              </div>
            </auth-component>
          </template>
        </bk-table-column>
        <bk-table-column
          v-if="allRenderColumnMap.tags"
          key="tags"
          align="left"
          class-name="edit-tag-column"
          :label="$t('template.场景标签.colHead')"
          prop="tags"
          width="200">
          <template slot-scope="{ row }">
            <auth-component
              auth="job_template/edit"
              :permission="row.canEdit"
              :resource-id="row.id">
              <jb-edit-tag
                :key="row.id"
                field="tags"
                :remote-hander="val => handleUpdateTask(row, val)"
                shortcurt
                :value="row.tags" />
              <div slot="forbid">
                {{ row.tagText }}
              </div>
            </auth-component>
          </template>
        </bk-table-column>
        <bk-table-column
          v-if="allRenderColumnMap.statusText"
          key="statusText"
          align="left"
          :label="$t('template.状态')"
          prop="statusText"
          width="100" />
        <bk-table-column
          v-if="allRenderColumnMap.creator"
          key="creator"
          align="left"
          :label="$t('template.创建人')"
          prop="creator"
          width="120" />
        <bk-table-column
          v-if="allRenderColumnMap.createTime"
          key="createTime"
          align="left"
          :label="$t('template.创建时间')"
          prop="createTime"
          width="180" />
        <bk-table-column
          v-if="allRenderColumnMap.lastModifyUser"
          key="lastModifyUser"
          align="left"
          :label="$t('template.更新人.colHead')"
          prop="lastModifyUser"
          width="160" />
        <bk-table-column
          v-if="allRenderColumnMap.lastModifyTime"
          key="lastModifyTime"
          align="left"
          :label="$t('template.更新时间')"
          prop="lastModifyTime"
          width="180" />
        <bk-table-column
          key="action"
          fixed="right"
          :label="$t('template.操作')"
          :resizable="false"
          width="130">
          <template slot-scope="{ row }">
            <router-link
              v-test="{ type: 'link', value: 'planDetail' }"
              class="mr10"
              :to="{
                name: 'viewPlan',
                params: { templateId: row.id },
              }">
              {{ $t('template.执行方案.label') }}
            </router-link>
            <router-link
              v-test="{ type: 'link', value: 'debugTemplate' }"
              class="mr10"
              :to="{
                name: 'debugPlan',
                params: { id: row.id },
                query: { from: 'taskList' },
              }">
              {{ $t('template.调试') }}
            </router-link>
            <list-operation-extend>
              <auth-component
                auth="job_template/edit"
                :permission="row.canEdit"
                :resource-id="row.id">
                <div
                  v-test="{ type: 'button', value: 'editTemplate' }"
                  class="action-item"
                  @click="handleEdit(row.id)">
                  {{ $t('template.编辑') }}
                </div>
                <div
                  slot="forbid"
                  v-test="{ type: 'button', value: 'editTemplate' }"
                  class="action-item">
                  {{ $t('template.编辑') }}
                </div>
              </auth-component>
              <auth-component
                auth="job_template/clone"
                :permission="row.canCreate && row.canView"
                :resource-id="row.id">
                <div
                  v-test="{ type: 'button', value: 'cloneTemplate' }"
                  class="action-item"
                  @click="handleClone(row.id)">
                  {{ $t('template.克隆') }}
                </div>
                <div
                  slot="forbid"
                  v-test="{ type: 'button', value: 'cloneTemplate' }"
                  class="action-item">
                  {{ $t('template.克隆') }}
                </div>
              </auth-component>
              <jb-popover-confirm
                :confirm-handler="() => handleDelete(row.id)"
                :content="$t('template.注意！模板下关联的所有执行方案也将被清除')"
                :title="$t('template.确定删除该作业？')">
                <auth-component
                  auth="job_template/delete"
                  :permission="row.canDelete"
                  :resource-id="row.id">
                  <div
                    v-test="{ type: 'button', value: 'deleteTemplate' }"
                    class="action-item">
                    {{ $t('template.删除') }}
                  </div>
                  <div
                    slot="forbid"
                    v-test="{ type: 'button', value: 'deleteTemplate' }"
                    class="action-item">
                    {{ $t('template.删除') }}
                  </div>
                </auth-component>
              </jb-popover-confirm>
            </list-operation-extend>
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
    </div>
    <jb-dialog
      v-model="isShowBatchEdit"
      header-position="left"
      :ok-text="$t('template.确定')"
      :title="$t('template.编辑标签')"
      :width="480">
      <batch-edit-tag
        v-if="isShowBatchEdit"
        :template-list="listSelect"
        @on-change="handleBatchEditChange" />
    </jb-dialog>
  </layout>
</template>
<script>
  import BackupService from '@service/backup';
  import NotifyService from '@service/notify';
  import TagManageService from '@service/tag-manage';
  import TaskService from '@service/task-manage';
  import UserService from '@service/user';

  import {
    listColumnsCache,
    taskExport,
  } from '@utils/cache-helper';

  import JbEditTag from '@components/jb-edit/tag';
  import JbPopoverConfirm from '@components/jb-popover-confirm';
  import JbSearchSelect from '@components/jb-search-select';
  import ListActionLayout from '@components/list-action-layout';
  import ListOperationExtend from '@components/list-operation-extend';
  import RenderList from '@components/render-list';

  import BatchEditTag from './components/batch-edit-tag.vue';
  import Layout from './components/layout';
  import TagPanel from './components/tag-panel';

  import I18n from '@/i18n';

  const TABLE_COLUMN_CACHE = 'task_list_columns';

  export default {
    name: 'TaskManageList',
    components: {
      JbEditTag,
      ListActionLayout,
      RenderList,
      ListOperationExtend,
      JbSearchSelect,
      JbPopoverConfirm,
      Layout,
      TagPanel,
      BatchEditTag,
    },
    data() {
      return {
        isShowBatchEdit: false,
        currentUser: {},
        searchValue: [],
        listSelect: [],
        backupInfo: {
          importJob: [],
          exportJob: [],
        },
        selectedTableColumn: [],
        tableSize: 'small',
      };
    },
    computed: {
      /**
       * @desc 列表骨架屏 loading
       * @returns { Boolean }
       */
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
      /**
       * @desc 导出功能禁用
       * @returns { Boolean }
       *
       * 1，有导入任务未完成，继续上一次的任务
       * 2，未选中作业
       * 3，选中的作业没用查看权限
       */
      isExportJobDisable() {
        if (this.backupInfo.exportJob.length > 0) {
          return false;
        }
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.listSelect.length; i++) {
          const current = this.listSelect[i];
          if (!current.canView) {
            return true;
          }
        }
        return this.listSelect.length < 1;
      },
      /**
       * @desc 导出功能禁用
       * @returns { Boolean }
       *
       * 1，未选中作业
       * 2，选中的作业没用查看权限
       */
      isBatchEditTagDisabled() {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.listSelect.length; i++) {
          const current = this.listSelect[i];
          if (!current.canEdit) {
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
      this.searchParams = {};
      this.searchClass = {};
      this.listDataSource = TaskService.taskList;
      this.searchData = [
        {
          name: 'ID',
          id: 'templateId',
          description: I18n.t('template.将覆盖其它条件'),
          validate(values, item) {
            const validate = (values || []).every(_ => /^(\d*)$/.test(_.name));
            return !validate ? I18n.t('template.ID只支持数字') : true;
          },
        },
        {
          name: I18n.t('template.作业模板名称'),
          id: 'name',
          default: true,
        },
        {
          name: I18n.t('template.场景标签.colHead'),
          id: 'tags',
          remoteMethod: TagManageService.fetchTagOfSearch,
        },
        {
          name: I18n.t('template.更新人.colHead'),
          id: 'lastModifyUser',
          remoteMethod: NotifyService.fetchUsersOfSearch,
          inputInclude: true,
        },
        {
          name: I18n.t('template.创建人'),
          id: 'creator',
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
          label: I18n.t('template.作业模板名称'),
          disabled: true,
        },
        {
          id: 'tags',
          label: I18n.t('template.场景标签.colHead'),
        },
        {
          id: 'statusText',
          label: I18n.t('template.状态'),
        },
        {
          id: 'creator',
          label: I18n.t('template.创建人'),
        },
        {
          id: 'createTime',
          label: I18n.t('template.创建时间'),
        },
        {
          id: 'lastModifyUser',
          label: I18n.t('template.更新人.colHead'),
        },
        {
          id: 'lastModifyTime',
          label: I18n.t('template.更新时间'),
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
          { id: 'lastModifyUser' },
          { id: 'lastModifyTime' },
        ]);
      }
    },
    mounted() {
      this.fetchUserInfo();
      this.fetchBackup();
    },
    methods: {
      /**
       * @desc 获取作业模板列表
       */
      fetchData() {
        // 合并左侧分类和右侧搜索的查询条件
        const searchParams = { ...this.searchParams };
        if (this.searchClass.type) {
          searchParams.type = this.searchClass.type;
        }
        if (this.searchClass.panelTag) {
          searchParams.panelTag = this.searchClass.panelTag;
        }
        this.$refs.list.$emit('onFetch', searchParams);
      },
      /**
       * @desc 获取登陆用户名
       */
      fetchUserInfo() {
        UserService.fetchUserInfo()
          .then((data) => {
            this.currentUser = data;
          });
      },
      /**
       * @desc 获取作业模板导出记录信息
       */
      fetchBackup() {
        BackupService.fetchInfo()
          .then((data) => {
            this.backupInfo = Object.freeze(data);
          });
      },
      /**
       * @desc 列表自定义配置
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
       * @desc 切换分类
       * @param {String} searchClass 最新分类
       */
      handleTagPlanChange(searchClass) {
        this.searchClass = searchClass;
        this.fetchData();
      },
      /**
       * @desc 切换分类
       * @param {Object} params 搜索参数
       */
      handleSearch(params) {
        this.searchParams = params;
        this.fetchData();
      },
      /**
       * @desc 跳转作业模板新建页
       */
      handleCreate() {
        this.$router.push({
          name: 'templateCreate',
          query: {
            from: 'list',
          },
        });
      },
      /**
       * @desc 跳转作业模板导入页
       */
      handleImport() {
        this.$router.push({
          name: 'taskImport',
        });
      },
      /**
       * @desc 导出选中的作业模板
       *
       * 同时只能存在一个导出任务，如果有导出任务没有结束给出提示
       */
      handleExport() {
        const confirmFn = () => {
          this.$router.push({
            name: 'taskExport',
          });
          taskExport.setItem('ids', this.listSelect.map(_ => _.id));
        };
        if (this.backupInfo.exportJob.length > 0 && this.listSelect.length > 0) {
          this.$bkInfo({
            title: I18n.t('template.上一次任务还未完成'),
            subTitle: I18n.t('template.您上一次的任务还在后台运行中，请先等待完成后再发起新的任务。'),
            okText: I18n.t('template.查看'),
            cancelText: I18n.t('template.返回'),
            confirmFn,
          });
        } else {
          confirmFn();
        }
      },
      /**
       * @desc 批量编辑 TAG
       */
      handleBatchEditTag() {
        this.isShowBatchEdit = true;
      },
      /**
       * @desc tag 批量编辑完成需要刷新列表和 tag 面板数据
       */
      handleBatchEditChange() {
        this.fetchData();
        this.$refs.tagPanelRef.init();
      },
      /**
       * @desc 查看登陆用户的作业模板
       */
      handleMyTask() {
        const currentUserName = this.currentUser.username;
        const creator = {
          name: I18n.t('template.创建人'),
          id: 'creator',
          values: [
            {
              id: currentUserName,
              name: currentUserName,
            },
          ],
        };
        this.searchParams = {
          ...this.searchParams,
          creator: currentUserName,
        };
        this.searchValue = [creator];
        this.fetchData();
      },
      /**
       * @desc 更新作业模板基本信息
       * @param {Object} task 作业模板数据
       * @param {Object} payload 编辑的数据
       */
      handleUpdateTask(task, payload) {
        const { id, name, description } = task;
        return TaskService.taskUpdateBasic({
          id,
          name,
          description,
          tags: payload.tags,
        }).then(() => {
          this.$refs.tagPanelRef.init();
        });
      },
      /**
       * @desc 选择作业模板
       * @param {Array} selectTemplate 选择的作业模板
       */
      handleSelection(selectTemplate) {
        this.listSelect = Object.freeze(selectTemplate);
      },
      /**
       * @desc 编辑作业模板
       * @param {Number} id 选中的作业模板
       */
      handleEdit(id) {
        this.$router.push({
          name: 'templateEdit',
          params: { id },
          query: {
            from: 'list',
          },
        });
      },
      /**
       * @desc 克隆作业模板
       * @param {Number} id 选中的作业模板
       */
      handleClone(id) {
        this.$router.push({
          name: 'templateClone',
          params: { id },
          query: {
            from: 'list',
          },
        });
      },
      /**
       * @desc 删除作业模板
       * @param {Number} id 选中的作业模板
       */
      handleDelete(id) {
        return TaskService.taskDelete({
          id,
        }).then(() => {
          this.fetchData();
          this.messageSuccess(I18n.t('template.删除模板成功'));
          this.$refs.tagPanelRef.init();
          return true;
        });
      },
      /**
       * @desc 收藏作业模板
       * @param {Object} task 选中的作业模板
       */
      handleCollection(task) {
        const requestHander = task.favored ? TaskService.taskDeleteFavorite : TaskService.taskUpdateFavorite;
        requestHander({
          id: task.id,
        }).then(() => {
          task.toggleFavored();
          this.messageSuccess(task.favored ? I18n.t('template.收藏成功') : I18n.t('template.取消收藏成功'));
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .template-list-page {
    .bk-table tr:hover {
      td.task-name-column {
        .task-name-box {
          .task-collection {
            display: inline-block;
          }
        }
      }
    }

    td.task-name-column {
      .cell {
        margin-left: -22px;
      }

      .task-name-box {
        position: relative;
        padding-left: 22px;

        .task-collection {
          position: absolute;
          top: 50%;
          left: 0;
          font-size: 14px;
          color: #c4c6cc;
          cursor: pointer;
          transform: translateY(-50%);

          &.unfavored {
            display: none;
          }

          &.favored {
            display: inline-block;
            color: #ffd695;
          }
        }

        .task-name-text {
          display: inline-block;
          height: 18px;
          max-width: calc(100% - 22px);
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          vertical-align: bottom;
        }
      }
    }
  }

</style>
