/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

export default {
  methods: {
    scrollActiveToView(event) {
      // 上下键位移动选中
      if (![
        38, 40,
      ].includes(event.keyCode)) {
        return;
      }
      if (event.keyCode === 38) {
        // 上移
        this.activeIndex -= 1;
      } else if (event.keyCode === 40) {
        // 下移
        this.activeIndex += 1;
      }
      if (this.activeIndex >= this.list.length) {
        this.activeIndex = this.list.length - 1;
      }
      if (this.activeIndex < 0) {
        this.activeIndex = 0;
      }

      const $list = this.$refs.list;
      this.$nextTick(() => {
        const wraperHeight = $list.getBoundingClientRect().height;
        const activeOffsetTop = $list.querySelector('.active').offsetTop + 34;
        if (activeOffsetTop > wraperHeight) {
          $list.scrollTop = activeOffsetTop - wraperHeight + 10;
        } else if (activeOffsetTop <= 42) {
          $list.scrollTop = 0;
        }
      });
    },
  },
};
