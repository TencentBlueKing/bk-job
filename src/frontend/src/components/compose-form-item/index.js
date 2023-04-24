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

import './styles.css';

export default {
  props: {
    headBackgroundColor: {
      type: String,
      default: '#FAFBFD',
    },
    tailBackgroundColor: String,
  },
  render(h) {
    const childrenArr = this.$slots.default;

    if (childrenArr.length > 1) {
      const childrenLength = childrenArr.length;
      let startIndex = 0;
      let headChildren = null;
      while (startIndex < childrenLength) {
        headChildren = childrenArr[startIndex];
        if (headChildren.tag && headChildren.componentOptions) {
          break;
        }
        startIndex += 1;
      }
      let tailChildren = null;
      let endIndex = childrenLength - 1;
      while (endIndex >= 0 && endIndex) {
        tailChildren = childrenArr[endIndex];
        if (tailChildren.tag && tailChildren.componentOptions) {
          break;
        }
        endIndex -= 1;
      }

      if (headChildren && tailChildren && headChildren !== tailChildren) {
        let firstChildStaticClass = 'compose-form-item-first';
        if (headChildren.data.staticClass) {
          firstChildStaticClass += ` ${headChildren.data.staticClass}`;
        }
        if (this.headBackgroundColor) {
          headChildren.data.style = Object.assign(headChildren.data.style || {}, {
            'background-color': this.headBackgroundColor,
          });
        }
        headChildren.data.staticClass = firstChildStaticClass;

        let lastChildStaticClass = 'compose-form-item-last';
        if (tailChildren.data.staticClass) {
          lastChildStaticClass += ` ${tailChildren.data.staticClass}`;
        }
        if (this.tailBackgroundColor) {
          tailChildren.data.style = Object.assign(tailChildren.data.style || {}, {
            'background-color': this.tailBackgroundColor,
          });
        }
        tailChildren.data.staticClass = lastChildStaticClass;
      }
    }
    return h('div', {
      staticClass: 'compose-form-item',
    }, childrenArr);
  },
};
