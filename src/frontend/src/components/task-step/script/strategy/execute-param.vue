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
  <jb-form-item :label="$t('脚本参数')">
    <div class="muti-item">
      <jb-input
        :placeholder="$t('脚本执行时传入的参数，同脚本在终端执行时的传参格式，如：./test.sh xxxx xxx xxx')"
        :value="formData[paramField]"
        @change="handleParamChange" />
    </div>
  </jb-form-item>
</template>
<script>
  import JbInput from '@components/jb-input';

  export default {
    components: {
      JbInput,
    },
    props: {
      paramField: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        default: () => ({}),
      },
    },
    methods: {
      handleParamChange(value) {
        this.$emit('on-change', this.paramField, value);
      },
    },
  };
</script>
<style lang='postcss'>
  .muti-item {
    display: flex;
    width: 100%;

    .bk-form-control {
      flex: 1;
    }
  }
</style>
