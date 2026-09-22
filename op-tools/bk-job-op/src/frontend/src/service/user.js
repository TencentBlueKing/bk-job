import request from './request';

/**
 * 查询当前登录用户信息
 *
 * @returns {Promise<{username: string}>}
 */
export const getUserInfo = () => request.get('/web/userInfo');
