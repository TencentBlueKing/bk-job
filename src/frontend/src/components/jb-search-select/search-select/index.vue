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
    ref="searchSelect"
    class="jb-bk-search-select"
    :class="{
      'focused': focused,
    }"
    v-bind="$attrs"
    @click="handleSearchSelectClick">
    <div
      ref="wrap"
      class="search-select-wrap">
      <div
        ref="tagGroup"
        class="search-tag-group"
        :style="tagGroupStyles">
        <render-tag
          v-for="(item, index) in renderTagList"
          :key="`${item.id}_${index}`"
          ref="tag"
          :data="item"
          :index="index"
          @change="handleTagChange"
          @delete="handleTagDelete"
          @focus="handleInputFocus" />
        <div
          v-if="isShowTagMultLine"
          key="multPlaceholder"
          class="mult-tag-placeholder">
          ...
        </div>
        <div
          key="input"
          ref="input"
          class="search-input-box"
          :style="searchInputBoxStyles"
          @click.stop="">
          <div style="position: absolute; top: -9999px; left: -9999px;">
            <pre
              ref="realInputContent"
              style="display: block; font: inherit; visibility: hidden;">{{ localValue }}</pre>
          </div>
          <div style="min-height: 22px; word-break: break-all; white-space: normal; visibility: hidden;">
            {{ localValue }}
          </div>
          <textarea
            ref="textarea"
            v-bk-clickoutside="handleInputOutSide"
            class="input-box"
            :placeholder="placeholderText"
            spellcheck="false"
            :value="localValue"
            @focus="handleInputFocus"
            @input="handleInputChange"
            @keydown="handleInputKeydown" />
        </div>
        <!-- <div
                    v-if="focused"
                    v-once
                    style="margin-top: 4px; font-size: 12px; line-height: 22px; color: #c4c6cc;">
                    {{ inputTips }}
                </div> -->
      </div>
      <div class="search-nextfix">
        <i
          v-if="isClearable"
          class="search-clear bk-icon icon-close-circle-shape"
          @click.self="handleClearAll" />
        <slot name="nextfix">
          <i
            class="bk-icon icon-search search-nextfix-icon"
            :class="{ 'is-focus': focused }"
            @click.stop="handleSubmit" />
        </slot>
      </div>
    </div>
    <div
      v-if="validateStr.length"
      class="bk-select-tips">
      <slot name="validate">
        <i class="bk-icon icon-exclamation-circle-shape select-tips" />{{ validateStr || '' }}
      </slot>
    </div>
  </div>
</template>
<script>
/* eslint-disable no-underscore-dangle */
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import _ from 'lodash';
  import Vue from 'vue';

  import {
    encodeRegexp,
    generatorMenu,
    popperConfig,
  } from './helper';
  import KeyMenu from './key-menu';
  import locale from './locale';
  import RenderTag from './render-tag';
  import SuggestMenu from './suggest-menu';
  import ValueMenu from './value-menu';

  export default {
    name: 'BkSearchSelect',
    components: {
      RenderTag,
    },
    provide() {
      return {
        searchSelect: this,
      };
    },

    model: {
      prop: 'values',
      event: 'change',
    },

    props: {
      data: {
        default: () => [],
        validator(data) {
          if (!Array.isArray(data)) {
            return false;
          }
          return true;
        },
      },
      explainCode: {
        type: String,
        default: '：',
      },
      placeholder: {
        type: String,
        default: locale.t('bk.searchSelect.placeholder'),
      },
      emptyText: {
        type: String,
        default: locale.t('bk.searchSelect.emptyText'),
      },

      displayKey: {
        type: String,
        default: 'name',
      },
      primaryKey: {
        type: String,
        default: 'id',
      },
      condition: {
        type: Object,
        default() {
          return {};
        },
      },
      values: {
        type: Array,
        default() {
          return [];
        },
      },
      remoteEmptyText: {
        type: String,
        default: locale.t('bk.searchSelect.remoteEmptyText'),
      },
      remoteLoadingText: {
        type: String,
        default: locale.t('bk.searchSelect.remoteLoadingText'),
      },
      showCondition: {
        type: Boolean,
        default: true,
      },
      readonly: {
        type: Boolean,
        default: false,
      },
      defaultFocus: {
        type: Boolean,
        default: false,
      },
      clearable: {
        type: Boolean,
        default: false,
      },
      maxTagWidth: {
        type: Number,
        default: 100,
      },
    },

    data() {
      return {
        menu: generatorMenu(),
        chipList: [],
        maxRenderTagNums: -1,
        textareaWidth: 0,
        focused: this.defaultFocus,
        localValue: '',
        defaultCondition: {},
        validateStr: '',
      };
    },

    computed: {
      currentSelectKey() {
        return this.data.find(item => item[this.primaryKey] === this.menu.id) || {};
      },
      // 通过输入框直接搜索的key
      defaultInputKey() {
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.data.length; i++) {
          const currentkey = this.data[i];
          // 没有配置自选项的key才支持设置为默认key
          if (currentkey.remote || currentkey.children || currentkey.remoteMethod) {
            continue;
          }
          if (currentkey.default) {
            return currentkey;
          }
        }
        return null;
      },

      placeholderText() {
        return this.chipList.length > 0 ? '' : this.placeholder;
      },
      renderTagList() {
        if (this.focused) {
          return this.chipList;
        }
        if (this.maxRenderTagNums < 1) {
          return this.chipList;
        }
        return this.chipList.slice(0, this.maxRenderTagNums);
      },
      isShowTagMultLine() {
        if (this.focused) {
          return false;
        }
        return this.maxRenderTagNums > 0;
      },

      tagGroupStyles() {
        return {
          width: 'calc(100% - 50px)',
          'max-height': this.focused ? '320px' : '30px',
          'white-space': this.focused ? 'initial' : 'nowrap',
        };
      },

      searchInputBoxStyles() {
        const styles = {
          position: 'relative',
          width: this.focused ? `${this.textareaWidth}px` : 'auto',
          'min-width': '20px',
          'max-width': '100%',
        };
        if (this.chipList.length < 1) {
          styles.width = '100%';
        }

        return styles;
      },

      isClearable() {
        return !this.readonly && this.clearable && this.chipList.length > 0;
      },
    },

    watch: {
      values: {
        handler(values) {
          if (values !== this.chipList) {
            this.chipList = values;
          }
        },
        deep: true,
        immediate: true,
      },
      menu: {
        handler() {
          this.updateLocalInput();
        },
        deep: true,
      },
    },

    created() {
      this.panelInstance = null;
      this.popperInstance = null;
      this.renderTagInstance = null;

      this.defaultCondition = {
        name: locale.t('bk.searchSelect.condition'),
      };
      if (!this.defaultCondition[this.displayKey]) {
        this.defaultCondition[this.displayKey] = locale.t('bk.searchSelect.condition');
      }

      this.inputTips = locale.t('bk.searchSelect.tips');

      this.calcTextareaWidth = _.throttle(this._calcTextareaWidth, 30);
      this.showPopper = _.throttle(this._showMenu, 50);
      this.remoteExecuteImmediate();
    },

    beforeDestroy() {
      this.popperInstance && this.popperInstance.destroy(true);
    },

    methods: {
      /**
       * @desc 计算输入框的高度
       */
      _calcTextareaWidth() {
        this.$nextTick(() => {
          const { width } = this.$refs.realInputContent.getBoundingClientRect();
          this.textareaWidth = width + 20;
        });
      },
      /**
       * @desc 显示 key 面板
       * @param {Object} lastPanelInstance 显示的下来面板实例
       */
      _showKeyMenu(lastPanelInstance) {
        if (this.panelInstance) {
          return;
        }
        let instance = null;
        if (lastPanelInstance && lastPanelInstance.$options.name === 'BKSearchKey') {
          instance = lastPanelInstance;
        } else {
          instance = new Vue(KeyMenu);
          instance.searchSelect = this;
          instance.$on('select', this.handleKeyChange);
          instance.$on('select-conditon', this.handleKeyConditonChange);
        }
        if (instance.needRender) {
          !instance._isMounted && instance.$mount();
          instance.generatorList();
          this.panelInstance = instance;
        }
      },
      /**
       * @desc 显示 value 面板
       * @param {Object} lastPanelInstance 显示的下来面板实例
       */
      _showValueMenu(lastPanelInstance) {
        if (this.panelInstance) {
          return;
        }
        let realValue = this.localValue;
        const keyText = this.currentSelectKey[this.displayKey] + this.explainCode || '';
        realValue = realValue.slice(keyText.length);
        const conditionText = this.menu.condition[this.displayKey] || '';
        realValue = realValue.slice(conditionText.length);

        let instance = null;
        if (lastPanelInstance && lastPanelInstance.$options.name === 'BKSearchValue') {
          instance = lastPanelInstance;
        } else {
          instance = new Vue(ValueMenu);
          instance.searchSelect = this;
          instance.$on('select-condition', this.handleValueConditionChange);
          instance.$on('select-check', this.handleMultCheck);
          instance.$on('change', this.handleValueChange);
          instance.$on('cancel', this.handleValueCancel);
        }

        instance.search = realValue.trim();
        instance.currentItem = this.currentSelectKey;
        instance.menu = this.menu;

        if (instance.needRender) {
          !instance._isMounted && instance.$mount();
          instance.generatorList();
          this.panelInstance = instance;
        }
      },
      /**
       * @desc 显示 suggest 面板
       * @param {Object} lastPanelInstance 显示的下来面板实例
       */
      _showSuggestMenu(lastPanelInstance) {
        if (this.panelInstance) {
          return;
        }
        let instance = null;
        if (lastPanelInstance && lastPanelInstance.$options.name === 'BKSearchSuggest') {
          instance = lastPanelInstance;
        } else {
          instance = new Vue(SuggestMenu);
          instance.searchSelect = this;
          instance.$on('select', this.handleMenuSuggestSelect);
        }
        if (instance.needRender) {
          !instance._isMounted && instance.$mount();
          instance.generatorList();
          this.panelInstance = instance;
        }
      },
      /**
       * @desc 显示下拉面板
       */
      _showMenu() {
        if (!this.popperInstance) {
          this.popperInstance = Tippy(this.$refs.input, { ...popperConfig });
        }

        const lastPanelInstance = this.panelInstance;
        this.panelInstance = null;
        setTimeout(() => {
          this._showKeyMenu(lastPanelInstance);
          this._showValueMenu(lastPanelInstance);
          this._showSuggestMenu(lastPanelInstance);

          if (!this.panelInstance) {
            lastPanelInstance && lastPanelInstance.$destroy();
            this.hidePopper(lastPanelInstance);
            return;
          }
          // 两次的弹出面板不是同一类型——销毁上一个
          if (lastPanelInstance
            && lastPanelInstance.$options.name !== this.panelInstance.$options.name) {
            lastPanelInstance.$destroy();
          }
          this.popperInstance.set({
            zIndex: window.__bk_zIndex_manager.nextZIndex(),
          });
          this.popperInstance.setContent(this.panelInstance.$el);
          this.popperInstance.popperInstance.update();
          this.popperInstance.show();
          this.renderTagInstance && this.renderTagInstance.hidePopper();
        });
      },
      /**
       * @desc 隐藏下拉面板
       */
      hidePopper() {
        if (this.panelInstance) {
          this.panelInstance.$destroy();
          this.panelInstance = null;
        }
        if (this.popperInstance) {
          this.popperInstance.hide(0);
        }
      },
      /**
       * @desc 立即执行 remote 配置项
       */
      remoteExecuteImmediate() {
        this._remoteKeyImmediateChildrenMap = {};
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.data.length; i++) {
          const currentItem = this.data[i];
          if (typeof currentItem.remoteMethod === 'function'
            && currentItem.remoteExecuteImmediate) {
            (async () => {
              try {
                const children = await currentItem.remoteMethod();
                this._remoteKeyImmediateChildrenMap[currentItem[this.primaryKey]] = children;
              } catch (error) {
                console.log(error);
              }
            })();
          }
        }
      },
      /**
       * @desc 设置输入框的值
       */
      updateLocalInput() {
        if (!this.menu.id) {
          this.localValue = '';
        } else {
          let text = `${this.currentSelectKey[this.displayKey]}${this.explainCode}`;
          if (this.menu.condition[this.primaryKey]) {
            text += `${this.menu.condition[this.displayKey]} `;
          }
          text += this.menu.checked.map(_ => _[this.displayKey]).join(' | ');
          this.localValue = text;
        }
        this.calcTextareaWidth();
        setTimeout(() => {
          this.$refs.textarea.focus();
        });
      },
      /**
       * @desc 验证用户输入
       * @param {Array} valList 用户输入结果列表
       */
      valueValidate(valList) {
        let validate = true;
        if (this.currentSelectKey
          && this.currentSelectKey.validate
          && typeof this.currentSelectKey.validate === 'function') {
          validate = this.currentSelectKey.validate([
            ...valList,
          ], this.currentSelectKey);
          if (typeof validate === 'string') {
            this.validateStr = validate;
            validate = false;
          } else {
            validate && (this.validateStr = '');
          }
        } else {
          this.validateStr = '';
        }
        return validate;
      },
      /**
       * @desc 新增一个选项
       * @param {Object} item 用户选中的筛选项
       */
      appendChipList(item) {
        const validate = this.valueValidate(item.values);
        if (!validate) return;

        const result = [
          ...this.chipList,
        ];
        result.push(item);

        // 根据primaryKey去重
        const memoMap = {};
        const stack = [];
        // eslint-disable-next-line no-plusplus
        for (let i = result.length - 1; i >= 0; i--) {
          const primaryKey = result[i][this.primaryKey];
          if (!primaryKey) {
            stack.unshift(result[i]);
            continue;
          }
          if (!memoMap[primaryKey]) {
            stack.unshift(result[i]);
            memoMap[primaryKey] = true;
          }
        }
        this.chipList = Object.freeze(stack);
        this.triggerChange();
      },
      /**
       * @desc 触发 change 操作
       */
      triggerChange() {
        this.menu = generatorMenu();
        this.$emit('change', [
          ...this.chipList,
        ]);
      },
      /**
       * @desc 点击时获得焦点
       */
      handleSearchSelectClick() {
        this.handleInputFocus();
      },
      /**
       * @desc 输入框获得焦点时需要执行的逻辑
       */
      handleInputFocus() {
        this.renderTagInstance && this.renderTagInstance.hidePopper();
        if (this.readonly) {
          return;
        }

        this.focused = true;

        this.$refs.textarea.focus();
        this.showPopper();
        this.$emit('focus');
      },
      /**
       * @desc 输入框失去焦点时需要执行的逻辑
       */
      handleInputOutSide(event) {
        if (!this.focused) {
          return;
        }
        let parent = event.target.parentNode;
        while (parent && parent.classList) {
          if (parent.classList.contains('jb-bk-search-list') || parent.classList.contains('jb-bk-search-select')) {
            return;
          }
          parent = parent.parentNode;
        }
        this.hidePopper();
        this.maxRenderTagNums = -1;
        this.focused = false;

        this.$nextTick(() => {
          const allTag = this.$refs.tagGroup.querySelectorAll('.search-tag-box');
          const {
            width: searchSelectWidth,
          } = this.$refs.searchSelect.getBoundingClientRect();

          let tagWidthTotal = 0;
          // eslint-disable-next-line no-plusplus
          for (let i = 0; i < allTag.length; i++) {
            const { width } = allTag[i].getBoundingClientRect();
            if (tagWidthTotal + width + 50 < searchSelectWidth) {
              tagWidthTotal = tagWidthTotal + width + 6;
            } else {
              this.maxRenderTagNums = i;
              break;
            }
          }
        });
        this.$emit('blur');
      },
      /**
       * @desc 输入框输入时需要执行的逻辑
       * @param {Object} event 输入框输入事件
       */
      handleInputChange(event) {
        const text = event.target.value.replace(/[\r\n]/, '');
        if (text === '') {
          this.menu = generatorMenu();
        }
        this.localValue = text;
        this.calcTextareaWidth();
        this.showPopper();
        this.$emit('input', event);
      },
      /**
       * @desc 键盘删除时的事件
       * @param {Object} event Keydown 事件
       */
      keyDelete(event) {
        // 删除逻辑的优先级需要保持下面的顺序

        // 删除value
        if (this.menu.checked.length > 0) {
          event.preventDefault();
          const checked = [
            ...this.menu.checked,
          ];
          checked.pop();
          this.menu.checked = Object.freeze(checked);
          return;
        }
        // 删除condition
        const condition = this.menu.condition[this.displayKey];
        const localValue = this.currentSelectKey[this.displayKey] + this.explainCode;
        if (condition && localValue + condition === this.localValue) {
          event.preventDefault();
          this.menu.checked = [];
          this.menu.condition = {};
          this.showPopper();
          return;
        }
        // 删除已选key
        if (this.currentSelectKey[this.primaryKey]) {
          const regx = new RegExp(`${encodeRegexp(this.explainCode)}$`);
          if (regx.test(this.localValue)) {
            event.preventDefault();
            this.menu = generatorMenu();
            this.showPopper();
            return;
          }
        }
        // 删除输入内容
        if (this.localValue !== '') {
          return;
        }
        // 删除tag
        if (this.chipList.length > 0) {
          const result = [
            ...this.chipList,
          ];
          result.pop();
          this.chipList = Object.freeze(result);
          this.triggerChange();
          this.showPopper();
        }
      },
      /**
       * @desc 键盘 enter 时的事件
       * @param {Object} event Keydown 事件
       */
      keySubmit() {
        // 输入框没有内容
        if (!this.localValue) {
          return;
        }

        if (!this.currentSelectKey[this.primaryKey]) {
          // 没有选中key

          if (this.defaultInputKey) {
            // 已配置默认key

            const id = this.defaultInputKey[this.primaryKey];
            const name = this.defaultInputKey[this.displayKey];

            this.appendChipList({
              [this.primaryKey]: id,
              [this.displayKey]: name,
              values: [
                {
                  [this.primaryKey]: this.localValue,
                  [this.displayKey]: this.localValue,
                },
              ],
            });
            this.showPopper();
          } else {
            // 直接使用输入框的内容
            this.appendChipList({
              [this.primaryKey]: this.localValue,
              [this.displayKey]: this.localValue,
              values: [],
            });
            this.showPopper();
          }
        } else {
          // 已选中key

          // 1，如果value配置了本地children或者支持remoteMethod，输入框直接enter无效（需要通过value面板选择）
          if (this.currentSelectKey.children
            || typeof this.currentSelectKey.remoteMethod === 'function') {
            return;
          }
          // 2, enter时value不能为空
          const keyText = this.currentSelectKey[this.displayKey] + this.explainCode;
          const conditionText = this.menu.condition[this.displayKey] || '';
          if (this.localValue.trim() === keyText + conditionText) {
            this.showPopper();
            return;
          }

          // 提交结果
          let realValue = this.localValue.replace(keyText, '');
          if (conditionText) {
            realValue = realValue.replace(this.menu.condition[this.displayKey], '');
          }

          const id = this.currentSelectKey[this.primaryKey];
          const name = this.currentSelectKey[this.displayKey];
          this.appendChipList({
            [this.primaryKey]: id,
            [this.displayKey]: name,
            values: [
              {
                [this.primaryKey]: realValue,
                [this.displayKey]: realValue,
              },
            ],
            condition: this.menu.condition,
          });
          this.showPopper();
        }
      },
      /**
       * @desc 用户操作时需要执行的逻辑
       * @param {Object} event Keydown 事件
       */
      handleInputKeydown(event) {
        if (this.readonly) {
          event.preventDefault();
          return;
        }
        if ([
          'ArrowDown',
          'ArrowUp',
        ].includes(event.code)) {
          event.preventDefault();
          return;
        }
        if ([
          'Backspace',
        ].includes(event.code)) {
          this.keyDelete(event);
          return;
        }
        if ([
          'Enter',
          'NumpadEnter',
        ].includes(event.code) && !event.isComposing) {
          event.preventDefault();
          if (this.popperInstance && this.popperInstance.state.isVisible) {
            return;
          }
          this.keySubmit(event);
        }
      },
      /**
       * @desc 用户点击搜索 icon 时需要执行的逻辑
       * @param {Object} event click 事件
       */
      handleSubmit(event) {
        this.keySubmit();
        // 通过搜索按钮触发提交不继续显示keymemu
        setTimeout(() => {
          this.$refs.textarea.blur();
          this.focused = false;
          this.hidePopper();
        }, 100);
        this.$emit('clear', event);
      },
      /**
       * @desc 编辑已选结果的处理逻辑
       * @param {Number} index 索引
       * @param {Object} value 新的搜索值
       */
      handleTagChange(index, value) {
        const list = [
          ...this.chipList,
        ];
        list.splice(index, 1, value);
        this.chipList = Object.freeze(list);
        this.triggerChange(false);
      },
      /**
       * @desc 删除已选结果的处理逻辑
       * @param {Number} index 索引
       */
      handleTagDelete(index) {
        const result = [
          ...this.chipList,
        ];
        result.splice(index, 1);
        this.chipList = Object.freeze(result);
        this.triggerChange();
      },
      /**
       * @desc 清空所有搜索值
       */
      handleClearAll() {
        this.menu = generatorMenu();
        this.chipList = [];
        this.triggerChange();
        this.showPopper();
        this.$emit('clear');
      },
      /**
       * @desc 用户选中了一个 key
       * @param {String} key 删选项的 key
       */
      handleKeyChange(key) {
        this.menu = generatorMenu();
        this.menu.id = key[this.primaryKey];

        this.showPopper();
      },
      /**
       * @desc 用户选中了一个 condition
       * @param {String} key condition 的key
       */
      handleKeyConditonChange(value) {
        this.appendChipList(value);
        this.showPopper();
      },
      /**
       * @desc 用户选中了一个 condition 的 value
       * @param {Object} condition condition 的 value
       */
      handleValueConditionChange(condition) {
        this.menu.condition = condition;
        this.showPopper();
      },
      /**
       * @desc 多选时的处理逻辑
       * @param {Array} values condition 的 value
       */
      handleMultCheck(values) {
        this.menu.checked = Object.freeze(values);
      },
      /**
       * @desc 用户选中了 value 面板时的处理逻辑
       */
      handleValueChange() {
        const id = this.currentSelectKey[this.primaryKey];
        const name = this.currentSelectKey[this.displayKey];
        this.appendChipList({
          [this.primaryKey]: id,
          [this.displayKey]: name,
          values: this.menu.checked,
          condition: this.menu.condition,
        });
        this.showPopper();
      },
      /**
       * @desc 用户关闭了 value 面板时的处理逻辑
       */
      handleValueCancel() {
        this.menu = generatorMenu();
        this.showPopper();
      },
      /**
       * @desc 用户选中了 suggest 面板时的处理逻辑
       */
      handleMenuSuggestSelect(value) {
        this.appendChipList(value);
        this.showPopper();
      },
      /**
       * @desc 外部调用——获取输入框实例引用
       */
      getInputInstance() {
        return this.$refs.textarea;
      },
      /**
       * @desc 外部调用——重置搜索状态
       */
      reset() {
        this.maxRenderTagNums = -1;
        this.chipList = [];
        this.$emit('change', []);
      },
    },
  };
</script>

<style lang="postcss">
  @import url("./styles/search-select.css");
  @import url("./styles/search-select-menu.css");
</style>
