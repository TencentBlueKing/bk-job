export const downloadUrl = (url) => {
  // 创建隐藏的可下载链接

  const eleLink = document.createElement('a');
  eleLink.style.display = 'none';
  eleLink.href = url;

  // 触发点击
  document.body.appendChild(eleLink);
  const { changeAlert } = window;
  window.changeFlag = false;
  eleLink.click();
  setTimeout(() => {
    window.changeFlag = changeAlert;
  });

  // 然后移除
  document.body.removeChild(eleLink);
};
