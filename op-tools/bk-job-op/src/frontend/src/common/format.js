export const EMPTY_TEXT = '--';

const pad = value => String(value).padStart(2, '0');

/**
 * 毫秒时间戳格式化为 yyyy-MM-dd HH:mm:ss（本地时区）。
 * 存量数据没有 updateTime，为 null 时显示 --
 *
 * @param {number|null|undefined} timestamp
 * @returns {string}
 */
export const formatTime = (timestamp) => {
  if (timestamp === null || timestamp === undefined || timestamp === '') {
    return EMPTY_TEXT;
  }
  const time = Number(timestamp);
  if (!Number.isFinite(time) || time <= 0) {
    return EMPTY_TEXT;
  }
  const date = new Date(time);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
};

/**
 * 空值统一渲染为 --
 */
export const displayText = value => (value === null || value === undefined || value === '' ? EMPTY_TEXT : value);

/**
 * id 形如 issue_{数字} 时推导出 GitHub Issue 链接，否则返回空字符串
 */
export const resolveIssueUrl = (id) => {
  if (!id) {
    return '';
  }
  const matched = /^issue_(\d+)$/.exec(String(id).trim());
  return matched ? `https://github.com/TencentBlueKing/bk-job/issues/${matched[1]}` : '';
};
