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
  <component
    :is="com"
    class="guide-page" />
</template>
<script>
  import EventBus from '@utils/event-bus';

  import CrontabList from './corntab-list';
  import ScriptList from './script-list';
  import TaskList from './task-list';

  const comMap = {
    taskList: TaskList,
    scriptList: ScriptList,
    cronList: CrontabList,
  };

  export default {
    name: '',
    data() {
      return {
        isEmpty: false,
        page: '',
      };
    },
    computed: {
      com() {
        if (!this.isEmpty) {
          return '';
        }
        return Object.prototype.hasOwnProperty.call(comMap, this.page) ? comMap[this.page] : '';
      },
    },
    watch: {
      $route: {
        handler(route) {
          this.page = route.name;
          this.isEmpty = false;
        },
        immediate: true,
      },
    },
    created() {
      EventBus.$on('page-empty', () => {
        this.isEmpty = true;
      });
    },
  };
</script>
<style lang="postcss">
  .guide-page {
    position: absolute;
    top: 0;
    right: 0;
    left: 0;
    z-index: 1000;
    min-height: calc(100vh - 104px);
    background: #fff;
  }
</style>
