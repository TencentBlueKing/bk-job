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

import _ from 'lodash';
import Vue, {
  customRef,
} from 'vue';
import VueRouter from 'vue-router';

import {
  leaveConfirm,
} from '@utils/assist';
import {
  routerCache,
} from '@utils/cache-helper';

import NotFound from '@views/404';
import AccountManage from '@views/account-manage/routes';
import BusinessPermission from '@views/business-permission';
import CronJob from '@views/cron-job/routes';
import DangerousRuleManage from '@views/dangerous-rule-manage/routes';
import Dashboard from '@views/dashboard/routes';
import DetectRecords from '@views/detect-records/routes';
import ExecutiveHistory from '@views/executive-history/routes';
import FastExecution from '@views/fast-execution/routes';
import FileManage from '@views/file-manage/routes';
import GlobalSetting from '@views/global-setting/routes';
import Home from '@views/home/routes';
import Entry from '@views/index';
import NotifyManage from '@views/notify-manage/routes';
import PlanManage from '@views/plan-manage/routes';
import PublicScriptManage from '@views/public-script-manage/routes';
import ScriptManage from '@views/script-manage/routes';
import ScriptTemplate from '@views/script-template/routes';
import ServiceState from '@views/service-state/routes';
import TagManage from '@views/tag-manage/routes';
import TaskManage from '@views/task-manage/routes';
import TicketManage from '@views/ticket-manage/routes';
import WhiteIP from '@views/white-ip/routes';

Vue.use(VueRouter);

let router;
let lastRouterHrefCache = '/';

const renderPageWithComponent = (route, component) => {
  if (route.component) {
    // eslint-disable-next-line no-param-reassign
    route.component = component;
  }
  if (route.children) {
    route.children.forEach((item) => {
      renderPageWithComponent(item);
    });
  }
};

export default ({ appList, isAdmin, scopeType, scopeId }) => {
  // scope 是否有效
  let isValidScope = false;
  // scope 是有有权限查看
  let hasScopePermission = false;

  const appInfo = appList.find(_ => _.scopeType === scopeType && _.scopeId === scopeId);
  // scope 存在于业务列表中——有效的 scope
  if (appInfo) {
    isValidScope = true;
    // scope 存在于业务列表中——有权限访问
    if (appInfo.hasPermission) {
      hasScopePermission = true;
    }
  }

  const systemManageRoute = [
    Dashboard,
    ScriptTemplate,
  ];

  // 生成路由配置
  const routes = [
    {
      path: '/',
      component: Entry,
      redirect: {
        name: 'home',
      },
      children: systemManageRoute,
    },
    {
      path: `/${scopeType}/${scopeId}`,
      component: Entry,
      redirect: {
        name: 'home',
      },
      children: [
        AccountManage,
        NotifyManage,
        Home,
        TaskManage,
        PlanManage,
        FastExecution,
        ScriptManage,
        CronJob,
        ExecutiveHistory,
        FileManage,
        TicketManage,
        TagManage,
      ],
    },
    {
      path: '/api_(execute|plan)/:id+',
      component: {
        render() {
          return this._e(); // eslint-disable-line no-underscore-dangle
        },
      },
    },
    {
      path: '*',
      name: '404',
      component: NotFound,
    },
  ];

  if (!isValidScope) {
    renderPageWithComponent(routes[1], NotFound);
  } else if (!hasScopePermission) {
    renderPageWithComponent(routes[1], BusinessPermission);
  }

  // admin用户拥有系统设置功能
  if (isAdmin) {
    systemManageRoute.push(PublicScriptManage);
    systemManageRoute.push(WhiteIP);
    systemManageRoute.push(GlobalSetting);
    systemManageRoute.push(ServiceState);
    systemManageRoute.push(DangerousRuleManage);
    systemManageRoute.push(DetectRecords);
  }

  router = new VueRouter({
    mode: 'history',
    routes,
    scrollBehavior() {
      return {
        x: 0, y: 0,
      };
    },
  });

  const routerPush = router.push;
  const routerReplace = router.replace;

  // window.routerFlashBack === true 时查找路由缓存参数
  const routerFlaskBack = (params, currentRoute) => {
    /* eslint-disable no-param-reassign */
    params = _.cloneDeep(params);
    if (window.routerFlashBack) {
      // 路由回退
      const query = routerCache.getItem(params.name);
      if (query) {
        params.query = {
          ...query,
          ...params.query || {},
        };
      }
    } else {
      routerCache.clearItem(params.name);
    }
    lastRouterHrefCache = router.resolve(params).href;
    return params;
  };
  const leaveConfirmHandler = (currentRoute) => {
    // 在业务逻辑中可以找到当前路由的 meta 挂在自定义 leavaConfirm
    if (Object.prototype.hasOwnProperty.call(currentRoute, 'meta')
            && Object.prototype.hasOwnProperty.call(currentRoute.meta, 'leavaConfirm')
            && typeof currentRoute.meta.leavaConfirm === 'function') {
      return currentRoute.meta.leavaConfirm();
    }
    return leaveConfirm();
  };
    // 路由切换时
    // 检测页面数据的编辑状态——弹出确认框提示用户确认
    // 如果需要路由回溯（window.routerFlashBack === true）查找缓存是否有跳转目标的路由缓存数据
  router.push = (params, callback = () => {}) => {
    const { currentRoute } = router;
    // 检测当前路由自定义离开确认交互
    leaveConfirmHandler(currentRoute).then(() => {
      routerPush.call(router, routerFlaskBack(params, currentRoute));
      window.routerFlashBack = false;
    }, () => {
      callback();
    });
  };
  // 路由切换时
  // 检测页面数据的编辑状态——弹出确认框提示用户确认
  // 如果需要路由回溯（window.routerFlashBack === true）查找缓存是否有跳转目标的路由缓存数据
  router.replace = (params, callback = () => {}) => {
    // 检测当前路由自定义离开确认交互
    const { currentRoute } = router;
    leaveConfirmHandler(currentRoute).then(() => {
      routerReplace.call(router, routerFlaskBack(params, currentRoute));
      window.routerFlashBack = false;
    }, () => {
      callback();
    });
  };
  // 异步路由加载失败刷新页面
  router.onError((error) => {
    if (/Loading chunk (\d*) failed/.test(error.message)) {
      window.location.href = lastRouterHrefCache;
    }
  });

  router.afterEach(() => {
    history.pushState(null, null, document.URL);
    const callback = () => {
      leaveConfirm()
        .then(() => {
          window.removeEventListener('popstate', callback);
          window.history.go(-2);
        })
        .catch(() => {
          history.pushState(null, null, document.URL);
        });
    };
    window.addEventListener('popstate', callback);

    const currentRoute = _.last(router.currentRoute.matched);
    if (currentRoute && currentRoute.instances.default) {
      const routerDefault = currentRoute.instances.default;
      setTimeout(() => {
        routerDefault.$once('hook:beforeDestroy', () => {
          window.removeEventListener('popstate', callback);
        });
      });
    }
  });
  return router;
};

let isRouteWatch = false;
export const useRoute = () => customRef((track, trigger) => ({
  get() {
    setTimeout(() => {
      if (isRouteWatch) {
        return;
      }
      window.BKApp.$watch('$route', () => {
        trigger();
      });
      isRouteWatch = true;
    });
    track();
    return router.currentRoute;
  },
}));

export const useRouter = () => router;
