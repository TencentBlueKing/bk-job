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
import { computed, onBeforeUnmount, reactive, watch } from 'vue';

export default (scrollEle, loadingEle, list) => {
  const pagination = reactive({
    total: 0,
    current: 1,
    pageSize: 20,
  });


  const renderData = computed(() => list.value.slice(0, pagination.current * pagination.pageSize));

  const handleScroll = _.throttle(() => {
    if (pagination.current * pagination.pageSize >= pagination.total) {
      return;
    }
    const {
      bottom: containerBottom,
    } = scrollEle.value.getBoundingClientRect();
    const {
      bottom: loadingBottom,
    } = loadingEle.value.getBoundingClientRect();

    if (loadingBottom - 60 < containerBottom) {
      pagination.current = pagination.current + 1;
    }
  }, 60);

  watch([scrollEle, loadingEle], () => {
    if (scrollEle.value && loadingEle.value) {
      scrollEle.value?.addEventListener('scroll', handleScroll);
    }
  });

  watch(list, () => {
    pagination.total = list.value.length;
    pagination.current = 1;
  }, {
    immediate: true,
  });

  onBeforeUnmount(() => {
    scrollEle.value?.removeEventListener('scroll', handleScroll);
  });

  return {
    data: renderData,
    pagination,
  };
};
