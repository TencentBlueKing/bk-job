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

import Vue from 'vue';
import I18n from '@/i18n';

export const scriptErrorAlert = () => {
    let confirmInfo = null;
    const handleClose = () => {
        confirmInfo.close();
    };
    const vm = new Vue();
    const h = vm.$createElement;
    confirmInfo = vm.$bkInfo({
        title: I18n.t('脚本中出现高危语句'),
        showFooter: false,
        width: 450,
        subHeader: (h => (
            <div>
                <div style="font-size: 14px; line-height: 22px; color: #63656E; text-align: center">
                    <span>{ I18n.t('请按脚本编辑框左侧') } </span>
                    <img src="/static/images/ace-editor/error-tips.png" style="width: 14px; vertical-align: middle;" />
                    <span> { I18n.t('图标的提示处理后重试') }</span>
                </div>
                <div style="padding: 24px 0 21px; text-align: center">
                    <bk-button
                        onClick={handleClose}
                        style="width: 96px"
                        theme="primary">
                        { I18n.t('好的') }
                    </bk-button>
                </div>
            </div>
        ))(h),
    });
};
