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
  <resizeable-box
    :parent-width="parentWidth"
    :width="parentWidth / 2">
    <div
      v-bkloading="{ isLoading }"
      class="script-template-preview-template">
      <div class="preview-result">
        <span>{{ $t('scriptTemplate.渲染结果') }}</span>
        <div
          v-if="needRefresh"
          class="refresh-flag"
          @click="handleRefresh">
          <span class="dot">
            <icon type="refresh-2" />
          </span>
          <span style="color: #d74242;">{{ $t('scriptTemplate.有更新') }}</span>
        </div>
      </div>
      <div
        :id="editorId"
        class="preview-content" />
    </div>
  </resizeable-box>
</template>
<script>
  import { Base64 } from 'js-base64';
  import _ from 'lodash';
  import * as monaco from 'monaco-editor';

  import ScriptTemplateService from '@service/script-template';

  import {
    formatScriptTypeValue,
  } from '@utils/assist';

  import ResizeableBox from './resizeable-box';

  const LANG_MAP = {
    Shell: 'shell',
    Bat: 'bat',
    Perl: 'perl',
    Python: 'python',
    Powershell: 'powershell',
    SQL: 'sql',
  };

  export default {
    name: '',
    components: {
      ResizeableBox,
    },
    props: {
      parentWidth: {
        type: Number,
        default: window.innerWidth,
      },
      scriptContent: {
        type: String,
        default: '',
      },
      // 当前的脚本语言
      scriptLanguage: {
        type: String,
        required: true,
      },
    },
    data() {
      return {
        isLoading: false,
        needRefresh: false,
      };
    },
    watch: {
      scriptContent: {
        handler() {
          if (this.hasRendered) {
            this.needRefresh = true;
            return;
          }
          this.fetchRenderScript();
        },
        immediate: true,
      },
      scriptLanguage(lang) {
        this.fetchRenderScript();
        setTimeout(() => {
          this.setModelLanguage(LANG_MAP[lang]);
        });
      },
    },
    created() {
      this.hasRendered = false;
      this.editorId = `scriptTemplatePrevice${_.random(1, 1000)}_${Date.now()}`;
    },
    mounted() {
      this.initEditor();
    },
    methods: {
      /**
       * @desc 编辑器设置语言
       */
      setModelLanguage(lang) {
        const model = this.editor.getModel();
        monaco.editor.setModelLanguage(model, lang);
      },
      /**
       * @desc 清除选区
       */
      clearSelection() {
        // 将选区重置到第一行第一列（文档开头）
        this.editor.setSelection(new monaco.Range(1, 1, 1, 1));
      },
      /**
       * 初始化脚本编辑器
       */
      initEditor() {
        const $handler = document.querySelector(`#${this.editorId}`);
        const options = {
          // 字体与基础显示
          fontSize: 13,
          lineHeight: 18, // 可选的视觉调整项，用于配合字体大小

          // 自动补全与代码提示
          quickSuggestions: {
            comments: true,
            strings: true,
            other: true,
          },
          suggestOnTriggerCharacters: true,
          snippetSuggestions: 'inline', // 可选值: "top", "bottom", "inline", "none"

          // 关键配置：固定溢出小部件的位置
          fixedOverflowWidgets: true,

          // 编辑行为
          wordWrap: 'on', // 可选值: "off", "on", "bounded"

          // 滚动行为
          scrollBeyondLastLine: false,  // 为true则能让最后一行代码越过视图区域的顶部

          // 其他常用推荐配置
          automaticLayout: true, // 非常重要：使编辑器在容器尺寸变化时自动调整布局[5](@ref)
          minimap: { enabled: false }, // 默认关闭小地图，若需要可开启[4](@ref)
          lineNumbers: 'on', // 显示行号[4](@ref)
          scrollbar: { // 精细控制滚动条
            verticalScrollbarSize: 8,
            horizontalScrollbarSize: 8,
          },
          mouseWheelScrollSensitivity: 3, // 调整鼠标滚轮滚动灵敏度[6](@ref)
          tabSize: 2, // 设置Tab缩进长度[4](@ref)
          readOnly: true,  // 设置是否只读
        };
        // 创建编辑器实例
        const editor = monaco.editor.create($handler, {
          theme: 'vs-dark',
          ...options,
        });


        // 先保存 editor 在设置 value
        this.editor = editor;
        this.setModelLanguage(LANG_MAP[this.scriptLanguage]);
        // 先保存 editor 在设置 value
        this.editor.revealLineNearTop(0);
        this.editor.setValue('');
        this.clearSelection();

        this.$once('hook:beforeDestroy', () => {
          editor.dispose();
          editor.getContainerDomNode()?.remove();
        });
      },
      /**
       * @desc 预览脚本模板
       */
      fetchRenderScript() {
        this.isLoading = true;
        ScriptTemplateService.fetchRenderScript({
          scriptContent: this.scriptContent,
          scriptLanguage: formatScriptTypeValue(this.scriptLanguage),
        }).then((data) => {
          this.editor.setValue(Base64.decode(data.scriptContent));
          this.clearSelection();
        })
          .finally(() => {
            this.isLoading = false;
            this.hasRendered = true;
            this.needRefresh = false;
          });
      },
      /**
       * @desc 有更新，重新预览脚本模板
       */
      handleRefresh() {
        this.fetchRenderScript();
      },
    },
  };
</script>
<style lang='postcss'>
  .script-template-preview-template {
    position: relative;
    z-index: 0;
    height: 100%;
    background: #292929;

    .preview-content {
      height: calc(100% - 51px);
      /* stylelint-disable selector-class-pattern */
      &.monaco_editor {
        background: #292929;

        .margin-view-overlays {
          background: #292929;
        }
      }
    }

    .preview-result {
      display: flex;
      align-items: flex-end;
      padding: 16px 20px;
      font-size: 14px;
      line-height: 19px;
      color: #c4c6cc;

      .refresh-flag {
        display: flex;
        margin-left: 9px;
        font-size: 12px;
        cursor: pointer;

        .dot {
          position: relative;
          margin-right: 8px;
          font-size: 14px;

          &::after {
            position: absolute;
            top: 6px;
            right: -3px;
            width: 4px;
            height: 4px;
            background: #d74242;
            border-radius: 50%;
            content: "";
          }
        }
      }
    }
  }
</style>
