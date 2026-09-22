import Vue from 'vue';
// bk-magic-vue 采用全量引入：本页面组件用量少，全量引入比按需引入少一层构建期不确定性，
// 样式统一由 dist 下的整包 CSS 提供（按需引入的组件级样式加载在此工程中不启用）
import bkMagic from 'bk-magic-vue';
import 'bk-magic-vue/dist/bk-magic-vue.min.css';

import App from './App.vue';
import router from './router';
import './css/reset.css';

Vue.use(bkMagic);

new Vue({
  router,
  render: h => h(App),
}).$mount('#app');
