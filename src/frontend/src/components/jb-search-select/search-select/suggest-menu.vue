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
  import _ from 'lodash';

  import {
    encodeRegexp,
  } from './helper';
  import Mixin from './mixin';

  export default {
    name: 'BKSearchSuggest',
    mixins: [
      Mixin,
    ],
    data() {
      return {
        activeIndex: -1,
        list: [],
      };
    },
    computed: {
      needRender() {
        // 没有有选中key，且输入框中有输入值
        if (!this.searchSelect.menu.id && !!this.searchSelect.localValue) {
          return true;
        }
        return false;
      },
    },
    watch: {
      list(list) {
        const {
          primaryKey,
          defaultInputKey,
        } = this.searchSelect;

        this.activeIndex = -1;
        if (!defaultInputKey) {
          return;
        }

        list.forEach((item, index) => {
          if (defaultInputKey[primaryKey] === item.keyId) {
            this.activeIndex = index;
          }
        });
      },
    },
    created() {
      // eslint-disable-next-line no-underscore-dangle
      this.generatorList = _.debounce(this._generatorList, 100);
    },
    mounted() {
      document.body.addEventListener('keydown', this.handleKeydown);
    },
    beforeDestroy() {
      document.body.removeEventListener('keydown', this.handleKeydown);
    },
    methods: {
      _generatorList() {
        const {
          data,
          chipList,
          localValue,
          displayKey,
          primaryKey,
          _remoteKeyImmediateChildrenMap,
        } = this.searchSelect;

        const selectKeyMap = chipList.reduce((result, item) => {
          result[item[primaryKey]] = true;
          return result;
        }, {});
        const search = localValue;

        const stack = [];
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < data.length; i++) {
          const current = data[i];
          // 过滤已选key
          if (selectKeyMap[current[primaryKey]]) {
            continue;
          }
          if (typeof current.remoteMethod === 'function'
            && !current.inputInclude
            && !current.remoteExecuteImmediate) {
            continue;
          }

          const children = _remoteKeyImmediateChildrenMap[current[primaryKey]] || current.children;

          if (children) {
            // 搜索本地配置子项
            children.forEach((currentChild) => {
              const reg = new RegExp(encodeRegexp(search), 'i');
              if (reg.test(currentChild.name)) {
                stack.push({
                  keyName: current[displayKey],
                  keyId: current[primaryKey],
                  valueName: currentChild[displayKey],
                  valueId: currentChild[primaryKey],
                  payload: current,
                });
              }
            });
            continue;
          }

          // 匹配验证规则
          if (typeof current.validate === 'function') {
            if (current.validate([
              { name: search },
            ]) !== true) {
              continue;
            }
          }

          stack.push({
            keyName: current[displayKey],
            keyId: current[primaryKey],
            valueName: search,
            valueId: search,
            payload: current,
          });
        }

        this.list = Object.freeze(stack);
      },

      handleKeydown(event) {
        if (!this.needRender) {
          return;
        }
        // 取消选中状态
        if (event.keyCode === 27) {
          this.activeIndex = -1;
          // this.$emit('enter-invalid-toggle', false)
          return;
        }
        // enter键直接触发选中
        if (event.keyCode === 13 && this.activeIndex > -1) {
          this.handleClick(this.list[this.activeIndex]);
          return;
        }
        this.scrollActiveToView(event);
        this.$emit('enter-invalid-toggle', true);
      },

      handleClick(payload) {
        const { displayKey } = this.searchSelect;
        const { primaryKey } = this.searchSelect;

        this.$emit('select', {
          [primaryKey]: payload.keyId,
          [displayKey]: payload.keyName,
          values: [
            {
              [primaryKey]: payload.valueId,
              [displayKey]: payload.valueName,
            },
          ],
        });
      },

      renderList(h) {
        // 列表为空
        if (this.list.length < 1) {
          return (
                    <div class="search-loading">{ this.searchSelect.remoteEmptyText }</div>
          );
        }

        return (
                <div ref="list" class="search-suggest-menu-wraper">
                    <table class="search-suggest-menu-list">
                        {
                            this.list.map((item, index) => (
                                <tr
                                    class={{
                                        'search-suggest-menu-item': true,
                                        active: index === this.activeIndex,
                                    }}
                                    key={index}
                                    onClick={() => this.handleClick(item)}>
                                    <td class="search-suggest-item-label">
                                        { item.keyName }：
                                    </td>
                                    <td class="search-suggest-item-value">
                                        <div class="value-text">{ item.valueName }</div>
                                        {
                                            item.payload && item.payload.description
                                                ? <div class="description-text">
                                                    ({ item.payload.description })
                                                </div>
                                                : ''
                                        }
                                    </td>
                                </tr>
                            ))
                        }
                    </table>
                </div>
        );
      },
    },
    render(h) {
      if (!this.needRender) {
        return null;
      }
      return (
            <div class="jb-bk-search-list" role="suggest-menu" tabIndex="-1">
                {this.renderList(h)}
            </div>
      );
    },
  };
</script>
