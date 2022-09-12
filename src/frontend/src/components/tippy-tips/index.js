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

import tippy from 'bk-magic-vue/lib/utils/tippy';
import _ from 'lodash';

const defaultOptions = {
    duration: 0,
    arrow: true,
};
export default {
    install (Vue) {
        Vue.mixin({
            updated: _.debounce(() => {
                const $els = document.body.querySelectorAll('[tippy-tips]');
                $els.forEach(($current) => {
                    const content = $current.getAttribute('tippy-tips');
                    if (!content) {
                        return;
                    }
                    const placement = $current.getAttribute('placement') || 'top';
                    if (!$current._tippy) { // eslint-disable-line no-underscore-dangle
                        tippy($current, {
                            ...defaultOptions,
                            content,
                            placement,
                        });
                    }
                });
            }, 300),
            beforeDestroy: _.debounce(function () {
                if (!this.$el || !this.$el.querySelectorAll) {
                    return;
                }
                const $els = this.$el.querySelectorAll('[tippy-tips]');
                $els.forEach(($current) => {
                    if ($current._tippy) { // eslint-disable-line no-underscore-dangle
                        $current._tippy.destroy(); // eslint-disable-line no-underscore-dangle
                    }
                });
            }, 300, {
                leading: true,
                trailing: false,
            }),
        });
    },
};
