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
  <div class="file-source-section">
    <jb-form-item
      class="batch-form-item"
      :label="$t('分批策略')"
      required>
      <div class="formula-tip">
        {{
          $t(
            '单传输目标上的并发任务数 = 单批次最大并发源主机/容器数 * 源单主机/容器最大并发文件数',
          )
        }}
      </div>
    </jb-form-item>
    <div class="file-source-form">
      <jb-form-item
        v-for="item in formItems"
        :key="item.field"
        :ref="item.field"
        :label="$t(item.label)"
        :property="item.field"
        required
        :rules="item.rules">
        <bk-input
          class="form-item-content"
          :max="item.max"
          :min="1"
          type="number"
          :value="formData[item.field]"
          @change="handleChange(item.field, $event)" />
      </jb-form-item>
    </div>
  </div>
</template>

<script setup>
  import { computed } from 'vue';

  import I18n from '@/i18n';

  const props = defineProps({
    maxExecuteObjectNumField: {
      type: String,
      required: true,
    },
    maxFileNumField: {
      type: String,
      required: true,
    },
    formData: {
      type: Object,
      required: true,
    },
    enabledField: {
      type: String,
      default: '',
    },
    typeField: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['on-change']);

  /**
   * 校验是否开启分批策略
   */
  const enableValidate = computed(() => {
    return (
      props.formData[props.enabledField]
      && props.formData[props.typeField] === 2
    );
  });

  /**
   * 创建校验规则
   */
  const createRule = (message) => {
    return computed(() => {
      if (!enableValidate.value) {
        return [];
      }

      return [
        {
          validator: value => Number(value) > 0,
          message: I18n.t(message),
          trigger: 'blur',
        },
      ];
    });
  };

  const maxExecuteObjectNumRule = createRule(
    '单批次最大并发源主机/容器数必填',
  );
  const maxFileNumRule = createRule(
    '源单主机/容器最大并发文件数必填',
  );

  const formItems = computed(() => [
    {
      field: props.maxExecuteObjectNumField,
      label: '单批次最大并发源主机/容器数',
      rules: maxExecuteObjectNumRule.value,
      max: 10000,
    },
    {
      field: props.maxFileNumField,
      label: '源单主机/容器最大并发文件数',
      rules: maxFileNumRule.value,
      max: 2000,
    },
  ]);

  const handleChange = (field, value) => {
    if(Number.isNaN(+value)) return;
    emit(
      'on-change',
      field,
      +value,
    );
  };
</script>

<style lang="postcss" scoped>
.file-source-section {
  width: 100%;

  .batch-form-item.bk-form-item {
    margin-bottom: 12px;

    .formula-tip {
      font-size: 12px;
      color: #979ba5;
    }
  }

  .file-source-form {
    background-color: #fafafa;
    padding: 16px 12px 16px 0;
    border-radius: 2px;
    margin-bottom: 12px;

    .jb-form-item {
      margin-bottom: 16px;

      .bk-label .bk-label-text {
        padding-left: 12px;
      }

      &:last-child {
        margin-bottom: 0;
      }
    }
  }
}
</style>
