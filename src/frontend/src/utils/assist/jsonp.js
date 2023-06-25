export const jsonp = (url) => {
  const scriptEl = document.createElement('script');
  scriptEl.src = url;
  document.body.appendChild(scriptEl);
  const removeCallback = () => {
    scriptEl.onload = null;
    scriptEl.onerror = null;
    document.body.removeChild(scriptEl);
  };
  scriptEl.onload = removeCallback;
  scriptEl.onerror = removeCallback;
};
