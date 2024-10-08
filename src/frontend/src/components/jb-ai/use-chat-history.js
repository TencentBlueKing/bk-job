import { onMounted, reactive, ref } from 'vue';

import AiService from '@service/ai';

export default () => {
  const hasMore = ref(true);
  const isLoadingMore = ref(false);
  const isLoading = ref(false);
  const messageList = ref([]);

  const pagination = reactive({
    start: 0,
    length: 15,
  });

  const fetchLatestChatHistoryList = () => {
    isLoading.value = true;
    AiService.fetchLatestChatHistoryList(pagination)
      .then((result) => {
        messageList.value = result;
        hasMore.value = result.length < pagination.length;
      })
      .finally(() => {
        isLoading.value = false;
      });
  };

  const loadMore = () => {
    pagination.start += pagination.length;
    isLoadingMore.value = true;
    AiService.fetchLatestChatHistoryList(pagination)
      .then((result) => {
        hasMore.value = result.length === pagination.length;
        messageList.value = [...result, ...messageList.value];
      })
      .finally(() => {
        isLoadingMore.value = false;
      });
  };

  onMounted(() => {
    fetchLatestChatHistoryList();
  });

  return {
    messageList,
    loading: isLoading,
    hasMore,
    loadingMore: isLoadingMore,
    pagination,
    loadMore,
  };
};
