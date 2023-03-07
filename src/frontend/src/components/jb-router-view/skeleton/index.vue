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
  <transition name="skeleton">
    <div
      v-if="visiable"
      ref="wraper"
      class="jb-view-skeleton">
      <component
        :is="realCom"
        :max-width="width"
        primary-color="#EBECF3"
        secondary-color="#F6F7FB"
        :speed="2" />
    </div>
  </transition>
</template>
<script>
  import Dashboard from './components/dashboard';
  import ExecutePlan from './components/execute-plan';
  import GlobalSetUp from './components/global-set-up';
  import HistoryStep from './components/history-step';
  import List from './components/list';
  import Notify from './components/notify';
  import ScriptVersion from './components/script-version';
  import SetVariable from './components/set-variable';
  import TaskDetail from './components/task-detail';
  import TaskExecutiveDetail from './components/task-execute-detail';
  import TaskList from './components/task-list';
  import TaskStepDetail from './components/task-step-detail';

  const comMap = {
    list: List,
    taskList: TaskList,
    taskDetail: TaskDetail,
    taskExecutiveDetail: TaskExecutiveDetail,
    taskStepExecutiveDetail: TaskStepDetail,
    historyStep: HistoryStep,
    setVariable: SetVariable,
    executePlan: ExecutePlan,
    notify: Notify,
    globalSetUp: GlobalSetUp,
    dashboard: Dashboard,
    scriptVersion: ScriptVersion,
  };

  export default {
    name: '',
    props: {
      type: String,
      visiable: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        width: 0,
      };
    },
    computed: {
      realCom() {
        if (!Object.prototype.hasOwnProperty.call(comMap, this.type)) {
          return 'div';
        }
        return comMap[this.type];
      },
    },
    mounted() {
      this.init();
      window.addEventListener('resize', this.init);
      this.$once('hook:beforeDestroy', () => {
        window.removeEventListener('resize', this.init);
      });
    },
    methods: {
      init() {
        if (!this.$refs.wraper) {
          return;
        }
        this.width = this.$refs.wraper.getBoundingClientRect().width;
      },
    },
  };
</script>
<style lang='postcss' scoped>
  .viewport-full {
    .jb-view-skeleton {
      padding: 0;
    }
  }

  .jb-view-skeleton {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 20px;
    left: 0;
    z-index: 1001;
    width: 100%;
    min-height: calc(100vh - 104px);
    padding: 20px 24px 0;
    overflow: hidden;
    background: #f5f7fa;
    opacity: 100%;
    visibility: visible;
  }

  .skeleton-leave-active {
    transition: visibility 0.7s linear, opacity 0.5s linear;
  }

  .skeleton-leave-to {
    opacity: 0%;
    visibility: hidden;
  }
</style>
