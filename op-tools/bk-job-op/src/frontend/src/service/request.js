import axios from 'axios';

import router from '../router';

/**
 * 未登录 / 无权限的业务异常。
 * 轮询层据此决定「停止轮询并跳转」，而不是当作普通失败走退避重试
 */
export class AuthError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'AuthError';
    this.status = status;
  }
}

const request = axios.create({
  baseURL: '/',
  timeout: 15000,
  // 同域部署，浏览器自动带 bk_ticket Cookie
  withCredentials: true,
});

const extractMessage = (data, fallback) => {
  if (data && typeof data === 'object' && data.message) {
    return data.message;
  }
  return fallback;
};

request.interceptors.response.use(
  response => response.data,
  (error) => {
    const response = error.response;
    if (!response) {
      return Promise.reject(new Error('网络异常或服务无响应'));
    }
    // 未登录：后端对 XHR 返回 401 + JSON（而不是 302），由前端顶层跳转到登录页
    if (response.status === 401) {
      const loginUrl = response.data && response.data.loginUrl;
      if (loginUrl) {
        window.location.href = loginUrl;
      }
      return Promise.reject(new AuthError(401, extractMessage(response.data, '登录态已失效，请重新登录')));
    }
    // 非白名单用户
    if (response.status === 403) {
      const message = extractMessage(response.data, '当前用户无权访问，请联系管理员加入白名单');
      if (router.currentRoute.path !== '/no-permission') {
        router.replace({
          path: '/no-permission',
          query: { message },
        });
      }
      return Promise.reject(new AuthError(403, message));
    }
    return Promise.reject(new Error(extractMessage(response.data, `请求失败（HTTP ${response.status}）`)));
  },
);

export default request;
