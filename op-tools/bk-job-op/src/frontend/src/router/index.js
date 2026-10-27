import Vue from 'vue';
import VueRouter from 'vue-router';

import IssueStatusList from '../views/IssueStatusList.vue';
import NoPermission from '../views/NoPermission.vue';

Vue.use(VueRouter);

// history 模式依赖后端把这些路由回落到 index.html（WebMvcConfig#addViewControllers）
const router = new VueRouter({
  mode: 'history',
  base: '/',
  routes: [
    {
      path: '/',
      redirect: '/issue-status',
    },
    {
      path: '/issue-status',
      name: 'issueStatus',
      component: IssueStatusList,
    },
    {
      path: '/no-permission',
      name: 'noPermission',
      component: NoPermission,
    },
    {
      path: '*',
      redirect: '/issue-status',
    },
  ],
});

export default router;
