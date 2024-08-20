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
  <div class="log-search-box">
    <compose-form-item>
      <bk-select
        v-model="searchModel"
        :clearable="false"
        style="width: 100px;">
        <bk-option
          id="log"
          :name="$t('history.搜索日志')" />
        <bk-option
          v-if="!isExecuteTargetContainer"
          id="ip"
          :name="$t('history.搜索 IP')" />
      </bk-select>
      <bk-input
        v-if="searchModel === 'log'"
        key="log"
        :disabled="data.isFile"
        right-icon="bk-icon icon-search"
        style="width: 292px;"
        :tippy-tips="data.isFile ? $t('history.分发文件步骤不支持日志搜索') : ''"
        :value="value.keyword"
        @keyup="handleLogSearch" />
      <bk-input
        v-if="searchModel === 'ip'"
        key="ip"
        right-icon="bk-icon icon-search"
        style="width: 292px;"
        :value="value.searchIp"
        @keyup="handleIPSearch" />
    </compose-form-item>
    <div
      v-if="searching"
      class="search-loading">
      <icon
        class="loading-flag"
        type="loading" />
    </div>
  </div>
</template>
<script setup lang="ts">
  import {
    computed,
    ref,
  } from 'vue';

  import ComposeFormItem from '@components/compose-form-item';

  const props = defineProps({
    value: {
      type: Object,
      default: () => ({
        keyword: '',
        searchIp: '',
      }),
    },
    data: {
      type: Object,
    },
    searching: {
      type: Boolean,
    },
  });

  const emits = defineEmits(['change']);

  const searchModel = ref('log');
  const isExecuteTargetContainer = computed(() => props.data.executeObjectType === 2);

  const handleLogSearch = (value, event) => {
    if (event.isComposing) {
      // 跳过输入法复合事件
      return;
    }

    // 输入框的值被清空直接触发搜索
    // enter键开始搜索
    if ((value === '' && value !== value.keyword)
      || event.keyCode === 13
      || event.type === 'click') {
      emits('change', {
        keyword: value,
        searchIp: '',
      });
    }
  };

  const handleIPSearch = (value, event) => {
    if (event.isComposing) {
      // 跳过输入法复合事件
      return;
    }

    // 输入框的值被清空直接触发搜索
    // enter键开始搜索
    if ((value === '' && value !== value.searchIp)
      || event.keyCode === 13) {
      emits('change', {
        keyword: '',
        searchIp: value,
      });
    }
  };
</script>
<style lang="postcss">
  .log-search-box {
    position: relative;
    display: flex;
    flex: 0 0 391px;
    background: #fff;

    .search-loading {
      position: absolute;
      top: 1px;
      right: 13px;
      bottom: 1px;
      display: flex;
      align-items: center;
      color: #c4c6cc;
      background: #fff;

      .loading-flag {
        animation: list-loading-ani 1s linear infinite;
      }
    }
  }
</style>
