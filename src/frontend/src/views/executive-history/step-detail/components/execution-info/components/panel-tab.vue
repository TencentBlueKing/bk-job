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
  <div class="execution-info-panel-tab">
    <div
      v-if="!isFile"
      class="item"
      :class="{ active: value === 'scriptLog' }"
      @click="handleTogglePanel('scriptLog')">
      {{ $t('history.执行日志') }}
    </div>
    <template v-if="isFile">
      <div
        class="item"
        :class="{ active: value === 'download' }"
        @click="handleTogglePanel('download')">
        {{ $t('history.下载信息') }}
      </div>
      <div
        class="item"
        :class="{ active: value === 'upload' }"
        @click="handleTogglePanel('upload')">
        {{ $t('history.上传源信息') }}
      </div>
    </template>
    <div
      v-if="isTask && !isFile"
      class="item"
      :class="{ active: value === 'variable' }"
      @click="handleTogglePanel('variable')">
      {{ $t('history.变量明细') }}
    </div>
  </div>
</template>
<script setup>
  defineProps({
    value: {
      type: String,
    },
    isTask: {
      type: Boolean,
    },
    isFile: {
      type: Boolean,
    },
  });

  const emits = defineEmits(['change', 'input']);

  const handleTogglePanel = (value) => {
    emits('change', value);
    emits('input', value);
  };
</script>
<style lang="postcss">
  .execution-info-panel-tab{
    display: flex;

    .item {
      position: relative;
      height: 42px;
      padding: 0 20px;
      cursor: pointer;
      user-select: none;

      &.active {
        z-index: 1;
        color: #fff;
        background: #212124;

        &::before {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 2px;
          background: #3a84ff;
          content: "";
        }
      }
    }
  }
</style>
