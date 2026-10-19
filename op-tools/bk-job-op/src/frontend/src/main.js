import Vue from 'vue';
// .babelrc 中的 import-bk-magic-vue 会将具名导入转换为按需引入，
// Vue 实例方法需要显式挂载；样式统一由 dist 下的整包 CSS 提供
import bkMagic from 'bk-magic-vue';
import { bkInfoBox, bkMessage } from 'bk-magic-vue';
import 'bk-magic-vue/dist/bk-magic-vue.min.css';

import App from './App.vue';
import router from './router';
import './css/reset.css';

Vue.use(bkMagic);
Vue.prototype.$bkInfo = bkInfoBox;
Vue.prototype.$bkMessage = bkMessage;

new Vue({
  router,
  render: h => h(App),
}).$mount('#app');
