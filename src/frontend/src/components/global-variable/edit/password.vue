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
  <div :class="{ 'variable-value-error': isError }">
    <bk-input
      ref="input"
      v-bk-tooltips="descPopover"
      :readonly="readonly"
      type="password"
      :value="value"
      @change="handleChange" />
    <p
      v-if="isError"
      class="variable-error">
      {{ $t('该变量的值必填') }}
    </p>
  </div>
</template>
<script>
  export default {
    props: {
      data: {
        type: Object,
        required: true,
      },
      placement: {
        type: String,
        required: true,
      },
      readonly: {
        type: Boolean,
        default: false,
      },
      withValidate: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        showError: false,
        value: '',
      };
    },
    computed: {
      isError() {
        if (this.data.required !== 1) {
          return false;
        }
        return !this.value;
      },
      descPopover() {
        return {
          theme: 'light',
          extCls: 'variable-desc-tippy',
          trigger: 'click mouseenter',
          hideOnClick: false,
          content: this.data.description,
          disabled: !this.data.description,
        };
      },
    },
    watch: {
      placement(newVal) {
        if (this.data.description) {
          // eslint-disable-next-line no-underscore-dangle
          this.$refs.input.$el._tippy.set({ placement: newVal });
        }
      },
    },
    created() {
      this.init();
    },
    methods: {
      init() {
        this.value = this.data.defaultValue || this.data.value;
      },
      handleChange(value) {
        this.value = value.trim();
        window.changeFlag = true;
      },
      reset() {
        this.init();
      },
      validate() {
        const { type, id, name } = this.data;
        const data = {
          id,
          name,
          type,
          value: this.value,
          targetValue: {},
        };
        return new Promise((resolve, reject) => {
          if (this.withValidate && this.isError) {
            return reject(new Error('password error'));
          }
          resolve(data);
        });
      },
    },
  };
</script>
