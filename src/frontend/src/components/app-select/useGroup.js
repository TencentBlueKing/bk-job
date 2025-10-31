import _ from 'lodash';
import { onBeforeUnmount, watch } from 'vue';

export default (scrollEle, expandScopeGroupMap, list, isShowSelectPanel) => {
  const handleScroll = _.throttle(() => {
    const groupElList = Array.from(scrollEle.value?.querySelectorAll('.group-item') || []);
    const { top: listTop, bottom: listBottom } = scrollEle.value.getBoundingClientRect();
    const stickyIndexMap = {};
    groupElList.forEach((groupEl, index) => {
      const { top } = groupEl.getBoundingClientRect();
      if (top - 32 * index < listTop) {
        groupEl.style.position = 'sticky';
        groupEl.style.top = `${32 * index}px`;
        groupEl.style.bottom = '';
        stickyIndexMap[index] = true;
      }
    });
    _.reverse(groupElList).forEach((groupEl, index) => {
      const { bottom } = groupEl.getBoundingClientRect();
      if (bottom + 32 * index > listBottom) {
        groupEl.style.position = 'sticky';
        groupEl.style.top = '';
        groupEl.style.bottom = `${32 * index}px`;
        stickyIndexMap[groupElList.length - 1 - index] = true;
      }
    });
  }, 60);

  watch(scrollEle, () => {
    if (scrollEle.value) {
      scrollEle.value?.addEventListener('scroll', handleScroll);
    }
  });
  watch([expandScopeGroupMap, list, isShowSelectPanel], () => {
    setTimeout(() => {
      handleScroll();
    }, 60);
  }, {
    immediate: true,
  });


  onBeforeUnmount(() => {
    scrollEle.value?.removeEventListener('scroll', handleScroll);
  });
};
