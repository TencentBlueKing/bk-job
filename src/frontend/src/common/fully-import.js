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
import bkMagicVue from 'bk-magic-vue';
import VueProgressBar from 'vue-progressbar';
import VueCompositionAPI from '@vue/composition-api';
import AuthRouterLink from '@components/auth/router-link';
import AuthButton from '@components/auth/button';
import AuthOption from '@components/auth/option';
import AuthCompontent from '@components/auth/component';
import Empty from '@components/empty';
import LowerComponent from '@components/lower-component';
import Exception from '@/components/exception';
import Icon from '@components/icon';
import JbDiff from '@components/jb-diff';
import JbRouterView from '@components/jb-router-view';
import JbForm from '@components/jb-form';
import JbFormItem from '@components/jb-form/item';
import JbInput from '@components/jb-input';
import JbPopoverConfirm from '@components/jb-popover-confirm';
import JbSideslider from '@components/jb-sideslider';
import JbDialog from '@components/jb-dialog';
import JbBreadcrumb from '@components/jb-breadcrumb';
import JbBreadcrumbItem from '@components/jb-breadcrumb/jb-breadcrumb-item';
import ScrollFaker from '@components/scroll-faker';
import SmartAction from '@components/smart-action';
import ElementTeleport from '@components/element-teleport';
import Request from '@components/request';
import Cursor from '@components/cursor';
import TippyTips from '@components/tippy-tips';
import 'bk-magic-vue/dist/bk-magic-vue.min.css';

Vue.use(bkMagicVue);
Vue.use(VueProgressBar, {
    color: '#3A84FF',
    failedColor: 'red',
    height: '2px',
    inverse: false,
    transition: {
        speed: '0.1s',
        opacity: '0.6s',
        termination: 100,
    },
    position: 'absolute',
    autoFinish: false,
});
Vue.use(VueCompositionAPI);

Vue.component('AuthRouterLink', AuthRouterLink);
Vue.component('AuthButton', AuthButton);
Vue.component('AuthOption', AuthOption);
Vue.component('AuthComponent', AuthCompontent);
Vue.component('Empty', Empty);
Vue.component('LowerComponent', LowerComponent);
Vue.component('AppException', Exception);
Vue.component('Icon', Icon);
Vue.component('JbDiff', JbDiff);
Vue.component('JbRouterView', JbRouterView);
Vue.component('JbForm', JbForm);
Vue.component('JbFormItem', JbFormItem);
Vue.component('JbInput', JbInput);
Vue.component('JbPopoverConfirm', JbPopoverConfirm);
Vue.component('JbSideslider', JbSideslider);
Vue.component('JbDialog', JbDialog);
Vue.component('JbBreadcrumb', JbBreadcrumb);
Vue.component('JbBreadcrumbItem', JbBreadcrumbItem);
Vue.component('ScrollFaker', ScrollFaker);
Vue.component('SmartAction', SmartAction);
Vue.component('ElementTeleport', ElementTeleport);
Vue.use(Cursor);
Vue.use(Request);
Vue.use(TippyTips);
