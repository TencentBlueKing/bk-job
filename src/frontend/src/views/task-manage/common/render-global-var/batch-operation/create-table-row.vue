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
  <tr class="global-variable-create-row">
    <td>
      <bk-select
        class="offset-left"
        :clearable="false"
        :value="formData.type"
        @change="value => handleChange('type', value)">
        <bk-option
          v-for="item in typeList"
          :id="item.id"
          :key="item.id"
          :name="item.name" />
      </bk-select>
    </td>
    <td>
      <div
        class="variable-name-box offset-left"
        :class="{
          'edit-error': isNameError,
        }">
        <bk-input
          :value="formData.name"
          @blur="handleShowNameError"
          @change="value => handleChange('name', value)" />
        <icon
          v-if="isNameError"
          v-bk-tooltips="errorNameText"
          class="input-error"
          type="info" />
      </div>
    </td>
    <td>
      <template v-if="isHostVarialbe">
        <div
          v-if="formData.defaultTargetValue.isEmpty"
          class="add-host-btn offset-left"
          @click="handleShowChooseIp">
          <icon
            style="margin-right: 6px;"
            type="plus" />
          {{ $t('添加服务器') }}
        </div>
        <jb-edit-host
          v-else
          field="defaultTargetValue"
          :value="formData.defaultTargetValue" />
      </template>
      <template v-else>
        <bk-input
          class="offset-left"
          :value="formData.defaultValue"
          @change="value => handleChange('defaultValue', value)" />
      </template>
    </td>
    <td>
      <jb-textarea
        class="offset-left"
        :value="formData.description"
        @change="value => handleChange('description', value)" />
    </td>
    <td>
      <bk-checkbox
        v-if="withChangable"
        :false-value="0"
        :true-value="1"
        :value="formData.changeable"
        @change="value => handleChange('changeable', value)" />
      <span v-else>--</span>
    </td>
    <td>
      <bk-checkbox
        :false-value="0"
        :true-value="1"
        :value="formData.required"
        @change="value => handleChange('required', value)" />
    </td>
    <td class="action-row">
      <icon
        class="action-btn"
        type="add-fill"
        @click="handleCreate" />
      <icon
        class="action-btn"
        type="reduce-fill"
        @click="handleDelete" />
    </td>
    <!-- <choose-ip
            v-model="isShowChooseIp"
            :host-node-info="formData.defaultTargetValue.hostNodeInfo"
            @on-change="handleHostChange" /> -->
    <ip-selector
      :original-value="originalValue"
      :show-dialog="isShowChooseIp"
      :value="formData.defaultTargetValue.hostNodeInfo"
      @change="handleHostChange"
      @close-dialog="handleCloseIPSelector" />
  </tr>
</template>
<script>
  import _ from 'lodash';

  import GlobalVariableModel from '@model/task/global-variable';

  import { globalVariableNameRule } from '@utils/validator';

  // import ChooseIp from '@components/choose-ip';
  import JbEditHost from '@components/jb-edit/host';

  import { createVariable } from '../util';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      // ChooseIp,
      JbEditHost,
    },
    props: {
      variableNameList: {
        type: Array,
      },
      data: {
        type: Object,
        require: true,
      },
    },
    data() {
      return {
        formData: _.cloneDeep(this.data),
        isShowChooseIp: false,
        isShowNameError: false,
        errorNameText: '',
      };
    },
    computed: {
      /**
       * @desc 是否有赋值可变选项
       * @returns { Boolean }
       */
      withChangable() {
        return [
          GlobalVariableModel.TYPE_STRING,
          GlobalVariableModel.TYPE_RELATE_ARRAY,
          GlobalVariableModel.TYPE_INDEX_ARRAY,
        ].includes(this.formData.type);
      },
      /**
       * @desc 主机变量
       * @returns { Boolean }
       */
      isHostVarialbe() {
        return [
          GlobalVariableModel.TYPE_HOST,
        ].includes(this.formData.type);
      },
      /**
       * @desc 变量名验证失败
       * @returns { Boolean }
       */
      isNameError() {
        return this.isShowNameError && this.errorNameText;
      },
    },
    created() {
      this.originalValue = _.cloneDeep(this.data.defaultTargetValue.hostNodeInfo);

      this.typeList = [
        {
          id: GlobalVariableModel.TYPE_STRING,
          name: I18n.t('template.字符串'),
        },
        {
          id: GlobalVariableModel.TYPE_NAMESPACE,
          name: I18n.t('template.命名空间'),
        },
        {
          id: GlobalVariableModel.TYPE_HOST,
          name: I18n.t('template.主机列表'),
        },
        {
          id: GlobalVariableModel.TYPE_PASSWORD,
          name: I18n.t('template.密文'),
        },
        {
          id: GlobalVariableModel.TYPE_RELATE_ARRAY,
          name: I18n.t('template.关联数组'),
        },
        {
          id: GlobalVariableModel.TYPE_INDEX_ARRAY,
          name: I18n.t('template.索引数组'),
        },
      ];
    },
    methods: {
      /**
       * @desc 验证变量名
       */
      validate() {
        this.isShowNameError = true;
        this.errorNameText = '';
        if (!this.formData.name) {
          this.errorNameText = I18n.t('template.变量名称必填');
        }
        if (!globalVariableNameRule.validator(this.formData.name)) {
          this.errorNameText = globalVariableNameRule.message;
        }
        if (this.variableNameList.includes(this.formData.name)) {
          this.errorNameText = I18n.t('template.变量名称已存在，请重新输入');
        }
        if (this.errorNameText) {
          return Promise.reject(new Error(this.errorNameText));
        }
        return Promise.resolve();
      },
      /**
       * @desc 触发 change 事件
       */
      triggerChange() {
        this.$emit('on-change', this.formData);
      },
      /**
       * @desc name 编辑框失去焦点时进行验证
       */
      handleShowNameError() {
        this.validate();
      },
      /**
       * @desc 设置主机变量的值
       */
      handleShowChooseIp() {
        this.isShowChooseIp = true;
      },
      handleCloseIPSelector() {
        this.isShowChooseIp = false;
      },
      /**
       * @desc 更新主机变量
       * @param { Object } hostNodeInfo 主机信息
       */
      handleHostChange(hostNodeInfo) {
        this.formData.defaultTargetValue.hostNodeInfo = hostNodeInfo;
        this.triggerChange();
      },
      /**
       * @desc 字段值更新
       * @param { String } field 字段名
       * @param { Any } value 字段值
       */
      handleChange(field, value) {
        if (field === 'type') {
          this.formData = createVariable(this.formData.id);
        }
        this.formData[field] = value;
        this.isShowNameError = false;
        this.triggerChange();
      },
      /**
       * @desc 添加新变量
       */
      handleCreate() {
        this.$emit('on-append');
      },
      /**
       * @desc 删除自己
       */
      handleDelete() {
        this.$emit('on-delete');
      },

    },
  };
</script>
<style lang="postcss">
  .global-variable-create-row {
    .offset-left {
      margin-left: -10px;
    }

    .bk-select {
      height: 30px;
      line-height: 30px;
      background: #f7f8fa;
      border: 1px solid transparent;

      &:hover {
        background: #f0f1f5;
      }

      .bk-select-name {
        height: 30px;
        padding-right: 16px;
        padding-left: 10px;
        line-height: 30px;
      }
    }

    .bk-input-text {
      .bk-form-input {
        height: 30px;
        line-height: 30px;
        background: #f7f8fa;
        border: 1px solid transparent;

        &:hover {
          background: #f0f1f5;
        }
      }
    }

    .job-textarea {
      .job-textarea-edit {
        background: #f7f8fa;
        border: 1px solid transparent;

        &:hover {
          background: #f0f1f5;
        }
      }
    }

    .add-host-btn {
      display: flex;
      height: 30px;
      padding: 0 10px;
      font-size: 12px;
      cursor: pointer;
      background: #f7f8fa;
      border-radius: 2px;
      align-items: center;
      justify-content: center;

      &:hover {
        background: #f0f1f5;
      }
    }

    .edit-error {
      .bk-input-text {
        .bk-form-input {
          border-color: #ea3636;
        }
      }
    }

    .variable-name-box {
      position: relative;

      .input-error {
        position: absolute;
        top: 0;
        right: 0;
        bottom: 0;
        z-index: 1;
        display: flex;
        align-items: center;
        padding: 0 10px;
        font-size: 16px;
        color: #ea3636;
        cursor: pointer;
      }
    }

    .bk-form-control {
      display: block;
      width: auto;
    }
  }
</style>
