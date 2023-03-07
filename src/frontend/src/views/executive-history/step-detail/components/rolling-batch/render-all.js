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
  functional: true,
  props: {
    stepData: Object,
    isTotalBtnFixed: Boolean,
    selectBatch: Number,
  },
  render(h, context) {
    const {
      stepData,
      isTotalBtnFixed,
      selectBatch,
    } = context.props;

    // 滚动全量执行(滚动步骤)不支持查看全部批次操作
    const disabled = stepData.runMode === 2;

    const classes = {
      'all-btn': true,
      fixed: isTotalBtnFixed,
      active: selectBatch === 0,
      disabled,
    };

    const handleClick = () => {
      if (disabled) {
        return;
      }
      context.listeners['on-change'] && context.listeners['on-change'](0);
    };

    return (
      <div
        ref="allBtn"
        class={classes}
        key="all"
        onClick={handleClick}>
        {context.parent.$t('history.全部批次')}
      </div>
    );
  },
};
