import request from './request';

export const listPullRequests = (params, signal) => request.get('/web/pullRequest/list', {
  params: {
    start: params.start,
    pageSize: params.pageSize,
  },
  signal,
});

export const deletePullRequest = prId => request.post('/web/pullRequest/delete', { prId });
