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
  <div
    ref="aceEditor"
    class="jd-ace-editor"
    :style="{ height: `${height}px` }">
    <div
      ref="contentWrapper"
      v-bkloading="{ isLoading: isLoading, opacity: 0.2 }"
      class="jb-ace-content"
      :class="{ readonly }"
      :style="boxStyle">
      <div
        v-if="showTabHeader"
        class="jb-ace-title"
        :style="{ height: `${tabHeight}px` }">
        <div
          v-for="(val, key) in tabList"
          :key="val"
          class="jb-ace-mode-item"
          :class="{ 'active': currentLang === key }"
          @click="handleLangChange(key)">
          {{ key }}
        </div>
      </div>
      <div class="jb-ace-main">
        <div class="ace-edit-content">
          <div
            :id="selfId"
            :style="editorStyle" />
        </div>
        <div class="right-side-panel">
          <slot name="side" />
        </div>
      </div>
      <div
        class="jb-ace-action"
        :style="{ height: `${tabHeight}px` }">
        <slot name="action" />
        <template v-if="!readonly && !isFullScreen">
          <icon
            v-bk-tooltips="$t('上传脚本')"
            type="upload"
            @click="handleUploadScript" />
          <icon
            v-bk-tooltips="$t('历史缓存')"
            type="history"
            @click.stop="handleShowHistory" />
        </template>
        <icon
          v-if="!isFullScreen"
          v-bk-tooltips="$t('全屏')"
          type="full-screen"
          @click="handleFullScreen" />
        <icon
          v-if="isFullScreen"
          v-bk-tooltips="$t('还原')"
          type="un-full-screen"
          @click="handleExitFullScreen" />
      </div>
      <div
        v-if="isShowHistoryPanel"
        ref="historyPanel"
        class="jb-ace-history-panel"
        @click.stop="">
        <div class="panel-header">
          <div>{{ $t('历史缓存') }}</div>
          <div
            class="save-btn"
            @click.stop="handleSaveHistory">
            {{ $t('手动保存') }}
          </div>
        </div>
        <div
          v-if="historyList.length > 0"
          style="max-height: 250px;">
          <scroll-faker>
            <div class="panel-body">
              <div
                v-for="item in historyList"
                :key="item.name"
                class="item">
                <div
                  v-bk-overflow-tips
                  class="history-name">
                  {{ item.name }}
                </div>
                <div
                  class="history-action"
                  @click="handleChangeValueFromHistory(item)">
                  {{ $t('载入') }}
                </div>
              </div>
            </div>
          </scroll-faker>
        </div>
        <empty
          v-else
          class="history-empty"
          :width="100" />
      </div>
      <input
        ref="upload"
        style="position: absolute; width: 0; height: 0;"
        type="file"
        @change="handleStartUpload">
    </div>
  </div>
</template>
<script>
  import ace from 'ace/ace';
  import { Base64 } from 'js-base64';
  import _ from 'lodash';

  import PublicScriptService from '@service/public-script-manage';
  import ScriptService from '@service/script-manage';
  import ScriptTemplateService from '@service/script-template';
  import UserService from '@service/user';

  import {
    escapeHTML,
    formatScriptTypeValue,
    prettyDateTimeFormat,
  } from '@utils/assist';

  import Empty from '@components/empty';
  import ScrollFaker from '@components/scroll-faker';

  import DefaultScript from './default-script';

  import 'ace/mode-sh';
  import 'ace/snippets/sh';
  import 'ace/mode-batchfile';
  import 'ace/snippets/batchfile';
  import 'ace/mode-perl';
  import 'ace/snippets/perl';
  import 'ace/mode-python';
  import 'ace/snippets/python';
  import 'ace/mode-powershell';
  import 'ace/snippets/powershell';
  import 'ace/mode-sql';
  import 'ace/snippets/sql';
  import 'ace/theme-monokai';
  import 'ace/ext-error_marker';
  import 'ace/ext-language_tools';
  import 'ace/ext-keybinding_menu';
  import 'ace/ext-elastic_tabstops_lite';
  import I18n from '@/i18n';

  export const builtInScript = Object.keys(DefaultScript).reduce((result, item) => {
    result[item] = Base64.encode(DefaultScript[item]);
    return result;
  }, {});

  const languageTools = ace.require('ace/ext/language_tools');

  const TAB_HEIGHT = 40;
  const LANG_MAP = {
    Shell: 'sh',
    Bat: 'batchfile',
    Perl: 'perl',
    Python: 'python',
    Powershell: 'powershell',
    SQL: 'sql',
  };
  const LOCAL_STORAGE_KEY = 'ace_editor_history';

  const HTMLEncode = (value) => {
    const temp = document.createElement('textarea');
    temp.value = value;
    return temp.value;
  };

  export default {
    name: 'AceEditor',
    components: {
      ScrollFaker,
      Empty,
    },
    inheritAttrs: false,
    props: {
      // 脚本内容
      value: {
        type: String,
      },
      height: {
        type: Number,
        default: 480,
      },
      // 只读模式
      readonly: {
        type: Boolean,
        default: false,
      },
      readonlyTips: {
        type: String,
        default: I18n.t('只读模式不支持编辑'),
      },
      // 当前的脚本语言
      lang: {
        type: String,
        required: true,
      },
      // 可支持切换的脚本类型（array：显示tab; string: 不显示tab）
      options: {
        type: [
          String,
          Array,
        ],
        default: () => Object.keys(LANG_MAP),
      },
      // 默认脚本是否显示用户自定义内容，默认为 true
      customEnable: {
        type: Boolean,
        default: true,
      },
      // 自定义全局变量
      constants: {
        type: Array,
        default: () => [],
      },
      // 切换脚本语言前的确认动作
      beforeLangChange: {
        type: Function,
        default: () => Promise.resolve(),
      },
    },
    data() {
      return {
        isLoading: false,
        content: '',
        currentLang: this.lang,
        isFullScreen: false,
        isShowHistoryPanel: false,
        tabHeight: TAB_HEIGHT,
        historyList: [],
        currentUser: {},
      };
    },
    computed: {
      /**
       * @desc 脚本编辑器块的样式
       * @returns {Object}
       */
      boxStyle() {
        const style = {
          position: 'absolute',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
        };
        if (this.isFullScreen) {
          style.position = 'fixed';
          style.zIndex = window.__bk_zIndex_manager.nextZIndex(); // eslint-disable-line no-underscore-dangle
          style.height = '100vh';
        }
        return style;
      },
      /**
       * @desc 脚本输入区的样式
       * @returns {Object}
       */
      editorStyle() {
        const tabHeight = this.showTabHeader ? TAB_HEIGHT : 0;
        return {
          height: this.isFullScreen ? `calc(100vh - ${tabHeight}px)` : `${this.height - tabHeight}px`,
        };
      },
      /**
       * @desc 是否显示脚本类型切换 TAB, 当options 配置 String 时不显示
       * @returns {Boolean}
       */
      showTabHeader() {
        return typeof this.options !== 'string';
      },
      /**
       * @desc 显示类型显示列表
       * @returns {Object}
       */
      tabList() {
        if (!Array.isArray(this.options)) {
          return [];
        }
        return this.options.reduce((res, item) => {
          if (Object.prototype.hasOwnProperty.call(LANG_MAP, item)) {
            res[item] = LANG_MAP[item];
          }
          return res;
        }, {});
      },
      /**
       * @desc 脚本编辑器语言模式
       * @returns {String}
       */
      mode() {
        return `ace/mode/${LANG_MAP[this.currentLang]}`;
      },
    },
    watch: {
      value: {
        handler(value) {
          this.editor.getSession().setAnnotations([]);
          // 只读模式没有默认值，直接使用输入值
          if (this.readonly) {
            this.editor.setValue(Base64.decode(value));
            this.editor.clearSelection();
            this.syntaxCheck(value);
            return;
          }
          // 外部传入空置直接清空编辑器
          if (value === '' && this.content !== '') {
            this.editor.setValue('');
            return;
          }
          const parseValue = Base64.decode(value);
          // 避免编辑造成的重复更新
          if (this.content !== parseValue) {
            this.editor.setValue(parseValue);
            this.editor.clearSelection();
          }
        },
      },
      lang(newLang) {
        if (this.currentLang !== newLang) {
          this.currentLang = newLang;
          setTimeout(() => {
            this.editor.getSession().setMode(this.mode);
          });
        }
      },
      readonly(readonly) {
        this.editor.setReadOnly(readonly);
      },
      constants: {
        handler() {

        },
        immediate: true,
      },
    },
    created() {
      this.selfId = `ace_editor_${_.random(1, 1000)}_${Date.now()}`;
      this.valueMemo = {};
      this.hasChanged = false;
      this.historyEnable = false;
      this.historyTimer = '';
      this.fetchUserInfo();
      this.fetchTemplate();
      this.syntaxCheck = _.debounce((content) => {
        ScriptService.getScriptValidation({
          content,
          scriptType: formatScriptTypeValue(this.currentLang),
        }).then((data) => {
          // 高危语句报错状态需要全局保存
          this.$store.commit('setScriptCheckError', _.some(data, _ => _.isDangerous));
          this.editor.getSession().setAnnotations(data);
        });
      }, 300);

      // 自定义语法提示
      this.completer = {
        getCompletions: (editor, session, pos, prefix, callback) => {
          const keywords = this.constants.map(item => ({
            name: item.name,
            value: `$\{${item.name}}`,
            caption: item.name,
            meta: 'Global Variable',
            type: item.typeDescription,
            description: item.description,
            score: 1000, // 让自定义全局变量排在最上面
          }));
          callback(null, keywords);
        },
        getDocTooltip(item) {
          if (item.meta === 'Global Variable' && item.description) {
            item.docHTML = [
              '<b>description</b>',
              '<hr />',
              escapeHTML(item.description),
            ].join('');
          }
        },
      };
    },
    beforeDestroy() {
      this.handleExitFullScreen();
      this.$store.commit('setScriptCheckError', null);
    },
    mounted() {
      this.initEditor();
      languageTools.addCompleter(this.completer);
      document.body.addEventListener('click', this.handleHideHistory);
      document.body.addEventListener('keyup', this.handleExitByESC);
      this.$once('hook:beforeDestroy', () => {
        clearTimeout(this.historyTimer);
        if (this.isChange) {
          this.pushLocalStorage();
        }
        _.remove(this.editor.completers, _ => _ === this.completer);
        document.body.removeEventListener('click', this.handleHideHistory);
        document.body.removeEventListener('keyup', this.handleExitByESC);
      });
    },
    methods: {
      /**
       * @desc 获取登陆用户信息
       */
      fetchUserInfo() {
        UserService.fetchUserInfo()
          .then((data) => {
            this.currentUser = Object.freeze(data);
          });
      },
      /**
       * @desc 获取默认脚本
       */
      fetchTemplate() {
        this.isLoading = true;
        const handlePromise = this.customEnable ? ScriptTemplateService.fetchTemplate() : Promise.resolve([]);
        handlePromise.then((data) => {
          const customScriptMap = data.reduce((result, item) => {
            result[formatScriptTypeValue(item.scriptLanguage)] = Base64.decode(item.scriptContent);
            return result;
          }, {});
          this.defaultScriptMap = Object.assign({}, DefaultScript, customScriptMap);

          // 只读或有传入值默认脚本使用prop.value
          // 其它情况使用脚本编辑器提供的默认值
          this.content = this.readonly || this.value
            ? Base64.decode(this.value || '')
            : this.defaultScriptMap[this.lang];
          this.editor.setValue(this.content);
          this.editor.scrollToLine(Infinity);
          this.editor.clearSelection();
        })
          .finally(() => {
            this.isLoading = false;
          });
      },
      /**
       * @desc 初始化脚本编辑器
       */
      initEditor() {
        const editor = ace.edit(this.selfId);
        editor.getSession().setMode(this.mode);
        editor.setOptions({
          fontSize: 13,
          enableBasicAutocompletion: true,
          enableLiveAutocompletion: true,
          enableSnippets: true,
          wrapBehavioursEnabled: true,
          autoScrollEditorIntoView: true,
          copyWithEmptySelection: true,
          useElasticTabstops: true,
          printMarginColumn: true,
          printMargin: 80,
          scrollPastEnd: 0.2,
        });
        editor.setTheme('ace/theme/monokai');
        editor.setShowPrintMargin(false);
        editor.$blockScrolling = Infinity;
        editor.setReadOnly(this.readonly);

        editor.on('change', () => {
          this.content = editor.getValue();
          const content = Base64.encode(this.content);
          if (this.content && !this.readonly) {
            this.syntaxCheck(content);
          }
          this.editor.getSession().setAnnotations([]);
          if (this.historyEnable) {
            this.hasChanged = true;
          }
          this.$emit('input', content);
          this.$emit('change', content);
        });
        editor.on('focus', () => {
          this.historyEnable = true;
        });
        editor.on('paste', (event) => {
          event.text = HTMLEncode(event.text);
        });
        // 先保存 editor 在设置 value
        this.editor = editor;

        this.$once('hook:beforeDestroy', () => {
          editor.destroy();
          editor.container.remove();
        });

        this.watchEditAction();

        const $handler = document.querySelector(`#${this.selfId}`);
        $handler.addEventListener('keydown', this.handleReadonlyWarning);
        this.$once('hook:beforeDestroy', () => {
          $handler.removeEventListener('keydown', this.handleReadonlyWarning);
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
       * @desc 外部调用-设置脚本编辑器内容
       * @param {String} 经过 base64 编码的脚本内容
       */
      setValue(value) {
        this.editor.setValue(Base64.decode(value));
        this.editor.clearSelection();
        this.editor.scrollToLine(Infinity);
      },
      /**
       * @desc 外部调用-重置脚本编辑内容使用默认脚本
       */
      resetValue() {
        this.editor.setValue(this.defaultScriptMap[this.lang]);
        this.editor.clearSelection();
        this.editor.scrollToLine(Infinity);
      },
      /**
       * @desc 监听脚本的编辑状态
       *
       * 每分钟自动缓存一次
       */
      watchEditAction() {
        if (this.readonly) {
          return;
        }
        this.historyTimer = setTimeout(() => {
          if (this.historyEnable && this.hasChanged) {
            this.pushLocalStorage();
          }
          this.hasChanged = false;
          this.watchEditAction();
        }, 60000);
      },
      /**
       * @desc 缓存脚本内容
       * @param {String} type 缓存类型（自动缓存、手动换粗）
       */
      pushLocalStorage(type = I18n.t('自动保存')) {
        // 当前脚本内容为空不缓存
        if (!this.value) {
          return;
        }
        // eslint-disable-next-line max-len
        const newCacheKey = `${type}_${window.PROJECT_CONFIG.SCOPE_TYPE}_${window.PROJECT_CONFIG.SCOPE_ID}_${this.currentUser.username}_${prettyDateTimeFormat(Date.now())}`;
        let historyList = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY));
        if (!_.isArray(historyList)) {
          historyList = [];
        }
        if (historyList.length > 0) {
          // 最新缓存内容和上一次缓存内容相同不缓存
          if (historyList[0].content === this.value) {
            return;
          }
        }
        historyList.unshift({
          name: newCacheKey,
          content: this.value,
          lang: this.lang,
        });
        localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(historyList.slice(0, 25)));
      },
      /**
       * @desc readonly模式下键盘操作提示
       * @param {Object} event keydown事件
       */
      handleReadonlyWarning(event) {
        if (!this.readonly) {
          return;
        }
        const { target } = event;
        // 脚本编辑器获得焦点的状态
        if (target.type !== 'textarea') {
          return;
        }

        if ([
          'Escape',
          'Meta',
          'ShiftLeft',
          'ShiftRight',
          'ControlLeft',
          'ControlRight',
          'AltLeft',
          'AltRight',
        ].includes(event.code)) {
          return;
        }
        if ((event.metaKey || event.ctrlKey)
          && !['KeyV', 'KeyX'].includes(event.code)) {
          return;
        }
        this.messageWarn(this.readonlyTips);
      },
      /**
       * @desc 脚本语言切换
       * @param {String} newLang 脚本语言
       */
      handleLangChange(newLang) {
        if (this.readonly || this.currentLang === newLang) {
          return;
        }
        const result = this.beforeLangChange();
        Promise.resolve()
          .then(() => {
            if (typeof result.then === 'function') {
              return result;
            }
            return result ? Promise.resolve() : Promise.reject(new Error('error'));
          })
          .then(() => {
            this.$emit('on-mode-change', newLang);
            // 切换语言时缓存上一语言的脚本内容
            this.valueMemo[this.currentLang] = this.content;

            this.currentLang = newLang;
            this.editor.getSession().setMode(this.mode);
            if (Object.prototype.hasOwnProperty.call(this.valueMemo, this.currentLang)) {
              // 使用新脚本语言的上一次缓存内容
              this.editor.setValue(this.valueMemo[this.currentLang]);
            } else {
              // 使用新脚本语言的默认内容
              this.editor.setValue(this.defaultScriptMap[this.currentLang]);
            }
            this.editor.clearSelection();
          });
      },
      /**
       * @desc 显示脚本缓存面板
       */
      handleShowHistory() {
        const historyList = JSON.parse(localStorage.getItem(LOCAL_STORAGE_KEY));
        if (_.isArray(historyList)) {
          this.historyList = Object.freeze(historyList);
        } else {
          this.historyList = [];
        }
        this.isShowHistoryPanel = true;
      },
      /**
       * @desc 隐藏脚本缓存面板
       */
      handleHideHistory() {
        this.isShowHistoryPanel = false;
      },
      /**
       * @desc 使用缓存的脚本内容
       * @param {Object} payload 缓存的脚本信息
       */
      handleChangeValueFromHistory(payload) {
        // 切换脚本类型tab
        this.$emit('on-mode-change', payload.lang);
        // 更新脚本内容
        this.editor.setValue(Base64.decode(payload.content));
        this.editor.clearSelection();
        this.handleHideHistory();
      },
      /**
       * @desc 手动缓存脚本内容
       */
      handleSaveHistory: _.debounce(function () {
        this.pushLocalStorage(I18n.t('手动保存'));
        this.handleShowHistory();
      }, 300),
      /**
       * @desc 触发脚本上传
       */
      handleUploadScript() {
        this.$refs.upload.click();
      },
      /**
       * @desc 开始上传
       * @param {Object} event input文件选中事件
       */
      handleStartUpload(event) {
        const { files } = event.target;
        if (!files.length) {
          return;
        }
        const fileName = files[0].name;
        const fileSuffixes = fileName.substr(fileName.lastIndexOf('.') + 1);
        const langMap = {
          sh: 'Shell',
          bat: 'Bat',
          pl: 'Perl',
          py: 'Python',
          ps1: 'powershell',
          sql: 'SQL',
        };
        if (!langMap[fileSuffixes]) {
          this.$bkMessage({
            theme: 'error',
            message: I18n.t('脚本类型不支持'),
          });
          return;
        }
        this.isLoading = true;
        const params = new FormData();
        params.append('script', files[0]);
        PublicScriptService.getUploadContent(params)
          .then((data) => {
            this.handleLangChange(langMap[fileSuffixes]);
            this.editor.setValue(Base64.decode(data.content));
          })
          .finally(() => {
            this.isLoading = false;
          });
        this.$refs.upload.value = '';
      },
      /**
       * @desc 切换编辑的全屏状态
       *
       * 全屏时需要把dom移动到body下面
       */
      handleFullScreen() {
        this.isFullScreen = true;
        this.messageInfo(I18n.t('按 Esc 即可退出全屏模式'));
        document.body.appendChild(this.$refs.contentWrapper);
        this.$nextTick(() => {
          this.editor.resize();
        });
      },
      /**
       * @desc 退出编辑的全屏状态
       *
       * 退出全屏时需要要把dom还原到原有位置
       */
      handleExitFullScreen() {
        this.isFullScreen = false;
        this.$refs.aceEditor.appendChild(this.$refs.contentWrapper);
        this.$nextTick(() => {
          this.editor.resize();
        });
      },
      /**
       * @desc esc快捷键退出编辑的全屏状态
       */
      handleExitByESC(event) {
        if (event.code !== 'Escape' || !this.isFullScreen) {
          return;
        }
        this.handleExitFullScreen();
      },
    },
  };
</script>
<style lang='postcss'>
  .jd-ace-editor {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    /* stylelint-disable selector-class-pattern */
    .ace_editor {
      padding-right: 14px;
      overflow: unset;
      font-family: Menlo, Monaco, Consolas, Courier, monospace;

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
        &::-webkit-scrollbar {
          width: 14px;
        }
      }

      .ace_scrollbar-h {
        &::-webkit-scrollbar {
          height: 14px;
        }
      }

      .ace_gutter-cell {
        &.ace_info,
        &.ace_warning,
        &.ace_error {
          background-size: 12px;
          background-position-x: 4px;
        }

        &.ace_info {
          background-image: url("/static/images/ace-editor/info.png");
        }

        &.ace_warning {
          background-image: url("/static/images/ace-editor/warning.png");
        }

        &.ace_error {
          background-image: url("/static/images/ace-editor/error.png");
        }
      }
    }

    .readonly {
      .jb-ace-title,
      .ace_gutter,
      .ace_content {
        filter: grayscale(0%) brightness(80%) saturate(70%) opacity(95%);
      }

      .jb-ace-mode-item {
        cursor: default;
      }
    }
  }

  .jb-ace-title {
    display: flex;
    font-size: 14px;
    color: #fff;
    background: #202024;

    .jb-ace-mode-item {
      display: flex;
      padding: 0 22px;
      color: #979ba5;
      cursor: pointer;
      border-top: 2px solid transparent;
      user-select: none;
      align-items: center;

      &.active {
        color: #fff;
        background: #313238;
        border-top: 2px solid #3a84ff;
      }
    }
  }

  .jb-ace-main {
    display: flex;
    background: #272822;

    .ace-edit-content {
      flex: 1;
      overflow: hidden;
    }

    .bk-loading {
      background: "rgba(0, 0, 0, 80%)" !important;
    }
  }

  .jb-ace-action {
    position: absolute;
    top: 0;
    right: 0;
    z-index: 1;
    display: flex;
    align-items: center;
    padding-right: 9px;
    font-size: 16px;
    line-height: 1;
    color: #c4c6cc;

    .job-icon {
      padding: 10px 9px;
      cursor: pointer;
    }
  }

  .jb-ace-history-panel {
    position: absolute;
    top: 40px;
    right: 10px;
    width: 350px;
    background: #fff;
    border-radius: 2px;
    user-select: none;

    &::before {
      position: absolute;
      top: -4px;
      right: 45px;
      width: 10px;
      height: 10px;
      background: inherit;
      content: "";
      transform: rotateZ(-45deg);
    }

    .panel-header {
      display: flex;
      align-items: center;
      height: 60px;
      padding: 0 20px;
      font-size: 14px;
      color: #313238;
      border-bottom: 1px solid #e7e7e7;

      .save-btn {
        display: flex;
        width: 86px;
        height: 32px;
        margin-left: auto;
        color: #63656e;
        cursor: pointer;
        background: #fff;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        align-items: center;
        justify-content: center;
      }
    }

    .panel-body {
      padding: 10px 20px;
      font-family: 'MicrosoftYaHei'; /* stylelint-disable-line */
      font-size: 12px;
      color: #4f5050;
      background: #fafbfd;

      .item {
        display: flex;
        height: 32px;
        align-items: center;
      }

      .history-name {
        width: 255px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .history-action {
        margin-left: auto;
        color: #3a84ff;
        cursor: pointer;
      }
    }

    .history-empty {
      padding-top: 44px;
      padding-bottom: 85px;
    }
  }
</style>
