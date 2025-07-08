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
  <div class="dangerous-rule-manage-page">
    <list-action-layout>
      <bk-button
        class="w120"
        theme="primary"
        @click="handleCreate">
        {{ $t('dangerousRule.新建') }}
      </bk-button>
      <template #right>
        <jb-search-select
          ref="search"
          :data="searchSelect"
          :placeholder="$t('dangerousRule.搜索语法检测表达式，规则说明，脚本类型')"
          style="width: 480px;"
          @on-change="handleSearch" />
      </template>
    </list-action-layout>
    <div v-bkloading="{ isLoading }">
      <bk-table
        ref="list"
        :data="list">
        <bk-table-column
          :label="$t('dangerousRule.语法检测表达式_col')"
          prop="expression">
          <template slot-scope="{ row }">
            <jb-edit-input
              field="expression"
              mode="block"
              :remote-hander="val => handleUpdate(row, val)"
              :rules="formRules.expression"
              :value="row.expression" />
          </template>
        </bk-table-column>
        <bk-table-column
          :label="$t('dangerousRule.规则说明_col')"
          prop="description">
          <template slot-scope="{ row }">
            <jb-edit-input
              field="description"
              mode="block"
              :remote-hander="val => handleUpdate(row, val)"
              :rules="formRules.description"
              :value="row.description" />
          </template>
        </bk-table-column>
        <bk-table-column
          :label="$t('dangerousRule.脚本类型_col')"
          prop="scriptTypeList">
          <template slot-scope="{ row }">
            <jb-edit-select
              field="scriptTypeList"
              :list="scriptTypeList"
              mode="block"
              multiple
              :remote-hander="val => handleUpdate(row, val)"
              :rules="formRules.scriptTypeList"
              show-select-all
              :value="row.scriptTypeList" />
          </template>
        </bk-table-column>
        <bk-table-column
          :label="$t('dangerousRule.动作_col')"
          prop="action"
          :render-header="renderActionHead">
          <template slot-scope="{ row }">
            <edit-action
              :value="row.action"
              @on-change="action => handleUpdate(row, { action })" />
          </template>
        </bk-table-column>
        <bk-table-column
          v-if="allRenderColumnMap.creator"
          key="creator"
          align="left"
          :label="$t('dangerousRule.创建人')"
          prop="creator"
          width="120" />
        <bk-table-column
          v-if="allRenderColumnMap.createTime"
          key="createTime"
          align="left"
          :label="$t('dangerousRule.创建时间')"
          prop="createTime"
          width="180" />
        <bk-table-column
          v-if="allRenderColumnMap.lastModifier"
          key="lastModifier"
          align="left"
          :label="$t('dangerousRule.更新人')"
          prop="lastModifier"
          sortable="custom"
          width="140" />
        <bk-table-column
          v-if="allRenderColumnMap.lastModifyTime"
          key="lastModifyTime"
          align="left"
          :label="$t('dangerousRule.更新时间')"
          prop="lastModifyTime"
          width="180" />
        <bk-table-column
          :label="$t('dangerousRule.操作')"
          :render-header="renderOperationHeader"
          :width="150">
          <template slot-scope="{ row, $index: index }">
            <div class="action-box">
              <bk-switcher
                v-test="{ type: 'button', value: 'toggleRuleStatus' }"
                :false-value="0"
                size="small"
                theme="primary"
                :true-value="1"
                :value="row.status"
                @update="status => handleUpdate(row, { status })" />
              <bk-button
                v-if="!isSearching"
                v-bk-tooltips.top="$t('dangerousRule.上移')"
                v-test="{ type: 'button', value: 'upMoveRule' }"
                class="arrow-btn ml10"
                :disabled="index === 0"
                text
                @click="handleMove(index, -1)">
                <icon type="increase-line" />
              </bk-button>
              <bk-button
                v-if="!isSearching"
                v-bk-tooltips.top="$t('dangerousRule.下移')"
                v-test="{ type: 'button', value: 'downMoveRule' }"
                class="arrow-btn"
                :disabled="index + 1 === list.length"
                text
                @click="handleMove(index, 1)">
                <icon type="decrease-line" />
              </bk-button>
              <jb-popover-confirm
                class="ml10"
                :confirm-handler="() => handleDelete(row.id)"
                :content="$t('dangerousRule.脚本编辑器中匹配该规则将不会再收到提醒')"
                :title="$t('dangerousRule.确定删除该规则？')">
                <bk-button
                  v-test="{ type: 'button', value: 'deleteRule' }"
                  text>
                  {{ $t('dangerousRule.删除') }}
                </bk-button>
              </jb-popover-confirm>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column type="setting">
          <bk-table-setting-content
            :fields="tableColumn"
            :selected="selectedTableColumn"
            :size="tableSize"
            @setting-change="handleSettingChange" />
        </bk-table-column>
        <empty
          v-if="isSearching"
          slot="empty"
          type="search">
          <div>
            <div style="font-size: 14px; color: #63656e;">
              {{ $t('搜索结果为空') }}
            </div>
            <div style="margin-top: 8px; font-size: 12px; line-height: 16px; color: #979ba5;">
              <span>{{ $t('可以尝试调整关键词') }}</span>
              <span>{{ $t('或') }}</span>
              <bk-button
                text
                @click="handleClearSearch">
                {{ $t('清空搜索条件') }}
              </bk-button>
            </div>
          </div>
        </empty>
      </bk-table>
    </div>
    <jb-sideslider
      :is-show.sync="isShowOperation"
      :title="$t('dangerousRule.新增检测规则')"
      :width="650">
      <add-rule @on-change="handleAddRuleChange" />
    </jb-sideslider>
  </div>
</template>
<script>
  import DangerousRuleService from '@service/dangerous-rule';
  import PublicScriptManageService from '@service/public-script-manage';

  import { listColumnsCache } from '@utils/cache-helper';

  import JbEditInput from '@components/jb-edit/input';
  import JbEditSelect from '@components/jb-edit/select';
  import JbPopoverConfirm from '@components/jb-popover-confirm';
  import JbSearchSelect from '@components/jb-search-select';
  import ListActionLayout from '@components/list-action-layout';

  import I18n from '@/i18n';

  import AddRule from './components/add-rule';
  import EditAction from './components/edit-action';

  const TABLE_COLUMN_CACHE = 'accout_list_columns1';

  export default {
    name: '',
    components: {
      JbEditInput,
      JbEditSelect,
      JbPopoverConfirm,
      EditAction,
      ListActionLayout,
      AddRule,
      JbSearchSelect,
    },
    data() {
      return {
        isLoading: true,
        isShowOperation: false,
        list: [],
        scriptTypeList: [],
        tableSize: 'small',
        selectedTableColumn: [],
        searchParams: {},
      };
    },
    computed: {
      isSkeletonLoading() {
        return this.$refs.list.isLoading;
      },
      allRenderColumnMap() {
        return this.selectedTableColumn.reduce((result, item) => {
          result[item.id] = true;
          return result;
        }, {});
      },
      isSearching() {
        return Object.keys(this.searchParams).length > 0;
      },
    },
    created() {
      this.editRule = {};
      this.fetchScriptType();

      this.searchSelect = [
        {
          name: I18n.t('dangerousRule.语法检测表达式_col'),
          id: 'expression',
          default: true,
        },
        {
          name: I18n.t('dangerousRule.规则说明_col'),
          id: 'description',
        },
        {
          name: I18n.t('dangerousRule.脚本类型_col'),
          id: 'scriptTypeList',
          remoteMethod: PublicScriptManageService.scriptTypeList,
        },
        {
          name: I18n.t('dangerousRule.动作_col'),
          id: 'action',
          remoteMethod: DangerousRuleService.fetchActionList,
        },
      ];

      this.formRules = {
        expression: [
          {
            required: true,
            message: I18n.t('dangerousRule.语法检测表达式不能为空'),
          },
        ],
        description: [
          {
            required: true,
            message: I18n.t('dangerousRule.规则说明不能为空'),
          },
        ],
        scriptTypeList: [
          {
            validator: value => value.length > 0,
            message: I18n.t('dangerousRule.脚本类型不能为空'),
          },
        ],
      };

      this.tableColumn = [
        {
          id: 'expression',
          label: I18n.t('dangerousRule.语法检测表达式_col'),
          disabled: true,
        },
        {
          id: 'description',
          label: I18n.t('dangerousRule.规则说明_col'),
          disabled: true,
        },
        {
          id: 'scriptTypeList',
          label: I18n.t('dangerousRule.脚本类型_col'),
          disabled: true,
        },
        {
          id: 'action',
          label: I18n.t('dangerousRule.动作_col'),
          disabled: true,
        },
        {
          id: 'creator',
          label: I18n.t('dangerousRule.创建人'),
        },
        {
          id: 'createTime',
          label: I18n.t('dangerousRule.创建时间'),
        },
        {
          id: 'lastModifier',
          label: I18n.t('dangerousRule.更新人'),
        },
        {
          id: 'lastModifyTime',
          label: I18n.t('dangerousRule.更新时间'),
        },
      ];
      const columnsCache = listColumnsCache.getItem(TABLE_COLUMN_CACHE);
      if (columnsCache) {
        this.selectedTableColumn = Object.freeze(columnsCache.columns);
        this.tableSize = columnsCache.size;
      } else {
        this.selectedTableColumn = Object.freeze([
          { id: 'expression' },
          { id: 'description' },
          { id: 'scriptTypeList' },
          { id: 'action' },
        ]);
      }
    },
    mounted() {
      this.fetchData();
    },
    methods: {
      /**
       * @desc 获取高危语句规则
       */
      fetchData() {
        this.isLoading = true;
        DangerousRuleService.fetchList({
          ...this.searchParams,
        }, {
          permission: 'page',
        })
          .then((data) => {
            this.list = data;
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      renderActionHead(h, data) {
        return (
          <span>
            <span>{ data.column.label }</span>
            <bk-popover placement="top">
              <icon
                class="action-tips"
                type="info" />
              <div slot="content">
                <div>{ I18n.t('dangerousRule._扫描_') }</div>
                <div>{ I18n.t('dangerousRule.命中规则的脚本执行任务仅会做记录，不会拦截') }</div>
                <div style="margin-top: 8px;">
                  { I18n.t('dangerousRule._拦截_') }
                </div>
                <div>{ I18n.t('dangerousRule.命中规则的脚本执行任务会被记录，并中止运行') }</div>
              </div>
            </bk-popover>
          </span>
        );
      },
      renderOperationHeader(h, data) {
        return (
          <span>
            <span>{ data.column.label }</span>
            <bk-popover placement="top">
              <icon
                class="action-tips"
                type="info" />
                <div slot="content">
                  <div>{ I18n.t('dangerousRule.规则的排序越靠前，表示检测优先级越高') }</div>
                </div>
            </bk-popover>
          </span>
        );
      },
      /**
       * @desc 表格列自定义
       * @param { Object } 列信息
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
       * @desc 获取支持的脚本类型列表
       */
      fetchScriptType() {
        PublicScriptManageService.scriptTypeList()
          .then((data) => {
            this.scriptTypeList = data;
          });
      },
      handleClearSearch() {
        this.$refs.search.reset();
      },
      handleCreate() {
        this.isShowOperation = true;
      },
      handleSearch(searchParams) {
        this.searchParams = searchParams;
        this.fetchData();
      },
      /**
       * @desc 更新脚本类型
       * @param {String} rule 高危语句规则
       * @param {Array} scriptTypeList 脚本语言列表哦
       */
      handleScriptTypeUpdate(rule, scriptTypeList) {
        this.editRule = {
          ...rule,
          scriptTypeList,
        };
      },
      /**
       * @desc 脚本语言下拉框收起时提交更新
       * @param {Boolean} toggle 脚本语言下拉框收起状态
       */
      handleSubmitScriptTypeChange(toggle) {
        if (!toggle
          && this.editRule.scriptTypeList
          && this.editRule.scriptTypeList.length > 0) {
          DangerousRuleService.update({
            ...this.editRule,
          }).then(() => {
            this.messageSuccess(I18n.t('dangerousRule.编辑成功'));
          });
        }
      },
      /**
       * @desc 更新高危语句配置
       * @param {Object} rule 高危语句规则
       * @param {Object} payload 脚本语言列表哦
       */
      handleUpdate(rule, payload) {
        return DangerousRuleService.update({
          ...rule,
          ...payload,
        }).then(() => {
          this.messageSuccess(I18n.t('dangerousRule.编辑成功'));
          Object.assign(rule, payload);
        });
      },
      /**
       * @desc 添加一条高危语句
       */
      handleAddRuleChange() {
        this.fetchData();
      },
      /**
       * @desc 移动高危语句的顺序
       * @param {Number} index 当前语句的位置索引
       * @param {Number} step 移动的步数
       */
      handleMove(index, step) {
        this.isLoading = true;
        DangerousRuleService.updateSort({
          id: this.list[index].id,
          dir: step,
        }).then(() => {
          const current = this.list[index];
          const change = this.list[index + step];
          this.list.splice(index, 1, change);
          this.list.splice(index + step, 1, current);
          this.messageSuccess(step < 0 ? I18n.t('dangerousRule.上移成功') : I18n.t('dangerousRule.下移成功'));
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 删除高危语句
       * @param {Number} id 高危语句的id
       */
      handleDelete(id) {
        return DangerousRuleService.remove({
          id,
        }).then(() => {
          this.messageSuccess(I18n.t('dangerousRule.删除成功'));
          this.fetchData();
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .dangerous-rule-manage-page {
    padding-bottom: 20px;

    .action-tips {
      margin-left: 4px;
      color: #c4c6cc;
    }

    .action-box {
      display: flex;
      align-items: center;

      .arrow-btn {
        font-size: 16px;
      }
    }

    .bk-table {
      td {
        background: #fff !important;
      }

      .bk-select-angle {
        display: none;
      }
    }

    .bk-select {
      line-height: 24px;

      .bk-select-name {
        height: 24px;
      }

      .bk-select-angle {
        top: 2px;
      }
    }
  }
</style>
