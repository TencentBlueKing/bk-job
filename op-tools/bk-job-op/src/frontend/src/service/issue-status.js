import request from './request';

/**
 * 分页查询 Issue 状态列表
 *
 * @param {Object} params idKeyword / start / pageSize
 * @param {AbortSignal} [signal] 用于取消在途请求
 * @returns {Promise<{total: number, start: number, size: number,
 *   data: Array<{id: string, status: string, statusDescription: string, updateTime: number,
 *   prStatus: string|null, prId: number|null}>}>}
 */
export const listIssueStatus = (params, signal) => request.get('/web/issueStatus/list', {
  params: {
    idKeyword: params.idKeyword || undefined,
    start: params.start,
    pageSize: params.pageSize,
  },
  signal,
});

/**
 * 删除指定 Issue 的状态数据
 *
 * @param {string} id Issue 标识
 * @returns {Promise<{deleted: number}>}
 */
export const deleteIssueStatus = id => request.post('/web/issueStatus/delete', { id });
