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

import bkMagicVue from 'bk-magic-vue';
import marked from 'marked';
import Vue from 'vue';
import VueProgressBar from 'vue-progressbar';

import CustomSettingsService from '@service/custom-settings';
import HostManageService from '@service/host-manage';
import QueryGlobalSettingService from '@service/query-global-setting';

import AuthButton from '@components/auth/button';
import AuthCompontent from '@components/auth/component';
import AuthOption from '@components/auth/option';
import AuthRouterLink from '@components/auth/router-link';
import Cursor from '@components/cursor';
import ElementTeleport from '@components/element-teleport';
import Empty from '@components/empty';
import Exception from '@components/exception';
import Icon from '@components/icon';
// import createIpSelector from '@components/ip-selector/index.js';
import JbBreadcrumb from '@components/jb-breadcrumb';
import JbBreadcrumbItem from '@components/jb-breadcrumb/jb-breadcrumb-item';
import JbDialog from '@components/jb-dialog';
import JbDiff from '@components/jb-diff';
import JbForm from '@components/jb-form';
import JbFormItem from '@components/jb-form/item';
import JbInput from '@components/jb-input';
import jbInputNumber from '@components/jb-input-number';
import JbPopoverConfirm from '@components/jb-popover-confirm';
import JbRouterView from '@components/jb-router-view';
import JbSideslider from '@components/jb-sideslider';
import JbTextarea from '@components/jb-textarea';
import LowerComponent from '@components/lower-component';
import Request from '@components/request';
import ResizeableBox from '@components/resizeable-box';
import ScrollFaker from '@components/scroll-faker';
import SmartAction from '@components/smart-action';
import Test from '@components/test';
import TippyTips from '@components/tippy-tips';

import createIpSelector from '@blueking/ip-selector';

import 'bk-magic-vue/dist/bk-magic-vue.min.css';
import '@blueking/ip-selector/dist/styles/vue2.6.x.css';

const IpSelector = createIpSelector({
  version: '7',
  panelList: ['staticTopo', 'dynamicTopo', 'dynamicGroup', 'manualInput'],
  unqiuePanelValue: false,
  nameStyle: 'camelCase',
  hostTableDefaultSelectAllMode: true,
  hostSearchSelect: true,
  // hostTableCustomColumnList: [
  //     {
  //         key: 'a1',
  //         label: '测试列一',
  //         index: 3,
  //         field: 'a1',
  //         width: '100px',
  //         renderHead: h => h('span', '测试列一header'),
  //         renderCell: h => h('span', 'render a1'),
  //     },
  //     {
  //         key: 'A2',
  //         label: '测试列二',
  //         field: 'A2',
  //         index: 4,
  //         width: '100px',
  //         renderHead: h => h('span', '测试列二header'),
  //         renderCell: h => h('span', 'render test1'),
  //     },
  // ],
  // hostTableRenderColumnList: ['ip', 'alive', 'osName', 'ipv6', 'cloudArea'],
  // hostMemuExtends: [
  //     {
  //         name: '复制更多一',
  //         action: () => {
  //             console.log('复制更多一');
  //         },
  //     },
  //     {
  //         name: '复制更多二',
  //         action: () => {
  //             console.log('复制更多二');
  //         },
  //     },
  // ],
  fetchTopologyHostCount: HostManageService.fetchTopologyWithCount,
  fetchTopologyHostsNodes: HostManageService.fetchTopologyHost,
  fetchTopologyHostIdsNodes: HostManageService.fetchTopogyHostIdList,
  fetchHostsDetails: HostManageService.fetchHostInfoByHostId,
  fetchHostCheck: HostManageService.fetchInputParseHostList,
  fetchNodesQueryPath: HostManageService.fetchNodePath,
  fetchHostAgentStatisticsNodes: HostManageService.fetchBatchNodeAgentStatistics,
  fetchDynamicGroups: HostManageService.fetchDynamicGroup,
  fetchHostsDynamicGroup: HostManageService.fetchDynamicGroupHost,
  fetchHostAgentStatisticsDynamicGroups: HostManageService.fetchBatchGroupAgentStatistics,
  fetchCustomSettings: CustomSettingsService.fetchAll,
  updateCustomSettings: CustomSettingsService.update,
  fetchConfig: () => QueryGlobalSettingService.fetchRelatedSystemUrls()
    .then(data => ({
      // eslint-disable-next-line max-len
      bk_cmdb_dynamic_group_url: `${data.BK_CMDB_ROOT_URL}/#/business/${window.PROJECT_CONFIG.SCOPE_ID}/custom-query`,
      // eslint-disable-next-line max-len
      bk_cmdb_static_topo_url: `${data.BK_CMDB_ROOT_URL}/#/business/${window.PROJECT_CONFIG.SCOPE_ID}/custom-query`,
    })),
});

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
Vue.component('JbInputNumber', jbInputNumber);
Vue.component('JbTextarea', JbTextarea);
Vue.component('JbPopoverConfirm', JbPopoverConfirm);
Vue.component('JbSideslider', JbSideslider);
Vue.component('JbDialog', JbDialog);
Vue.component('JbBreadcrumb', JbBreadcrumb);
Vue.component('JbBreadcrumbItem', JbBreadcrumbItem);
Vue.component('ScrollFaker', ScrollFaker);
Vue.component('SmartAction', SmartAction);
Vue.component('ElementTeleport', ElementTeleport);
Vue.component('ResizeableBox', ResizeableBox);
Vue.component('IpSelector', IpSelector);

Vue.use(Cursor);
Vue.use(Request);
Vue.use(TippyTips);
Vue.use(Test);

marked.use({
  renderer: {
    link(link, title, text) {
      return `<a href="${link}" target="_blank">${text}</a>`;
    },
  },
});
