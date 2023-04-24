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

import { ordinalSuffixOf } from '@utils/assist';

const BATCH_STATUS_RUNNING = 2;
const BATCH_STATUS_SUCCESS = 3;
const BATCH_STATUS_FAIL = 4;
const BATCH_STATUS_INGORE_ERROR = 6;
const BATCH_STATUS_MANUAL_CONFIRM = 7;

export default {
  functional: true,
  props: {
    selectBatch: Number,
    data: Object,
    currentRunningBatch: Number,
  },
  render(h, context) {
    const {
      data,
      selectBatch,
      currentRunningBatch,
    } = context.props;

    const active = data.batch === selectBatch;
    const will = data.batch > currentRunningBatch;

    const clasess = {
      'batch-item': true,
      active,
      will,
      confirm: data.status === BATCH_STATUS_MANUAL_CONFIRM,
      fail: data.status === BATCH_STATUS_FAIL,
    };

    const handleClickSelect = (event) => {
      context.listeners['on-change']
             && context.listeners['on-change'](data.batch, event);
    };

    const renderSuccessIcon = () => {
      if (![
        BATCH_STATUS_SUCCESS,
        BATCH_STATUS_INGORE_ERROR,
      ].includes(data.status)) {
        return null;
      }
      if (data.batch === selectBatch) {
        return (
          <div class="batch-item-status">
            <Icon type="check-line" style="color: #2dc89d" />
          </div>
        );
      }
      return null;
    };

    const renderConfirmIcon = () => {
      if (data.status !== BATCH_STATUS_MANUAL_CONFIRM) {
        return null;
      }
      if (data.batch === selectBatch
                || data.batch === currentRunningBatch) {
        return (
          <div class="batch-item-status">
            <Icon type="stop-2" style="color: #FF9C01" />
          </div>
        );
      }
      return null;
    };

    const renderFailedIcon = () => {
      if (![
        BATCH_STATUS_FAIL,
      ].includes(data.status)) {
        return null;
      }
      if (data.batch === selectBatch
                || data.batch === currentRunningBatch) {
        return (
          <div class="batch-item-status">
            <Icon type="wrong" style="color: #FF5656" />
          </div>
        );
      }
      return null;
    };

    const renderExecutingIcon = () => {
      if (data.batch === currentRunningBatch
                && data.status === BATCH_STATUS_RUNNING) {
        return (
          <div class="batch-item-status rotate-loading">
            <Icon type="batch-loading" style="color: #3a84ff" />
          </div>
        );
      }
      return null;
    };

    const renderText = context.parent.$i18n.locale === 'en-US'
      ? ordinalSuffixOf(data.batch)
      : `第 ${data.batch} 批`;

    return (
      <div
        class={clasess}
        key={data.batch}
        onClick={handleClickSelect}>
        {renderText}
        {renderSuccessIcon()}
        {renderConfirmIcon()}
        {renderFailedIcon()}
        {renderExecutingIcon()}
      </div>
    );
  },
};
