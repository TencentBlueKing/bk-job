/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

import _ from 'lodash';
import {
  computed,
  getCurrentInstance,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  shallowRef,
  watch,
} from 'vue';

import {
  getOffset,
  makeMap,
} from '@utils/assist';

export default (props, columnList, allShowColumn) => {
  const currentInstance = getCurrentInstance();

  const listRef = ref();
  const list = shallowRef([]);
  const pagination = reactive({
    page: 1,
    pageSize: 0,
  });
  const tableMaxHeight = ref(0);
  const selectRowKey = ref('');
  const windowInnerWidth = ref(window.innerWidth);
  const positionLeftOffset = ref(0);

  const hasMore = computed(() => pagination.page * pagination.pageSize < props.total);

  const rootStyles = computed(() => {
    const rightLogWidth = 800;
    const paddingLeft = 24;
    const maxWidth = windowInnerWidth.value - positionLeftOffset.value - rightLogWidth - paddingLeft;
    const allShowColumnMap = makeMap(allShowColumn.value);
    const allShowColumnWidth = columnList.value.reduce((result, item) => {
      if (allShowColumnMap[item.name]) {
        return result + item.width;
      }
      return result;
    }, 65);

    return {
      width: `${Math.min(Math.max(allShowColumnWidth, 217), maxWidth)}px`,
    };
  });

  watch(() => props.name, () => {
    pagination.page = 1;
    selectRowKey.value = '';
  });

  const handleRowClick = (row) => {
    selectRowKey.value = row.key;
    currentInstance.proxy.$emit('on-change', row);
  };

  watch(() => props.data, () => {
    if (listRef.value) {
      positionLeftOffset.value = listRef.value.getBoundingClientRect().left;
    }
    // 切换分组时最新的分组数据一定来自API返回数据
    // listLoading为false说明是本地切换不更新列表
    if (!props.listLoading) {
      return;
    }
    list.value = props.data;

    if (props.data.length < 1) {
      handleRowClick({});
    } else if (!selectRowKey.value) {
      selectRowKey.value = props.data[0].key;
      handleRowClick(props.data[0]);
    }
  }, {
    immediate: true,
  });

  /**
   * @desc 根据屏幕高度计算单页 pageSize
   */
  const calcPageSize = () => {
    const { top } = getOffset(listRef.value);
    const windowHeight = window.innerHeight;
    const rowHeight = 40;
    const listHeight = windowHeight - top - 20;
    pagination.pageSize = parseInt(listHeight / rowHeight + 6, 10);
    currentInstance.proxy.$emit('on-pagination-change', pagination.pageSize);
  };

  /**
   * @desc 表格行的样式 class
   */
  const rowClassNameCallback = ({ row }) => (selectRowKey.value === row.key ? 'active' : '');
  /**
     * @desc 表格滚动到底部
     */
  const handleScrollEnd = () => {
    if (!hasMore.value) {
      return;
    }
    // 增加分页
    pagination.page = pagination.page + 1;
    currentInstance.proxy.$emit('on-pagination-change', pagination.page * pagination.pageSize);
  };

  /**
     * @desc 列表数据排序
     */
  const handleSortChange = ({ prop, order }) => {
    if (!order) {
      currentInstance.proxy.$emit('on-sort', {
        orderField: '',
        order: '',
      });
    } else {
      currentInstance.proxy.$emit('on-sort', {
        orderField: prop,
        order: order === 'descending' ? 0 : 1,
      });
    }

    currentInstance.proxy.$emit('on-pagination-change', pagination.pageSize);
  };
    /**
     * @desc 清空搜索
     */
  const handleClearSearch = () => {
    currentInstance.proxy.$emit('on-clear-search');
  };

  const handleWindowResize = _.throttle(() => {
    windowInnerWidth.value = window.innerWidth;
  }, 60);

  onMounted(() => {
    calcPageSize();
    tableMaxHeight.value = listRef.value.getBoundingClientRect().height;

    window.addEventListener('resize', handleWindowResize);
    onBeforeUnmount(() => {
      window.removeEventListener('resize', handleWindowResize);
    });
  });


  return {
    listRef,
    list,
    tableMaxHeight,
    selectRowKey,
    hasMore,
    rootStyles,
    rowClassNameCallback,
    handleRowClick,
    handleScrollEnd,
    handleSortChange,
    handleClearSearch,
  };
};
