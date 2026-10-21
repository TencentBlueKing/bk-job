const STATUS_CONFIG = {
  WAIT_FOR_REVIEW: {
    text: '等待评审',
    theme: 'warning',
  },
  HANDLING: {
    text: '正在处理',
    theme: 'primary',
  },
  MERGED: {
    text: '已合入',
    theme: 'success',
  },
  CLOSED: {
    text: '已关闭',
    theme: 'default',
  },
};

export const pullRequestStatusText = status => (
  STATUS_CONFIG[status] ? STATUS_CONFIG[status].text : status
);

export const pullRequestStatusTheme = status => (
  STATUS_CONFIG[status] ? STATUS_CONFIG[status].theme : 'default'
);
