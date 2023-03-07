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
  <div class="batch-edit-plan-global-variable">
    <div class="action-target-info">
      <span class="label">{{ $t('template.更新范围') }}</span>
      <span class="content-split">:</span>
      <span>
        <span>{{ $t('template.已选') }}</span>
        <span class="number strong">{{ planList.length }}</span>
        <span>{{ $t('template.个执行方案，') }}{{ $t('template.来自') }}</span>
        <span class="number strong">{{ relatedTemplateNums }}</span>
        <span>{{ $t('template.个作业模板') }}</span>
      </span>
    </div>
    <div
      class="action-target-info"
      style="margin-top: 10px;">
      <span class="label">{{ $t('template.选择变量') }}</span>
      <span
        class="content-split"
        style="color: #ea3636;">*</span>
      <bk-button
        v-if="isHasSelectedAll"
        text
        @click="handleCancleSelectAll">
        {{ $t('template.取消全选') }}
      </bk-button>
      <bk-button
        v-else
        text
        @click="handleSelectAll">
        {{ $t('template.选择全部') }}
      </bk-button>
      <span style="margin-left: 36px;">
        <span>{{ $t('template.已选') }}</span>
        <span class="strong">{{ Object.values(selectVariableMap).length }}</span>
        <span> / </span>
        <span
          class="strong"
          style="color: #63656e;">
          {{ Object.values(globalVariableMap).length }}
        </span>
      </span>
    </div>
    <div class="global-variable-list">
      <render-global-variable
        v-for="(variableData, key) in globalVariableMap"
        :key="key"
        :active="!!selectVariableMap[key]"
        :data="variableData"
        @select="handleVariableSelect(key)" />
    </div>
    <div
      v-if="isSelectNotEmpty"
      class="global-variable-value">
      <template v-for="(variableData, key) in globalVariableMap">
        <edit-global-variable
          v-if="selectVariableMap[key]"
          :key="key"
          :data="variableData"
          :value="selectVariableValueMap[key]"
          @on-change="value => handleValueChange(key, value)"
          @on-remove="handleRemoveSelect(key)" />
      </template>
    </div>
  </div>
</template>
<script>
  import TaskHostNodeModel from '@model/task-host-node';

  import {
    genGlobalVariableKey,
  } from '../utils';

  import EditGlobalVariable from './components/edit-global-variable';
  import RenderGlobalVariable from './components/render-global-variable';

  export default {
    name: '',
    components: {
      RenderGlobalVariable,
      EditGlobalVariable,
    },
    inheritAttrs: false,
    props: {
      planList: {
        type: Array,
        required: true,
      },
    },
    data() {
      return {
        relatedTemplateNums: 0,
        globalVariableMap: {},
        selectVariableMap: {},
        selectVariableValueMap: {},
      };
    },
    computed: {
      isHasSelectedAll() {
        const selectNums = Object.values(this.selectVariableMap).length;
        if (selectNums < 1) {
          return false;
        }
        return selectNums === Object.values(this.globalVariableMap).length;
      },
      isSelectNotEmpty() {
        return Object.values(this.selectVariableMap).length > 0;
      },
    },
    created() {
      this.traverPlanList();
    },
    methods: {
      /**
       * @desc 遍历执行方案全局变量
       */
      traverPlanList() {
        const templateIdMap = {};
        const globalVariableMap = {};
        this.planList.forEach((planData) => {
          templateIdMap[planData.templateId] = true;
          planData.variableList.forEach((variableData) => {
            globalVariableMap[genGlobalVariableKey(variableData)] = variableData;
          });
        });

        this.relatedTemplateNums = Object.values(templateIdMap).length;
        this.globalVariableMap = Object.freeze(globalVariableMap);
      },
      /**
       * @desc 编辑全局变量值更新
       */
      triggerChange() {
        window.changeFlag = true;
        this.$emit('on-edit-change', Object.assign({}, this.selectVariableValueMap));
      },
      /**
       * @desc 外部调用，获取需要更新的执行方案
       * @return {Object} 需要更新的执行方案列表
       */
      getEditValue() {
        return Object.assign({}, this.selectVariableValueMap);
      },
      /**
       * @desc 单次选中全局变量
       * @param {String} key 全局变量的key
       */
      handleVariableSelect(key) {
        const selectVariableMap = Object.assign({}, this.selectVariableMap);
        const selectVariableValueMap = Object.assign({}, this.selectVariableValueMap);
        if (selectVariableMap[key]) {
          // 删除选择
          delete selectVariableMap[key];
          // 删除值
          delete selectVariableValueMap[key];
        } else {
          // 选中变量
          selectVariableMap[key] = true;
          // 初始化选中变量值
          if (this.globalVariableMap[key].isHost) {
            selectVariableValueMap[key] = new TaskHostNodeModel({});
          } else {
            selectVariableValueMap[key] = '';
          }
        }
        this.selectVariableMap = Object.freeze(selectVariableMap);
        this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
        this.triggerChange();
      },
      /**
       * @desc 全选全局变量
       */
      handleSelectAll() {
        const selectVariableMap = {};
        const selectVariableValueMap = {};
        for (const key in this.globalVariableMap) {
          // 选中变量
          selectVariableMap[key] = true;
          // 初始化选中变量值
          if (this.globalVariableMap[key].isHost) {
            selectVariableValueMap[key] = new TaskHostNodeModel({});
          } else {
            selectVariableValueMap[key] = '';
          }
        }
        this.selectVariableMap = Object.freeze(selectVariableMap);
        this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
        this.triggerChange();
      },
      /**
       * @desc 取消全选全局变量
       */
      handleCancleSelectAll() {
        // 删除选择
        this.selectVariableMap = {};
        // 删除值
        this.selectVariableValueMap = {};
        this.triggerChange();
      },
      /**
       * @desc 取消选择单个全局变量
       * @param {String} key 全局变量的key
       *
       * 取消选择时需要清除已编辑的值
       */
      handleRemoveSelect(key) {
        // 删除选择
        const selectVariableMap = Object.assign({}, this.selectVariableMap);
        delete selectVariableMap[key];
        this.selectVariableMap = Object.freeze(selectVariableMap);
        // 删除值
        const selectVariableValueMap = Object.assign({}, this.selectVariableValueMap);
        delete selectVariableValueMap[key];
        this.selectVariableValueMap = Object.freeze(selectVariableValueMap);
        this.triggerChange();
      },
      /**
       * @desc 编辑单个全局变量的值
       * @param {String} key 全局变量的key
       * @param {Any} value 全局变量的value
       */
      handleValueChange(key, value) {
        this.selectVariableValueMap = Object.freeze(Object.assign({}, this.selectVariableValueMap, {
          [key]: value,
        }));
        this.triggerChange();
      },
    },
  };
</script>
<style lang='postcss'>
  .batch-edit-plan-global-variable {
    .action-target-info {
      display: flex;
      padding-top: 20px;
      font-size: 14px;
      line-height: 20px;
      color: #63656e;

      .label {
        color: #313238;
      }

      .content-split {
        width: 18px;
        text-align: left;
      }
    }

    .global-variable-list {
      display: flex;
      flex-wrap: wrap;
      padding-top: 4px;
      margin: 0 -5px;
    }

    .global-variable-value {
      margin-top: 30px;
    }
  }
</style>
