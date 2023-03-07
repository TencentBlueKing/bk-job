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
  <div class="global-bariable-batch-operation">
    <table>
      <thead>
        <tr>
          <th style="width: 130px;">
            {{ $t('template.变量类型') }}<span class="require-flag" />
          </th>
          <th>{{ $t('template.变量名称') }}<span class="require-flag" /></th>
          <th>
            <span
              v-bk-tooltips="$t('template.请输入变量的初始值 [可选]')"
              class="hover-tips">
              {{ $t('template.初始值') }}
            </span>
          </th>
          <th style="width: 320px;">
            {{ $t('template.变量描述') }}
          </th>
          <th style="width: 80px;">
            <span
              v-bk-tooltips="$t('template.变量的值在执行中可变')"
              class="hover-tips">
              {{ $t('template.赋值可变') }}
            </span>
          </th>
          <th style="width: 100px;">
            {{ $t('template.执行时必填') }}
          </th>
          <th style="width: 80px;">
            {{ $t('template.操作') }}
          </th>
        </tr>
      </thead>
      <template v-for="(variableItem, index) in variableList">
        <render-table-row
          v-if="variableItem.id > 0 && variableItem.delete !== 1"
          :key="variableItem.id"
          ref="variableEdit"
          :data="variableItem"
          :variable-name-list="calcExcludeNameList(variableItem)"
          @on-append="handleAppendVariable(index)"
          @on-change="value => handleChange(index, value)"
          @on-delete="handleDelete(index)" />
        <create-table-row
          v-else-if="variableItem.id < 0"
          :key="variableItem.id"
          ref="variableCreate"
          :data="variableItem"
          :variable-name-list="calcExcludeNameList(variableItem)"
          @on-append="handleAppendVariable(index)"
          @on-change="value => handleChange(index, value)"
          @on-delete="handleDelete(index)" />
      </template>
    </table>
    <div
      v-if="isEmpty"
      class="empty-box"
      @click="handleAppendVariable(0)">
      <icon type="add-fill" />
      <span>{{ $t('template.全局变量.label') }}</span>
    </div>
  </div>
</template>
<script>
  import _ from 'lodash';

  import GlobalVariableModel from '@model/task/global-variable';

  import { createVariable } from '../util';

  import CreateTableRow from './create-table-row.vue';
  import RenderTableRow from './render-table-row.vue';

  export default {
    name: '',
    components: {
      RenderTableRow,
      CreateTableRow,
    },
    props: {
      variable: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        variableList: _.cloneDeep(this.variable),
      };
    },
    computed: {
      isEmpty() {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.variableList.length; i++) {
          if (!this.variableList[i].delete) {
            return false;
          }
        }
        return true;
      },
    },
    methods: {
      /**
       * @desc 不包含当前索引变量的变量名列表
       * @param { Object } variableData 变量数据
       * @returns { Array }
       *
       * 不包含变量名为空和已删除的变量
       */
      calcExcludeNameList(variableData) {
        return this.variableList.reduce((result, item) => {
          if (variableData.id !== item.id
            && item.name
            && item.delete !== 1) {
            result.push(item.name);
          }
          return result;
        }, []);
      },
      /**
       * @desc 更新变量信息
       * @param {Number} index 编辑的变量索引
       * @param {Object} variableData 全局变量数据
       */
      handleChange(index, variableData) {
        const variableList = [...this.variableList];
        const variable = new GlobalVariableModel(variableData);
        variableList.splice(index, 1, variable);
        this.variableList = variableList;
        window.changeFlag = true;
      },
      /**
       * @desc 删除指定索引的变量
       * @param {Number} index 编辑的变量索引
       */
      handleDelete(index) {
        const variableList = [...this.variableList];
        const editVariable = variableList[index];
        if (editVariable.id > 0) {
          // 删除已存在的变量——设置delete
          editVariable.delete = 1;
        } else {
          // 删除新建的变量——直接删除
          variableList.splice(index, 1);
        }
        this.variableList = variableList;
        window.changeFlag = true;
      },
      /**
       * @desc 在指定索引位置添加一个新变量
       * @param {Number} index 编辑的变量索引
       */
      handleAppendVariable(index) {
        this.variableList.splice(index + 1, 0, createVariable());
        window.changeFlag = true;
      },
      /**
       * @desc 提交编辑
       * @returns {Promise}
       */
      submit() {
        const queue = [];
        if (this.$refs.variableEdit) {
          queue.push(...this.$refs.variableEdit.map(item => item.validate()));
        }
        if (this.$refs.variableCreate) {
          queue.push(...this.$refs.variableCreate.map(item => item.validate()));
        }
        return Promise.all(queue)
          .then(() => this.$emit('on-change', this.variableList));
      },
    },
  };
</script>
<style lang="postcss">
  .global-bariable-batch-operation {
    table {
      width: 100%;
      font-size: 12px;
      line-height: 18px;
      color: #63656e;
      border: 1px solid #dcdee5;
      border-radius: 2px;
      table-layout: fixed;

      thead {
        background: #fafbfd;
      }

      th,
      td {
        height: 41px;
        padding-right: 5px;
        padding-left: 15px;
        text-align: left;
      }

      th {
        font-weight: normal;
        color: #313238;
      }

      td {
        padding-top: 5px;
        padding-bottom: 5px;
        border-top: 1px solid #dcdee5;
      }

      .hover-tips {
        padding-bottom: 2px;
        border-bottom: 1px dashed #c4c6cc;
      }

      .require-flag {
        &::after {
          display: inline-block;
          height: 8px;
          font-size: 12px;
          line-height: 1;
          color: #ea3636;
          vertical-align: middle;
          content: "*";
        }
      }

      .action-row {
        user-select: none;

        .action-btn {
          font-size: 18px;
          color: #c4c6cc;
          cursor: pointer;
        }
      }
    }

    .empty-box {
      display: flex;
      height: 32px;
      margin-top: 6px;
      font-size: 12px;
      color: #979ba5;
      cursor: pointer;
      background: #fcfdff;
      border: 1px dashed #c4c6cc;
      border-radius: 2px;
      align-items: center;
      justify-content: center;

      i {
        margin-right: 7px;
        font-size: 14px;
        color: #c4c6cc;
      }

      &:hover {
        color: #3a84ff;
        border-color: #3a84ff;

        i {
          color: #3a84ff;
        }
      }
    }
  }
</style>
