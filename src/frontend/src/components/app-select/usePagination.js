import _ from 'lodash';
import { computed, onBeforeUnmount, reactive, watch } from 'vue';

export default (scrollEle, loadingEle, list) => {
  const pagination = reactive({
    total: 0,
    current: 1,
    pageSize: 20,
  });


  const renderData = computed(() => list.value.slice(0, pagination.current * pagination.pageSize));

  const handleScroll = _.throttle(() => {
    if (pagination.current * pagination.pageSize >= pagination.total) {
      return;
    }
    const {
      bottom: containerBottom,
    } = scrollEle.value.getBoundingClientRect();
    const {
      bottom: loadingBottom,
    } = loadingEle.value.getBoundingClientRect();

    if (loadingBottom - 60 < containerBottom) {
      pagination.current = pagination.current + 1;
    }
  }, 60);

  watch([scrollEle, loadingEle], () => {
    if (scrollEle.value && loadingEle.value) {
      scrollEle.value?.addEventListener('scroll', handleScroll);
    }
  });

  watch(list, () => {
    pagination.total = list.value.length;
    pagination.current = 1;
  }, {
    immediate: true,
  });

  onBeforeUnmount(() => {
    scrollEle.value?.removeEventListener('scroll', handleScroll);
  });

  return {
    data: renderData,
    pagination,
  };
};
