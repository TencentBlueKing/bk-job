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
  namespaced: true,
  state: {
    bkAppCode: '', // appcode
    name: '蓝鲸作业平台', // 站点的名称，通常显示在页面左上角，也会出现在网页title中
    nameEn: 'BlueKing JOB', // 站点的名称-英文
    appLogo: '', // 站点logo
    favicon: '/static/images/favicon.icon', // 站点favicon
    helperText: '',
    helperTextEn: '',
    helperLink: '',
    brandImg: '',
    brandImgEn: '',
    brandName: '', // 品牌名，会用于拼接在站点名称后面显示在网页title中
    favIcon: '',
    brandNameEn: '', // 品牌名-英文
    footerInfo: '', // 页脚的内容，仅支持 a 的 markdown 内容格式
    footerInfoEn: '', // 页脚的内容-英文
    footerCopyright: '', // 版本信息，包含 version 变量，展示在页脚内容下方

    footerInfoHTML: '',
    footerInfoHTMLEn: '',
    footerCopyrightContent: '',

    // 需要国际化的字段，根据当前语言cookie自动匹配，页面中应该优先使用这里的字段
    i18n: {
      name: '',
      helperText: '...',
      brandImg: '...',
      brandName: '...',
      footerInfoHTML: '...',
    },
  },
  mutations: {
    update(state, value) {
      Object.keys(state).forEach((key) => {
        state[key] = value[key];
      });
    },
  },
};
