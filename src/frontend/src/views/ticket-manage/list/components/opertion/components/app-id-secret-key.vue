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
  <jb-form
    ref="form"
    form-type="vertical"
    :model="formData"
    :rules="rules">
    <jb-form-item
      :label="$t('ticket.AppId')"
      property="value1"
      required>
      <bk-input v-model="formData.value1" />
    </jb-form-item>
    <jb-form-item
      :label="$t('ticket.SecretKey')"
      property="value2"
      required>
      <bk-input v-model="formData.value2" />
    </jb-form-item>
    <jb-form-item :label="$t('ticket.描述')">
      <bk-input
        v-model="formData.description"
        maxlength="100"
        type="textarea" />
    </jb-form-item>
  </jb-form>
</template>

<script>
  import I18n from '@/i18n';

  export default {
    name: 'AppIdSecretKey',
    props: {
      data: {
        type: Object,
        default: () => ({}),
      },
      type: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        formData: {
          value1: this.data.value1,
          value2: this.data.value2,
          description: this.data.description,
        },
      };
    },
    created() {
      if (this.type !== 'APP_ID_SECRET_KEY') {
        this.formData.value1 = '';
        this.formData.value2 = '';
      }
      this.rules = {
        value1: [
          {
            required: true,
            message: I18n.t('ticket.AppID必填'),
            trigger: 'blur',
          },
        ],
        value2: [
          {
            required: true,
            message: I18n.t('ticket.SecretKey必填'),
            trigger: 'blur',
          },
        ],
      };
    },
    methods: {
      /**
       * @desc 校验表单
       *
       * 校验成功传递表单数据到父组件
       */
      getData() {
        return this.$refs.form.validate()
          .then(validator => this.formData, validator => Promise.reject(validator.content));
      },
    },
  };
</script>
