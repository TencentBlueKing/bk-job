import { onMounted, ref } from 'vue';

import AiService from '@service/ai';

export default () => {
  const loading = ref(false);
  const messageList = ref([]);

  const apiPath = `${window.PROJECT_CONFIG.AJAX_URL_PREFIX}/job-analysis/web/ai/scope/${window.PROJECT_CONFIG.SCOPE_TYPE}/${window.PROJECT_CONFIG.SCOPE_ID}`;

  const fetchLatestChatHistoryList = () => {
    loading.value = true;
    AiService.fetchLatestChatHistoryList()
      .then((result) => {
        messageList.value = result;
      })
      .finally(() => {
        loading.value = false;
      });
  };

  const fetchContent = (recordId) => {
    loading.value = true;
    const latestChatMessage = {
      role: 'assistant',
      content: '内容正在生成中...',
      status: 'loading',
    };
    messageList.value.push(latestChatMessage);

    fetch(`${apiPath}/chatStream`, {
      method: 'POST',
      credentials: 'include',
      body: JSON.stringify({
        recordId,
      }),
      headers: {
        'Content-Type': 'application/json',
      },
    })
      .then(response => new Promise((resolve, reject) => {
        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');

        latestChatMessage.content = '';

        let fragment = '';
        // 递归函数来读取数据
        const read = () => {
          reader.read()
            .then(({ done, value }) => {
              if (done) {
                latestChatMessage.status = 'success';
                return resolve();
              }

              // 解码二进制数据块并添加到内容字符串
              const chunk = `${fragment}${decoder.decode(value, { stream: true })}`;

              chunk.split('\n')
                .forEach((item) => {
                  try {
                    latestChatMessage.content += (item ? JSON.parse(item).data.content : '');
                  } catch {
                    fragment += item;
                  }
                });

              // 递归读取下一块数据
              read();
            })
            .catch(error => reject(error));
        };

        // 开始读取流
        read();
      }))
      .then(() => {
        fetchLatestChatHistoryList();
      })
      .catch((error) => {
        console.error('Fetch error:', error);
        latestChatMessage.status = 'error';
        messageList.value = [...messageList.value];
        loading.value = false;
      });
  };

  onMounted(() => {
    fetchLatestChatHistoryList();
  });

  return {
    loading,
    messageList,
    fetchContent,
  };
};
