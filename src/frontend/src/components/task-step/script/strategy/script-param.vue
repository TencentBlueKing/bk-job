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
  <jb-form-item
    :label="$t('脚本参数')"
    :property="paramField"
    :rules="rules">
    <div
      class="execute-script-params-block"
      :class="{
        'is-secure-field': formData[secureField]
      }">
      <jb-input
        :maxlength="65536"
        :placeholder="$t('脚本执行时传入的参数，同脚本在终端执行时的传参格式，如：./test.sh xxxx xxx xxx')"
        :type="paramType"
        :value="formData[paramField]"
        @change="handleParamChange">
        <template #number>
          64KB
        </template>
      </jb-input>
      <bk-checkbox
        class="muti-checkbox"
        :false-value="0"
        :true-value="1"
        :value="formData[secureField]"
        @change="handleSecureParam">
        {{ $t('敏感参数') }}
      </bk-checkbox>
    </div>
  </jb-form-item>
</template>
<script>
  import { getStringByteCount } from '@utils/assist';

  import JbInput from '@components/jb-input';

  import I18n from '@/i18n';


  export default {
    components: {
      JbInput,
    },
    props: {
      paramField: {
        type: String,
        required: true,
      },
      secureField: {
        type: String,
        required: true,
      },
      formData: {
        type: Object,
        default: () => ({}),
      },

    },
    computed: {
      paramType() {
        return this.formData[this.secureField] ? 'password' : 'text';
      },
    },
    created() {
      this.rules = [
        {
          validator: value => getStringByteCount(value) <= 65536,
          message: I18n.t('脚本参数最大输入为 64KB'),
          trigger: 'blur',
        },
      ];
    },
    methods: {
      handleParamChange(value) {
        this.$emit('on-change', this.paramField, value);
      },
      handleSecureParam(value) {
        this.$emit('on-change', this.secureField, value);
      },
    },
  };
</script>
<style lang='postcss'>
  .execute-script-params-block {
    display: flex;
    width: 100%;

    &.is-secure-field{
      .control-icon{
        display: none;
      }
    }

    .jb-input {
      flex: 1;
    }

    .muti-checkbox {
      display: flex;
      align-items: center;
      flex: 0 0 auto;
      margin-left: 10px;
    }
  }
</style>
