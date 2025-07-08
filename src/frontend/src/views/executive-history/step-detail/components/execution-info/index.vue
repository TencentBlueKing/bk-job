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
  <div
    ref="rootRef"
    class="step-execution-info-box"
    :style="rootStyles">
    <div class="tab-container">
      <render-title :task-execute-detail="taskExecuteDetail" />
      <div class="split-line" />
      <panel-tab
        v-model="activePanel"
        :is-file="isFile"
        :is-task="isTask" />
      <div class="extend-box">
        <ai-helper
          v-if="taskExecuteDetail && taskExecuteDetail.result === 'fail'"
          :get-log="handleGetLog"
          :task-instance-id="taskInstanceId" />
        <extend-download
          :active-panel="activePanel"
          :execute-object="taskExecuteDetail.executeObject"
          :step-instance-id="stepInstanceId"
          :task-instance-id="taskInstanceId" />
        <extend-font
          v-model="fontSize"
          :active-panel="activePanel" />
        <extend-fullscreen
          v-model="isFullscreen"
          @exit-fullscreen="handleExitFullscreen"
          @fullscreen="handleFullscreen" />
        <extend-line-feed
          v-model="isScriptLogLineFeed"
          :active-panel="activePanel" />
      </div>
    </div>
    <div
      class="tab-content-wraper"
      :style="logContentStyles">
      <component
        :is="renderCom"
        :key="activePanel"
        ref="logContentRef"
        :execute-count="executeCount"
        :font-size="fontSize"
        :line-feed="isScriptLogLineFeed"
        :mode="activePanel"
        :name="`${stepInstanceId}_${taskExecuteDetail.key}_${executeCount}`"
        :step-instance-id="stepInstanceId"
        :task-execute-detail="taskExecuteDetail"
        :task-instance-id="taskInstanceId"
        v-bind="$attrs"
        v-on="$listeners" />
    </div>
  </div>
</template>
<script setup>
  import {
    computed,
    ref,
    watch,
  } from 'vue';

  import I18n from '@/i18n';

  import AiHelper from './components/ai-helper.vue';
  import ExtendDownload from './components/extend-download.vue';
  import ExtendFont from './components/extend-font.vue';
  import ExtendFullscreen from './components/extend-fullscreen.vue';
  import ExtendLineFeed from './components/extend-line-feed.vue';
  import FileLog from './components/file-log/index.vue';
  import PanelTab from './components/panel-tab.vue';
  import RenderTitle from './components/render-title.vue';
  import ScriptLog from './components/script-log.vue';
  import VariableView from './components/variable-view';

  const props = defineProps({
    taskInstanceId: {
      type: Number,
    },
    stepInstanceId: {
      type: Number,
    },
    taskExecuteDetail: {
      type: Object,
      required: true,
    },
    executeCount: {
      type: Number,
      required: true,
    },
    isTask: {
      type: Boolean,
      default: false,
    },
    isFile: {
      type: Boolean,
      default: false, // 展示文件日志
    },
  });

  const rootRef = ref();
  const logContentRef = ref();

  const activePanel = ref('');
  const isFullscreen = ref(false);
  const fontSize = ref(12);
  const isScriptLogLineFeed = ref(false);

  const renderCom = computed(() => {
    const comMap = {
      scriptLog: ScriptLog,
      download: FileLog,
      upload: FileLog,
      variable: VariableView,
    };
    return comMap[activePanel.value];
  });

  const rootStyles = computed(() => {
    if (isFullscreen.value) {
      return {
        position: 'fixed',
        top: 0,
        right: 0,
        bottom: 0,
        left: 0,
        zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
      };
    }
    return {};
  });

  const logContentStyles = computed(() => {
    const lineHeightMap = {
      12: 20,
      13: 21,
      14: 22,
    };
    return {
      fontSize: `${fontSize.value}px`,
      lineHeight: `${lineHeightMap[fontSize.value]}px`,
    };
  });

  watch(() => props.isFile, () => {
    activePanel.value = props.isFile ? 'download' : 'scriptLog';
  }, {
    immediate: true,
  });

  const handleGetLog = () => logContentRef.value.getLog();

  let infoBoxParentNode;
  const handleFullscreen = () => {
    this.messageInfo(I18n.t('history.按 Esc 即可退出全屏模式'));
    infoBoxParentNode = rootRef.value.parentNode;
    document.body.appendChild(rootRef.value);
    logContentRef.value && logContentRef.value.resize();
  };
  /**
   * @desc 退出日志全屏
   */
  const handleExitFullscreen = () => {
    if (infoBoxParentNode) {
      infoBoxParentNode.appendChild(rootRef.value);
      infoBoxParentNode = null;
    }
    setTimeout(() => {
      logContentRef.value && logContentRef.value.resize();
    });
  };
</script>
<style lang='postcss'>
  .step-execution-info-box {
    position: relative;
    height: 100%;

    .tab-container {
      position: relative;
      z-index: 1;
      display: flex;
      font-size: 13px;
      line-height: 42px;
      color: #c4c6cc;
      background: #2f3033;
      box-shadow: 0 2px 4px 0 #000;
      align-items: center;

      .split-line {
        width: 1px;
        height: 20px;
        margin-right: -1px;
        background: #63656e;
      }

      .extend-box {
        display: flex;
        margin-left: auto;
        font-size: 18px;

        .extend-item {
          display: flex;
          height: 42px;
          padding: 0 10px;
          margin-left: 2px;
          cursor: pointer;
          align-items: center;
          justify-content: center;
        }
      }
    }

    .tab-content-wraper {
      flex: 1;
      height: calc(100% - 42px);
      background: #1d1d1d;
    }
  }
</style>
