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
    class="page-sentence-rule">
    <table class="rule-table">
      <thead>
        <tr>
          <th style="width: 20%;">
            {{ $t('setting.语法检测表达式') }}
          </th>
          <th>{{ $t('setting.规则说明') }}</th>
          <th style="width: 300px;">
            {{ $t('setting.脚本类型') }}
          </th>
          <th style="width: 240px;">
            {{ $t('setting.操作') }}
            <icon
              v-bk-tooltips="{
                theme: 'light',
                content: $t('setting.规则的排序越靠前，表示检测优先级越高'),
              }"
              class="action-tips"
              type="info" />
          </th>
        </tr>
      </thead>
      <table-action-row @on-change="handleAdd" />
      <tbody
        v-for="(rule, index) in list"
        :key="rule.id">
        <tr>
          <td>
            <jb-edit-input
              field="expression"
              :remote-hander="val => handleUpdate(rule, val)"
              :value="rule.expression" />
          </td>
          <td>
            <jb-edit-input
              field="description"
              :remote-hander="val => handleUpdate(rule, val)"
              :value="rule.description" />
          </td>
          <td>
            <bk-select
              class="script-type-edit"
              :clearable="false"
              multiple
              show-select-all
              :value="rule.scriptTypeList"
              @change="val => handleScriptTypeUpdate(rule, val)"
              @toggle="handleSubmitType">
              <bk-option
                v-for="item in scriptTypeList"
                :id="item.id"
                :key="item.id"
                :name="item.name" />
            </bk-select>
          </td>
          <td>
            <div class="action-box">
              <bk-button
                :disabled="index === 0"
                text
                @click="handleMove(index, -1)">
                {{ $t('setting.上移') }}
              </bk-button>
              <bk-button
                :disabled="index + 1 === list.length"
                text
                @click="handleMove(index, 1)">
                {{ $t('setting.下移') }}
              </bk-button>
              <jb-popover-confirm
                :confirm-handler="() => handleDelete(rule.id)"
                :content="$t('setting.脚本编辑器中匹配该规则将不会再收到提醒')"
                :title="$t('setting.确定删除该规则？')">
                <bk-button text>
                  {{ $t('setting.删除') }}
                </bk-button>
              </jb-popover-confirm>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
<script>
  import GlobalSettingService from '@service/global-setting';
  import PublicScriptManageService from '@service/public-script-manage';

  import JbEditInput from '@components/jb-edit/input';
  import JbPopoverConfirm from '@components/jb-popover-confirm';

  import TableActionRow from '../components/table-action-row';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      JbEditInput,
      JbPopoverConfirm,
      TableActionRow,
    },
    data() {
      return {
        isLoading: false,
        list: [],
        scriptTypeList: [],
      };
    },
    created() {
      this.editRule = {};
      this.fetchData();
      this.fetchScriptType();
    },
    methods: {
      /**
       * @desc 获取高危语句规则
       */
      fetchData() {
        this.isLoading = true;
        GlobalSettingService.fetchDangerousRules()
          .then((data) => {
            this.list = data;
          })
          .finally(() => {
            this.isLoading = false;
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
      handleSubmitType(toggle) {
        if (!toggle && this.editRule.scriptTypeList && this.editRule.scriptTypeList.length > 0) {
          GlobalSettingService.updateDangerousRules({
            ...this.editRule,
          }).then(() => {
            this.messageSuccess(I18n.t('setting.编辑成功'));
          });
        }
      },
      /**
       * @desc 更新高危语句配置
       * @param {String} rule 高危语句规则
       * @param {Object} payload 脚本语言列表哦
       */
      handleUpdate(rule, payload) {
        return GlobalSettingService.updateDangerousRules({
          ...rule,
          ...payload,
        }).then(() => {
          Object.assign(rule, payload);
        });
      },
      /**
       * @desc 添加一条高危语句
       */
      handleAdd() {
        this.fetchData();
      },
      /**
       * @desc 移动高危语句的顺序
       * @param {Number} index 当前语句的位置索引
       * @param {Number} step 移动的步数
       */
      handleMove(index, step) {
        this.isLoading = true;
        GlobalSettingService.updateDangerousRulesSort({
          id: this.list[index].id,
          dir: step,
        }).then(() => {
          const current = this.list[index];
          const change = this.list[index + step];
          this.list.splice(index, 1, change);
          this.list.splice(index + step, 1, current);
          this.messageSuccess(step < 0 ? I18n.t('setting.上移成功') : I18n.t('setting.下移成功'));
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
        return GlobalSettingService.deleteDangerousRule({
          id,
        }).then(() => {
          this.messageSuccess(I18n.t('setting.删除成功'));
          this.fetchData();
          return true;
        });
      },
    },
  };
</script>
<style lang='postcss'>
  .page-sentence-rule {
    padding: 42px 40px 50px;

    .rule-table {
      width: 100%;
      border: 1px solid #dcdee5;
      border-radius: 2px;

      th {
        color: #313238;
        background: #fafbfd;
      }

      th,
      td {
        height: 40px;
        padding-left: 20px;
        font-size: 12px;
        color: #63656e;
        text-align: left;
        border-top: 1px solid #dcdee5;
      }

      .bk-button-text {
        font-size: 12px;

        .icon-plus {
          font-size: 18px;
        }
      }
    }

    .input {
      width: 100%;

      .bk-form-input {
        height: 26px;
      }
    }

    .action-tips {
      color: #c4c6cc;
    }

    .action-box {
      display: flex;

      .bk-button-text {
        margin-right: 14px;
      }
    }

    .script-type-edit {
      &.bk-select {
        margin-left: -10px;
        border-color: transparent;

        &:hover {
          background: #f0f1f5;

          .bk-select-angle {
            display: block;
          }
        }

        .bk-select-angle {
          display: none;
        }
      }
    }

    .bk-select {
      .bk-select-name {
        height: 24px;
        line-height: 24px;
      }

      .bk-select-angle {
        top: 2px;
      }
    }
  }
</style>
