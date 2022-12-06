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
import Store from '@/store';

/**
 * @desc 高危语句动作为扫描时
 */
const sacnDialog = (resolve, reject) => {
    let confirmInfo = null;
    const handleConfirm = () => {
        confirmInfo.close();
        resolve();
    };
    const handleCancel = () => {
        confirmInfo.close();
        reject(new Error('cancel'));
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
                    <span style="color: red; font-weight: bold">{ I18n.t('警告！') }</span>
                    <span>{ I18n.t('脚本中出现高危语句') }</span>
                    <span>（</span>
                    <span>{ I18n.t('详见编辑框左侧') } </span>
                    <img src="/static/images/ace-editor/error-tips.png" style="width: 14px; vertical-align: middle;" />
                    <span> { I18n.t('图标的提示') }</span>
                    <span>）</span>
                    <span> { I18n.t('请确定是否要继续操作？') }</span>
                </div>
                <div style="padding: 24px 0 21px; text-align: center">
                    <bk-button
                        onClick={handleConfirm}
                        class="mr10"
                        style="width: 96px"
                        theme="primary">
                        { I18n.t('确定继续') }
                    </bk-button>
                    <bk-button
                        onClick={handleCancel}
                        style="width: 96px">
                        { I18n.t('立即取消') }
                    </bk-button>
                </div>
            </div>
        ))(h),
        closeFn: handleCancel,
    });
};

/**
 * @desc 高危语句动作为拦截时
 */
const preventDialog = (resolve, reject) => {
    let confirmInfo = null;
    const handleClose = () => {
        confirmInfo.close();
        reject(new Error('cancel'));
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
        closeFn: handleClose,
    });
};

export const scriptErrorConfirm = () => new Promise((resolve, reject) => {
    const { scriptCheckError } = Store.state;
    if (!scriptCheckError) {
        return resolve();
    }

    if (scriptCheckError.isActionScan) {
        sacnDialog(resolve, reject);
    } else {
        preventDialog(resolve, reject);
    }
});
