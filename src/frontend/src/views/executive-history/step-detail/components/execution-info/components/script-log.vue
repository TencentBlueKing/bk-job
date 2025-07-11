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
    ref="contentBox"
    v-bkloading="{
      isLoading, opacity: .1,
    }"
    class="step-execute-script-log">
    <div
      class="log-wraper"
      @mouseup="handleMouseUp">
      <div
        v-once
        id="executeScriptLog"
        style="height: 100%;" />
    </div>
    <div
      v-if="taskExecuteDetail && isRunning"
      class="log-status">
      <div class="log-loading">
        {{ $t('history.执行中') }}
      </div>
    </div>
    <div class="log-action-box">
      <div
        v-bk-tooltips="backTopTips"
        class="action-item"
        @click="handleScrollTop">
        <icon type="up-to-top" />
      </div>
      <div
        v-bk-tooltips="backBottomTips"
        class="action-item action-bottom"
        @click="handleScrollBottom">
        <icon type="up-to-top" />
      </div>
    </div>
    <div
      v-if="isAiUseable"
      ref="aiExtendTool"
      class="ai-extend-tool"
      :style="aiExtendToolStyle"
      @click="handleSelectedAnalyzeError"
      @mousedown.stop>
      <img
        src="/static/images/ai.png"
        style="width: 16px">
    </div>
  </div>
</template>
<script>
  import ace from 'ace/ace';
  import _ from 'lodash';

  import AiService from '@service/ai';
  import TaskExecuteService from '@service/task-execute';

  import {
    getOffset,
  } from '@utils/assist';
  import eventBus from '@utils/event-bus';

  import I18n from '@/i18n';

  import mixins from '../../mixins';

  import 'ace/mode-text';
  import 'ace/theme-monokai';
  import 'ace/ext-searchbox';

  export default {
    mixins: [
      mixins,
    ],
    props: {
      name: String,
      taskInstanceId: {
        type: Number,
        required: true,
      },
      stepInstanceId: {
        type: Number,
        required: true,
      },
      taskExecuteDetail: {
        type: Object,
        required: true,
      },
      executeCount: {
        type: Number,
        required: true,
      },
      logFilter: {
        type: String,
        default: '',
      },
      fontSize: {
        type: Number,
        default: 12,
      },
      lineFeed: {
        type: Boolean,
        default: true,
      },
    },
    data() {
      return {
        // 日志loading，切换主机的时候才显示
        isLoading: true,
        // 是否执行中
        isRunning: false,
        // 自动动滚动到底部
        isWillAutoScroll: true,
        aiExtendToolStyle: {},
        isAiEnable: false,
      };
    },
    computed: {
      isAiUseable() {
        return this.isAiEnable && this.taskExecuteDetail && this.taskExecuteDetail.result !== 'fail';
      },
    },
    watch: {
      /**
       * @desc 查看的日志目标改变，重新获取日志
       *
       * 日志目标改变，重置页面操作的数据
       */
      name: {
        handler() {
          // 日志自动滚动
          this.isLoading = true;
          this.isWillAutoScroll = true;
          this.autoScrollTimeout();
          this.fetchLogContent();
        },
        immediate: true,
      },
      /**
       * @desc 字体大小改变时虚拟滚动重新计算
       */
      fontSize: {
        handler(fontSize) {
          this.editor.setFontSize(fontSize);
        },
      },
      lineFeed: {
        handler(lineFeed) {
          setTimeout(() => {
            this.editor && this.editor.setOptions({
              wrap: lineFeed ? 'free' : 'none',
            });
          });
        },
        immediate: true,
      },
    },
    created() {
      this.backTopTips = {
        content: I18n.t('history.回到顶部'),
        placements: [
          'top',
        ],
        theme: 'light',
      };
      this.backBottomTips = {
        content: I18n.t('history.前往底部'),
        placements: [
          'top',
        ],
        theme: 'light',
      };
      this.logContent = '';
      this.fetchAiConfig();
    },
    mounted() {
      this.initEditor();
      this.initAiHelper();
    },
    methods: {
      /**
       * @desc 获取脚本日志
       */
      fetchLogContent() {
        if (!this.taskExecuteDetail.executeObject) {
          this.isLoading = false;
          if (this.editor) {
            this.editor.setValue('');
            this.editor.clearSelection();
          }
          return;
        }

        TaskExecuteService.fetchLogContentOfHostId({
          taskInstanceId: this.taskInstanceId,
          stepInstanceId: this.stepInstanceId,
          executeObjectType: this.taskExecuteDetail.executeObject.type,
          executeObjectResourceId: this.taskExecuteDetail.executeObject.executeObjectResourceId,
          executeCount: this.executeCount,
          batch: this.taskExecuteDetail.batch,
        })
          .then(({
            finished,
            logContent,
          }) => {
            this.isRunning = !finished;
            this.logContent = _.trim(logContent || '', '\n');
            this.$nextTick(() => {
              this.editor.setValue(logContent);
              this.editor.clearSelection();
            });
            // 当前主机执行结束
            if (!finished) {
              this.$pollingQueueRun(this.fetchLogContent);
            }
          })
          .finally(() => {
            this.isLoading = false;
          });
      },
      fetchAiConfig() {
        AiService.fetchConfig()
          .then((data) => {
            this.isAiEnable = data.enabled;
          });
      },
      initEditor() {
        const editor = ace.edit('executeScriptLog');
        editor.getSession().setMode('ace/mode/text');
        editor.setTheme('ace/theme/monokai');
        editor.setOptions({
          fontSize: this.fontSize,
          wrapBehavioursEnabled: true,
          copyWithEmptySelection: true,
          useElasticTabstops: true,
          printMarginColumn: false,
          printMargin: 80,
          showPrintMargin: false,
          scrollPastEnd: 0.05,
          fixedWidthGutter: true,
        });
        editor.$blockScrolling = Infinity;
        editor.setReadOnly(true);
        const editorSession = editor.getSession();
        // 自动换行时不添加缩进
        editorSession.$indentedSoftWrap = false;
        editorSession.on('changeScrollTop', (scrollTop) => {
          const {
            height,
            maxHeight,
          } = editor.renderer.layerConfig;
          this.isWillAutoScroll = height + scrollTop + 30 >= maxHeight;
        });

        this.editor = editor;
        this.$once('hook:beforeDestroy', () => {
          editor.destroy();
          editor.container.remove();
        });
      },
      initAiHelper() {
        if (!this.isAiUseable) {
          return;
        }
        const handleHideAiExtendTool = () => {
          this.aiExtendToolStyle = {};
        };

        document.body.addEventListener('mousedown', handleHideAiExtendTool);
        this.$once('hook:beforeDestroy', () => {
          document.body.removeEventListener('mousedown', handleHideAiExtendTool);
        });
      },
      /**
       * @desc 外部调用
       */
      resize() {
        this.$nextTick(() => {
          this.editor.resize();
        });
      },
      /**
       * @desc 外部调用
       */
      getLog(contentSize) {
        if (this.logContent.length > contentSize) {
          return Promise.reject();
        }
        return Promise.resolve().then(() => {
          eventBus.$emit('ai:analyzeError', {
            taskInstanceId: this.taskInstanceId,
            stepInstanceId: this.stepInstanceId,
            executeObjectType: this.taskExecuteDetail.executeObject.type,
            executeObjectResourceId: this.taskExecuteDetail.executeObject.executeObjectResourceId,
            executeCount: this.executeCount,
            batch: this.taskExecuteDetail.batch,
            content: this.editor.getValue(),
          });
        });
      },
      /**
       * @desc 日志滚动定时器
       */
      autoScrollTimeout() {
        if (this.isWillAutoScroll && !this.isLoading) {
          this.handleScrollBottom();
          return;
        }
        setTimeout(() => {
          this.autoScrollTimer = this.autoScrollTimeout();
        }, 1000);
      },
      /**
       * @desc 回到日志顶部
       */
      handleScrollTop() {
        this.editor.scrollToLine(0);
      },
      /**
       * @desc 回到日志底部
       */
      handleScrollBottom() {
        this.isWillAutoScroll = true;
        this.editor.scrollToLine(Infinity);
      },
      handleMouseUp(event) {
        if (!this.isAiUseable) {
          return;
        }
        setTimeout(() => {
          if (!this.editor.getSelectedText()) {
            this.aiExtendToolStyle = {};
            return;
          }
          const {
            left: contentBoxLeft,
            top: contentBoxTop,
          } = getOffset(this.$refs.contentBox);

          const { pageX, pageY } = event;
          this.aiExtendToolStyle = {
            display: 'flex',
            top: `${Math.max(pageY - 40 - contentBoxTop, 8)}px`,
            left: `${Math.max(pageX + 4 - contentBoxLeft, 8)}px`,
          };
        });
      },
      handleSelectedAnalyzeError() {
        this.aiExtendToolStyle = {};
        eventBus.$emit('ai:analyzeError', {
          taskInstanceId: this.taskInstanceId,
          stepInstanceId: this.stepInstanceId,
          executeObjectType: this.taskExecuteDetail.executeObject.type,
          executeObjectResourceId: this.taskExecuteDetail.executeObject.executeObjectResourceId,
          executeCount: this.executeCount,
          batch: this.taskExecuteDetail.batch,
          content: this.editor.getSelectedText(),
        });
      },
    },
  };
</script>
<style lang='postcss'>
@keyframes script-execute-loading {
  0% {
    content: ".";
  }

  30% {
    content: "..";
  }

  60% {
    content: "...";
  }
}

.step-execute-script-log {
  position: relative;
  height: 100%;
  max-height: 100%;
  min-height: 100%;

  .log-wraper {
    position: absolute;
    top: 0;
    bottom: 20px;
    left: 0;
    width: 100%;
    padding-right: 20px;
    /* stylelint-disable selector-class-pattern */
    .ace_editor {
      overflow: unset;
      line-height: 1.6;
      color: #c4c6cc;
      background: #1d1d1d;

      .ace_gutter {
        padding-top: 4px;
        margin-bottom: -4px;
        color: #63656e;
        background: #292929;
      }

      .ace_scroller {
        padding-top: 4px;
        margin-bottom: -4px;
      }

      .ace_hidden-cursors .ace_cursor {
        opacity: 0% !important;
      }

      .ace_selected-word {
        background: rgb(135 139 145 / 25%);
      }

      .ace_scrollbar-v,
      .ace_scrollbar-h {
        &::-webkit-scrollbar-thumb {
          background-color: #3b3c42;
          border: 1px solid #63656e;
        }

        &::-webkit-scrollbar-corner {
          background-color: transparent;
        }
      }

      .ace_scrollbar-v {
        margin-right: -20px;

        &::-webkit-scrollbar {
          width: 14px;
        }
      }

      .ace_scrollbar-h {
        margin-bottom: -20px;

        &::-webkit-scrollbar {
          height: 14px;
        }
      }
    }
  }

  .log-status {
    position: absolute;
    bottom: 0;
    left: 0;
    padding-left: 20px;
    color: #fff;
  }

  .log-loading {
    &::after {
      display: inline-block;
      content: ".";
      animation: script-execute-loading 2s linear infinite;
    }
  }

  .keyword {
    color: #212124;
    background: #f0dc73;
  }

  .log-action-box {
    position: absolute;
    right: 20px;
    bottom: 20px;
    z-index: 10;
    display: flex;

    .action-item {
      position: relative;
      display: flex;
      width: 32px;
      height: 32px;
      margin-left: 12px;
      font-size: 18px;
      color: #000;
      cursor: pointer;
      background: rgb(255 255 255 / 80%);
      border-radius: 50%;
      align-items: center;
      justify-content: center;

      &:hover {
        background: rgb(255 255 255);
      }

      &.action-bottom {
        transform: rotateZ(180deg);
      }
    }
  }

  .ai-extend-tool{
    position: absolute;
    z-index: 1000;
    display: none;
    width: 32px;
    height: 32px;
    pointer-events:all;
    cursor: pointer;
    background: #3D3D3D;
    border: 1px solid #4F4F52;
    border-radius: 2px;
    box-shadow: 0 2px 10px 0 #000;;
    align-items: center;
    justify-content: center;
  }
}
</style>
