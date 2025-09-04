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
  <div class="host-dropdown-menu">
    <div
      ref="handleRef"
      @click="handleShow"
      @mouseenter="handleShow">
      <slot />
    </div>
    <div
      ref="popRef"
      style="min-width: 98px; padding: 5px 0; margin: -0.3rem -0.6rem; word-break: keep-all;">
      <slot name="menu" />
    </div>
  </div>
</template>
<script setup>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import {
    onBeforeUnmount,
    onMounted,
    ref,
  } from 'vue';

  const handleRef = ref();
  const popRef = ref();

  const isActive = ref(false);

  let tippyInstance;

  const handleShow = () => {
    tippyInstance.show();
  };

  onMounted(() => {
    tippyInstance = Tippy(handleRef.value, {
      arrow: false,
      allowHTML: false,
      content: popRef.value,
      placement: 'bottom-start',
      trigger: 'manual',
      theme: 'light',
      hideOnClick: true,
      animateFill: false,
      interactive: true,
      boundary: 'window',
      distance: 6,
      onShow() {
        isActive.value = true;
      },
      onHide() {
        isActive.value = false;
      },
    });
  });

  onBeforeUnmount(() => {
    if (tippyInstance) {
      tippyInstance.hide();
      tippyInstance.destroy();
      tippyInstance = null;
    }
  });
</script>
<style lang="postcss" scoped>
  .host-dropdown-menu {
    display: block;
  }
</style>
