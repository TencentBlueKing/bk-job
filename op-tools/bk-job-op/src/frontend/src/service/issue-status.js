import request from './request';

/**
 * 分页查询 Issue 状态列表
 *
 * @param {Object} params idKeyword / start / pageSize
 * @param {AbortSignal} [signal] 用于取消在途请求
 * @returns {Promise<{total: number, start: number, size: number, data: Array}>}
 */
export const listIssueStatus = (params, signal) => request.get('/issueStatus/list', {
  params: {
    idKeyword: params.idKeyword || undefined,
    start: params.start,
    pageSize: params.pageSize,
  },
  signal,
});
