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
  <bk-form-item
    ref="bkFormItem"
    class="jb-form-item"
    :class="classes"
    :error-display-type="errorDisplayType"
    :label="label"
    v-bind="$attrs"
    v-on="$listeners">
    <slot />
  </bk-form-item>
</template>
<script>
  export default {
    name: 'JbFormItem',
    inheritAttrs: false,
    props: {
      label: {
        type: String,
        default: '',
      },
      errorDisplayType: {
        type: String,
        default: 'normal',
      },
    },

    computed: {
      classes() {
        const classes = {
          'label-miss': !this.label,
        };
        return classes;
      },
    },

    methods: {
      clearValidator() {
        this.$refs.bkFormItem.clearValidator();
      },
    },
  };
</script>
<style lang='postcss'>
  .jb-form-item {
    &.label-miss {
      .bk-label {
        height: 0;
        min-height: 0;
        font-size: 0;
      }
    }

    .jb-form-item-content {
      flex: 1;
    }
  }
</style>
