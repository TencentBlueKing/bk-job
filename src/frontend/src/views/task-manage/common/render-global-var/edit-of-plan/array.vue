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
  <jb-form
    ref="varArrayForm"
    :model="formData">
    <jb-form-item :label="$t('template.数组类型')">
      <div class="array-type-group">
        <bk-radio-group :value="arrayType">
          <bk-radio-button
            class="item"
            :disabled="arrayType !== 5"
            :value="5">
            {{ $t('template.关联数组') }}
          </bk-radio-button>
          <bk-radio-button
            class="item"
            :disabled="arrayType !== 6"
            :value="6">
            {{ $t('template.索引数组') }}
          </bk-radio-button>
        </bk-radio-group>
      </div>
    </jb-form-item>
    <jb-form-item :label="$t('template.变量名称')">
      <bk-input
        v-model="formData.name"
        disabled />
    </jb-form-item>
    <jb-form-item :label="$t('template.变量值')">
      <bk-input
        v-model="formData.defaultValue"
        :native-attributes="{ autofocus: 'autofocus' }" />
    </jb-form-item>
    <jb-form-item :label="$t('template.变量描述')">
      <bk-input
        v-model="formData.description"
        disabled
        maxlength="100"
        :row="5"
        type="textarea" />
    </jb-form-item>
    <jb-form-item>
      <bk-checkbox
        v-model="formData.changeable"
        disabled
        :false-value="0"
        :true-value="1">
        {{ $t('template.赋值可变') }}
      </bk-checkbox>
    </jb-form-item>
    <jb-form-item>
      <bk-checkbox
        v-model="formData.required"
        disabled
        :false-value="0"
        :true-value="1">
        {{ $t('template.执行时必填') }}
      </bk-checkbox>
    </jb-form-item>
  </jb-form>
</template>
<script>
  const getDefaultData = () => ({
    id: 0,
    delete: 0,
    // 变量名
    name: '',
    // 默认值
    defaultValue: '',
    // 变量描述
    description: '',
    // 赋值可变 0-不可变 1-可变
    changeable: 0,
    // 必填 0-非必填 1-必填
    required: 0,
  });
  export default {
    name: 'VarString',
    props: {
      data: {
        type: Object,
        default() {
          return {};
        },
      },
    },
    data() {
      return {
        arrayType: 5,
        formData: getDefaultData(),
      };
    },
    watch: {
      data: {
        handler(value) {
          if (Object.keys(value).length) {
            const { name, defaultValue, description, changeable, required, id, type } = value;
            this.formData = {
              name,
              defaultValue,
              description,
              changeable,
              required,
              id,
              delete: value.delete,
            };
            this.arrayType = type;
          }
        },
        immediate: true,
      },
    },
    methods: {
      submit() {
        return Promise.resolve({
          ...this.formData,
          type: this.arrayType,
        });
      },

      reset() {
        this.formData = getDefaultData();
      },
    },
  };
</script>
<style lang='postcss'>
  .array-type-group {
    position: relative;
    z-index: 1;

    .item {
      .bk-radio-button-text {
        width: 108px;
      }
    }
  }
</style>
