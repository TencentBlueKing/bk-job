<!--
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
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
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
-->

<template>
  <div class="jb-execute-account-wrapper">
    <div
      v-if="accountVar"
      class="jb-execute-account"
      :class="[themeClass, { 'jb-execute-account-highlight': highlight }]"
      @click="handleToggle">
      <div class="jb-execute-account-flag">
        <icon type="string" />
      </div>
      <div class="jb-execute-account-name">
        {{ accountVar }}
      </div>
    </div>
    <div
      v-else
      class="jb-execute-account-text"
      :class="[{ 'text-value-highlight': highlight }]">
      {{ displayText }}
    </div>
  </div>
</template>

<script>
  export default {
    name: 'RenderExecuteAccount',
  };
</script>

<script setup>
  import { computed, getCurrentInstance, ref, watch } from 'vue';

  import I18n from '@/i18n';

  const { proxy } = getCurrentInstance();

  const props = defineProps({
    // 全局变量名
    accountVar: {
      type: [String, Number],
      default: '',
    },
    // 账号 id
    accountId: {
      type: [String, Number],
      default: '',
    },
    // 空值展示
    emptyText: {
      type: String,
      default: '--',
    },
    // 全局变量列表（含 accountVar 对应的账号类型变量）
    allVariables: {
      type: Array,
      default: () => [],
    },
    // 账号列表
    accountList: {
      type: Array,
      default: () => [],
    },
    // 视觉风格：blue（详情/查看页） / gray（同步对比页）
    theme: {
      type: String,
      default: 'blue',
      validator: val => ['blue', 'gray'].includes(val),
    },
    // 差异高亮态（同步对比页 diff 命中时）
    highlight: {
      type: Boolean,
      default: false,
    },
  });

  const themeClass = computed(() => `jb-execute-account-theme-${props.theme}`);

  // 账号 id → 别名（账号列表由外部传入，组件内不发起请求）
  const resolveAliasById = (id) => {
    if (id === '' || id == null) return '';
    const account = props.accountList.find(item => item.id === Number(id));
    return account ? account.alias : '';
  };

  // 非变量场景：accountId → 别名；列表为空或解析失败展示 emptyText
  const displayText = ref(props.emptyText);
  const resolveDisplayText = () => {
    if (props.accountId !== '' && props.accountId != null) {
      displayText.value = resolveAliasById(props.accountId) || props.emptyText;
    } else {
      displayText.value = props.emptyText;
    }
  };
  resolveDisplayText();
  // 账号列表或账号 id 变化（外部异步传入）时重新解析
  watch(() => props.accountList, resolveDisplayText);
  watch(() => props.accountId, resolveDisplayText);

  // 点击展开后由 accountVar 解析出的账号别名
  const resolvedAlias = ref('');

  // 变量名（accountVar）→ 实际值（账号 id）→ 别名
  const resolveVariableAlias = () => {
    const variable = props.allVariables.find(item => item.name === props.accountVar);
    if (!variable) {
      resolvedAlias.value = props.emptyText;
      return;
    }
    const variableAccountId = variable.defaultValue ?? variable.value;
    resolvedAlias.value = resolveAliasById(variableAccountId) || props.emptyText;
  };

  const handleToggle = () => {
    if (!props.accountVar) return;
    resolveVariableAlias();
    proxy.$bkInfo({
      title: resolvedAlias.value || props.emptyText,
      okText: I18n.t('关闭'),
    });
  };
</script>

<style lang="postcss" scoped>
  .jb-execute-account-wrapper {
    display: flex;
    max-width: 100%;
    overflow: hidden;
    align-items: center;
  }

  .jb-execute-account {
    display: flex;
    line-height: 1;
    cursor: pointer;
    flex: 0 0 auto;

    &.jb-execute-account-highlight {
      padding: 3px 5px;
      background: #fddfcb;
      outline: 1px solid #f9c9a9;

      .jb-execute-account-flag {
        background: #f0c581;
      }

      .jb-execute-account-name {
        border-color: #f0c581;
      }
    }

    .jb-execute-account-flag {
      display: flex;
      width: 24px;
      height: 24px;
      font-size: 13px;
      color: #fff;
      border-bottom-left-radius: 2px;
      border-top-left-radius: 2px;
      flex: 0 0 24px;
      align-items: center;
      justify-content: center;
    }

    .jb-execute-account-name {
      display: flex;
      padding: 0 10px;
      white-space: nowrap;
      border: 1px solid #dcdee5;
      border-left: none;
      border-top-right-radius: 2px;
      border-bottom-right-radius: 2px;
      align-items: center;
    }
  }

  .jb-execute-account-text {
    display: flex;
    overflow: hidden;
    color: #63656e;
    text-overflow: ellipsis;
    white-space: nowrap;
    align-items: center;
  }

  .text-value-highlight {
    padding: 0 3px;
    background: #fddfcb;
  }

  .jb-execute-account-theme-blue {
    .jb-execute-account-flag {
      background: #3a84ff;
    }
  }

  .jb-execute-account-theme-gray {
    overflow: hidden;

    .jb-execute-account-flag {
      background: #c4c6cc;
    }

    .jb-execute-account-name {
      font-size: 12px;
      color: #63656e;
      background: #fff;
    }
  }

</style>
