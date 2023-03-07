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
  <tbody>
    <tr>
      <td>
        {{ data.typeText }}
      </td>
      <td>
        <jb-edit-input
          field="name"
          mode="block"
          :rules="rules.name"
          :value="data.name"
          @on-change="handleChange" />
      </td>
      <td>
        <jb-edit-host
          v-if="data.isHost"
          field="defaultTargetValue"
          :value="data.defaultTargetValue"
          @on-change="handleChange" />
        <jb-edit-input
          v-else
          field="defaultValue"
          mode="block"
          :value="data.defaultValue"
          @on-change="handleChange" />
      </td>
      <td>
        <jb-edit-textarea
          field="description"
          mode="block"
          :rows="1"
          :value="data.description"
          @on-change="handleChange" />
      </td>
      <td>
        <bk-checkbox
          v-if="withChangable"
          :false-value="0"
          :true-value="1"
          :value="data.changeable" />
        <span v-else>--</span>
      </td>
      <td>
        <bk-checkbox
          :false-value="0"
          :true-value="1"
          :value="data.required" />
      </td>
      <td class="action-row">
        <icon
          class="action-btn"
          type="add-fill"
          @click="handleAppend" />
        <icon
          class="action-btn"
          type="reduce-fill"
          @click="handleDelete" />
      </td>
    </tr>
  </tbody>
</template>
<script>
  import GlobalVariableModel from '@model/task/global-variable';

  import { globalVariableNameRule } from '@utils/validator';

  import JbEditHost from '@components/jb-edit/host';
  import JbEditInput from '@components/jb-edit/input';
  import JbEditTextarea from '@components/jb-edit/textarea';

  import I18n from '@/i18n';

  export default {
    name: '',
    components: {
      JbEditInput,
      JbEditTextarea,
      JbEditHost,
    },
    props: {
      data: {
        type: Object,
        required: true,
      },
      variableNameList: {
        type: Array,
      },
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
        ].includes(this.data.type);
      },
    },
    created() {
      this.rules = {
        name: [
          {
            required: true,
            message: I18n.t('template.变量名称必填'),
            trigger: 'blur',
          },
          {
            validator: globalVariableNameRule.validator,
            message: globalVariableNameRule.message,
            trigger: 'blur',
          },
          {
            validator: val => !this.variableNameList.includes(val),
            message: I18n.t('template.变量名称已存在，请重新输入'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 验证变量名
       * @returns {Promise}
       */
      validate() {
        this.errorNameText = '';
        if (!this.data.name) {
          this.errorNameText = I18n.t('template.变量名称必填');
        } else if (!globalVariableNameRule.validator(this.data.name)) {
          this.errorNameText = globalVariableNameRule.message;
        } else if (this.variableNameList.includes(this.data.name)) {
          this.errorNameText = I18n.t('template.变量名称已存在，请重新输入');
        }
        if (this.errorNameText) {
          return Promise.reject(new Error(this.errorNameText));
        }
        return Promise.resolve();
      },
      /**
       * @desc 更新变量
       * @param {Object} payload 更新字段数据
       */
      handleChange(payload) {
        this.$emit('on-change', Object.assign({}, this.data, payload));
      },
      /**
       * @desc 删除自己
       */
      handleDelete() {
        this.$emit('on-delete');
      },
      /**
       * @desc 添加新变量
       */
      handleAppend() {
        this.$emit('on-append');
      },
    },
  };
</script>
