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

<script>
  import Tippy from 'bk-magic-vue/lib/utils/tippy';
  import Vue from 'vue';

  import {
    generatorMenu,
    popperConfig,
  } from './helper';
  import ValueMenu from './value-menu';

  let tagInstance = null;

  export default {
    name: '',
    inject: [
      'searchSelect',
    ],
    props: {
      index: {
        type: Number,
        required: true,
      },
      data: {
        type: Object,
        required: true,
      },
    },
    data() {
      return {
        isEditing: false,
        textareaWidth: 0,
        localValue: '',
        keyBoxWidth: '',
        searchSelectWidth: 0,
        menu: generatorMenu(),
      };
    },
    computed: {
      textareaStyles() {
        return {
          position: 'relative',
          width: `${this.textareaWidth}px`,
          'max-width': `${this.searchSelectWidth - this.keyBoxWidth - 20}px`,
        };
      },
      valueStyles() {
        const {
          isTagMultLine,
          maxTagWidth,
        } = this.searchSelect;

        if (isTagMultLine && maxTagWidth > 0) {
          return {
            'max-width': `${maxTagWidth}px`,
            height: '22px',
            'text-overflow': 'ellipsis',
            overflow: 'hidden',
            'white-space': 'nowrap',
          };
        }
        return {};
      },
      conditionText() {
        // 是否选中condition
        let conditionText = '';
        if (Object.keys(this.menu.condition).length > 0) {
          conditionText = this.menu.condition[this.searchSelect.displayKey];
        }
        return conditionText;
      },
      currentItem() {
        return this.searchSelect.data.find(_ => _.id === this.data.id);
      },
    },
    watc: {
      data() {
        this.updateLocalValue();
      },
    },
    created() {
      this.menu = {
        id: this.data.id,
        checked: this.data.values,
        condition: this.data.condition || {},
      };
      this.updateLocalValue();
      this.memoValue = this.localValue;
      this.searchSelect.$once('show-menu', this.hidePopper);
    },
    mounted() {
      this.searchSelect.renderTagInstance = this;
      this.keyBoxWidth = this.$refs.keyBox.getBoundingClientRect().width;
      document.body.addEventListener('click', this.handleInputOutSide);
    },
    beforeDestroy() {
      document.body.removeEventListener('click', this.handleInputOutSide);
    },
    methods: {
      calcTextareaWidth() {
        this.$nextTick(() => {
          const { width } = this.$refs.realContent.getBoundingClientRect();
          this.textareaWidth = width + 20;
        });
      },

      updateLocalValue() {
        const {
          displayKey,
        } = this.searchSelect;
        let text = '';
        const conditionKey = this.menu.condition[displayKey];
        if (conditionKey) {
          text = `${conditionKey} `;
        }

        const values = this.menu.checked;
        if (values && values.length > 0) {
          text = `${text}${values.map(_ => _[displayKey]).join(' | ')}`;
        }
        this.localValue = text;
        this.calcTextareaWidth();
      },

      showPopper() {
        if (tagInstance && tagInstance !== this) {
          tagInstance.hidePopper();
        }

        tagInstance = this;
        if (!this.popperInstance) {
          this.popperInstance = Tippy(this.$refs.valueBox, { ...popperConfig });
        }
        if (!this.menuInstance) {
          this.menuInstance = new Vue(ValueMenu);
          this.menuInstance.searchSelect = this.searchSelect;
          this.menuInstance.$on('select-condition', this.handleConditionSelect);
          this.menuInstance.$on('select-check', this.handleMultCheck);
          this.menuInstance.$on('change', this.handleSubmit);
          this.menuInstance.$on('cancel', this.handleMultCancel);
        }

        this.menuInstance.search = this.localValue.slice(this.conditionText.length).trim();
        this.menuInstance.currentItem = this.currentItem;

        this.menuInstance.menu = this.menu;

        if (this.menuInstance.needRender) {
          this.menuInstance.$mount();
          this.menuInstance.generatorList();
          this.popperInstance.set({
            zIndex: window.__bk_zIndex_manager.nextZIndex(), // eslint-disable-line no-underscore-dangle
          });
          this.popperInstance.setContent(this.menuInstance.$el);
          this.popperInstance.show();
        }
      },

      hidePopper() {
        this.isEditing = false;
        if (this.popperInstance) {
          this.popperInstance.hide(0);
          this.popperInstance.destroy();
          this.popperInstance = null;
        }
        if (this.menuInstance) {
          this.menuInstance.$destroy();
          this.menuInstance = null;
        }
      },

      handleWraperClick(event) {
        event.stopPropagation();
      },

      handleInputOutSide(event) {
        let parent = event.target.parentNode;
        while (parent && parent.classList) {
          if (parent.classList.contains('jb-bk-search-list') || parent.classList.contains('bk-search-select')) {
            return;
          }
          parent = parent.parentNode;
        }
        this.localValue = this.memoValue;
        this.hidePopper();
      },

      handleTagClick(event) {
        if (this.isEditing) {
          return;
        }
        if (!this.searchSelect.focused) {
          this.$emit('focus');
          return;
        }
        setTimeout(() => {
          this.$refs.textarea.focus();
          this.$refs.textarea.selectionStart = this.conditionText.length;
          this.$refs.textarea.selectionEnd = this.localValue.length;
        });
        this.calcTextareaWidth();
        this.isEditing = true;
        this.searchSelectWidth = this.searchSelect.$refs.searchSelect.getBoundingClientRect().width;
        // 编辑tag的值时隐藏 searchSelect 操作面板
        this.searchSelect.hidePopper();
        this.showPopper();
      },

      handleTagInput(event) {
        // 禁用换行符
        this.localValue = event.target.value.replace(/\n/, '');

        this.calcTextareaWidth();

        if (this.menuInstance) {
          this.menuInstance.search = this.localValue;
          if (this.menuInstance.isCondition) {
            // 重新选择condition
            const conditionText = this.menu.condition[this.searchSelect.displayKey] || '';

            if (!conditionText
              || conditionText.length > this.localValue.length
              || (conditionText.length === this.localValue.length && conditionText !== this.localValue)) {
              this.menu = {
                ...this.menu,
                condition: {},
                checked: [],
              };
              setTimeout(() => {
                this.menuInstance && this.menuInstance.generatorList();
              });
            }
          }
          this.showPopper();
        }
      },

      handleRemove() {
        this.$emit('delete', this.index);
      },
      // 多选时——选中一项
      handleMultCheck(values) {
        this.menu.checked = Object.freeze(values);
        this.updateLocalValue();
      },

      // enter 提交值
      handleKeydown(event) {
        if ([
          'Backspace',
        ].includes(event.code) && this.localValue === '') {
          this.handleRemove();
          this.hidePopper();
          return;
        }
        if ([
          'ArrowDown',
          'ArrowUp',
        ].includes(event.code)) {
          event.preventDefault();
          return;
        }
        // enter 键触发
        if ([
          'Enter',
          'NumpadEnter',
        ].includes(event.code)) {
          event.preventDefault();
          if (this.popperInstance && this.popperInstance.state.isVisible) {
            return;
          }
          const {
            displayKey,
            primaryKey,
          } = this.searchSelect;
          let realValue = this.localValue;
          if (this.conditionText) {
            realValue = this.localValue.slice(this.conditionText.length);
          }
          if (realValue === '') {
            return;
          }
          this.menu.checked = [
            {
              [displayKey]: realValue,
              [primaryKey]: realValue,
            },
          ];
          this.handleSubmit();
        }
      },
      // 提交 tag 的值
      handleSubmit(values) {
        const value = {
          ...this.data,
          values: this.menu.checked,
        };
        this.hidePopper();
        this.updateLocalValue();
        this.memoValue = this.localValue;
        this.$emit('change', this.index, value);
      },
      // 取消编辑
      handleMultCancel() {
        this.localValue = this.memoValue;
        this.hidePopper();
      },

      handleConditionSelect(value) {
        this.menu.condition = Object.freeze({ ...value });
        this.updateLocalValue();
        this.showPopper();
        if (!this.menuInstance.needRender) {
          this.popperInstance.hide(0);
          this.$refs.textarea.focus();
        }
      },

      renderKey(h) {
        const {
          displayKey,
          explainCode,
        } = this.searchSelect;
        const text = this.data[displayKey];

        return (
                <div ref="keyBox" class="tag-label">{text}{explainCode}</div>
        );
      },

      renderValue(h) {
        const renderContent = (h) => {
          if (this.isEditing) {
            return (
                        <div style={this.textareaStyles}>
                            <div style="min-height: 22px; white-space: normal; word-break: break-all; visibility: hidden;">{this.localValue}</div>
                            <textarea
                                ref="textarea"
                                class="tag-value-edit"
                                spellcheck="false"
                                value={this.localValue}
                                onKeydown={this.handleKeydown}
                                onInput={this.handleTagInput} />
                        </div>
            );
          }
          return (
                    <div style={this.valueStyles} onClick={this.handleTagClick}>{this.localValue}</div>
          );
        };
        return (
                <div ref="valueBox" class="tag-value" onClick={this.handleWraperClick}>
                    <div style="position: absolute; top: -9999px; left: -9999px;">
                        <pre ref="realContent" style="display: block; visibility: hidden; font: inherit;">{this.localValue}</pre>
                    </div>
                    {renderContent(h)}
                </div>
        );
      },

      renderClear(h) {
        if (this.searchSelect.readonly || this.isEditing) {
          return null;
        }
        return (
                <i class="tag-clear bk-icon icon-close" onClick={this.handleRemove} />
        );
      },

    },

    render(h) {
      const classes = {
        'search-tag-box': true,
        focused: this.isEditing,
      };
      return (
            <div class={classes}>
                <div class="search-tag">
                    {this.renderKey(h)}
                    {this.renderValue(h)}
                </div>
                {this.renderClear(h)}
            </div>
      );
    },
  };
</script>
