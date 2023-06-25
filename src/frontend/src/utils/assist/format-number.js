export const formatNumber = (target, short = false) => {
  const format = val => `${val}`.replace(/(\d{1,3})(?=(\d{3})+$)/g, '$1,');
  if (!short) {
    return format(target);
  }
  if (target < 10000) {
    return format(target);
  }
  if (target < 1000000) {
    return `${parseFloat(format(target / 1000), 2)} K`;
  }
  return `${parseFloat(format(target / 1000000), 2)} M`;
};