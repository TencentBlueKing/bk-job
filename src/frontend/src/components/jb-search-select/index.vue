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
  <search-select
    ref="searchSelect"
    class="jb-search-select"
    v-bind="$attrs"
    :data="data"
    :placeholder="placeholder"
    :show-condition="false"
    :values="searchValue"
    v-on="$listeners"
    @change="handleChange" />
</template>
<script>
  import _ from 'lodash';

  import { userSearchCache } from '@utils/cache-helper';

  import SearchSelect from './search-select';

  const filterValue = (payload) => {
    // 过滤空值，保证每项只会筛选一次
    const result = _.cloneDeep(payload);
    const valueMap = {};
    const resultIdSet = new Set();
    result.forEach((value) => {
      if (!value) {
        return;
      }
      if (resultIdSet.has(value.id)) {
        // 删除旧的值，同一个ID再次被添加时保持添加顺序
        resultIdSet.delete(value.id);
      }
      resultIdSet.add(value.id);
      valueMap[value.id] = value;
    });
    return [...resultIdSet].map(key => Object.freeze(valueMap[key]));
  };

  export default {
    name: 'JbSearchSelect',
    components: {
      SearchSelect,
    },
    props: {
      data: {
        type: Array,
        default: () => [],
      },
      placeholder: {
        type: String,
        default: '',
      },
      value: {
        type: Array,
        default: () => [],
      },
      parseUrl: {
        type: Boolean,
        default: true,
      },
      // 外部设置的筛选值添加到已有筛选值后面
      appendValue: {
        type: Array,
        default: () => [],
      },
    },
    data() {
      return {
        searchValue: [],
      };
    },
    watch: {
      value: {
        handler(val) {
          let oldSearchComponentValue = [];
          if (this.$refs.searchSelect) {
            oldSearchComponentValue = this.$refs.searchSelect.chipList;
          }
          this.searchValue = filterValue([...oldSearchComponentValue, ...val]);
        },
        immediate: true,
      },
      appendValue: {
        handler(appendValue) {
          setTimeout(() => {
            if (!this.$refs.searchSelect) {
              return;
            }
            const appendMap = appendValue.reduce((result, item) => {
              result[item.id] = true;
              return result;
            }, {});
            const value = [];
            this.$refs.searchSelect.chipList.forEach((currentValue) => {
              if (!appendMap[currentValue.id]) {
                value.push(currentValue);
              }
            });

            this.searchValue = Object.freeze([...value, ...appendValue]);
          });
        },
        immediate: true,
      },
    },
    created() {
      if (this.parseUrl) {
        this.parseURLData();
      }
    },
    methods: {
      /**
       * @desc 解析 URL 参数
       */
      parseURLData() {
        this.URLQuery = this.$route.query;

        const defaultSearchComponentValue = [];
        // 默认筛选数据
        const defaultSearchValue = {};
        const promiseStack = [];
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < this.data.length; i++) {
          const currentData = this.data[i];

          // 只解析存在于搜索配置中的字段
          if (!Object.prototype.hasOwnProperty.call(this.URLQuery, currentData.id)) {
            continue;
          }
          // 解析url中筛选项的值
          const currentURLParamValue = this.URLQuery[currentData.id];
          if (currentData.multiable) {
            defaultSearchValue[currentData.id] = currentURLParamValue.split(',');
          } else {
            defaultSearchValue[currentData.id] = currentURLParamValue;
          }

          let remoteHandler = Promise.resolve();

          if (currentData.remoteMethod) {
            remoteHandler = currentData.remoteMethod();
          } else if (currentData.children) {
            remoteHandler = Promise.resolve(currentData.children);
          } else {
            remoteHandler = Promise.resolve(currentURLParamValue);
          }

          const { id, name } = currentData;
          const urlSearchValue = this.URLQuery[id];

          remoteHandler.then((data) => {
            let currentSearchComponentValue = {};
            let searchValueArr = [];
            if (currentData.multiable) {
              searchValueArr = urlSearchValue.split(',');
            } else {
              searchValueArr = [urlSearchValue];
            }
            if (_.isArray(data)) {
              // 远程备选列表；本地备选列表
              const valueStack = [];
              searchValueArr.forEach((item) => {
                // 兼容筛选值已经被删掉的场景
                const childItem = data.find(_ => `${_.id}` === `${item}`);
                if (childItem) {
                  valueStack.push({
                    id: childItem.id,
                    name: childItem.name,
                  });
                }
              });
              if (valueStack.length < 1) {
                return;
              }

              currentSearchComponentValue = {
                id,
                name,
                values: valueStack,
              };
            } else {
              // 本地直接输入
              const valueStack = [];
              searchValueArr.forEach((item) => {
                // 兼容空值的情况
                if (item) {
                  valueStack.push({
                    id: item,
                    name: item,
                  });
                }
              });
              if (valueStack.length < 1) {
                return;
              }
              currentSearchComponentValue = {
                id,
                name,
                values: valueStack,
              };
            }
            defaultSearchComponentValue.push(currentSearchComponentValue);
          });
          promiseStack.push(remoteHandler);
        }
        Promise.all(promiseStack).finally(() => {
          let oldSearchComponentValue = [];
          if (this.$refs.searchSelect) {
            oldSearchComponentValue = this.$refs.searchSelect.chipList;
          }
          this.searchValue = filterValue([...oldSearchComponentValue, ...defaultSearchComponentValue]);
          setTimeout(() => {
            this.$emit('on-change', defaultSearchValue);
            this.$emit('input', defaultSearchValue);
          });
        });
      },
      /**
       * @desc 按 API 格式要求处理筛选值
       * @params {Object} payload 筛选结果
       */
      handleChange(payload) {
        const validValue = payload;
        const result = {};
        const defaultValueMap = val => val.id;
        const trim = valueTarget => (_.isString(valueTarget) ? _.trim(valueTarget) : valueTarget);
        validValue.forEach((currentValue) => {
          const valueMap = typeof currentValue.map === 'function' ? currentValue.map : defaultValueMap;
          if (currentValue.multiable) {
            result[currentValue.id] = currentValue.values.map(item => trim(valueMap(item)));
          } else if (currentValue.values) {
            result[currentValue.id] = trim(valueMap(currentValue.values[0]));
          } else {
            result[currentValue.id] = trim(valueMap(currentValue));
          }
        });
        // 缓存用户筛选的数据
        const userFieldArr = ['operator', 'creator', 'lastModifyUser'];
        userFieldArr.forEach((userField) => {
          if (_.has(result, userField)) {
            userSearchCache.setItem(result[userField]);
          }
        });

        this.$emit('on-change', result);
        this.$emit('input', result);
      },
      /**
       * @desc 外部调用——检查搜索条件是否为空
       * @returns {Boolean}
       */
      checkEmpty() {
        return this.$refs.searchSelect.chipList.length < 1;
      },
      /**
       * @desc 外部调用——重置筛选条件
       */
      reset() {
        this.searchValue = [];
        this.$refs.searchSelect.reset();
      },
    },
  };
</script>
<style lang="postcss">
  .jb-search-select {
    z-index: 999;
    background: #fff;

    textarea {
      &::placeholder {
        color: #c4c6cc;
      }
    }
  }
</style>
