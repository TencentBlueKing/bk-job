import { ref } from 'vue';

import I18n from '@/i18n';

export default (messageList) => {
  const loading = ref(false);

  const apiPath = `${window.PROJECT_CONFIG.AJAX_URL_PREFIX}/job-analysis/web/ai/scope/${window.PROJECT_CONFIG.SCOPE_TYPE}/${window.PROJECT_CONFIG.SCOPE_ID}`;

  const fetchContent = (recordId) => {
    loading.value = true;
    const latestChatMessage = {
      role: 'assistant',
      content: I18n.t('请稍等，我正在处理您的请求...'),
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
      .then((response) => {
        if (!response.ok) {
          throw new Error(`HTTP 错误！状态：${response.status}`);
        }

        return new Promise((resolve, reject) => {
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
                  latestChatMessage.content = `${latestChatMessage.content}`;
                  return resolve();
                }

                // 解码二进制数据块并添加到内容字符串
                const chunk = `${fragment}${decoder.decode(value, { stream: true })}`;

                chunk.split('\n')
                  .forEach((item) => {
                    try {
                      const chunkData = JSON.parse(item).data;
                      latestChatMessage.time = chunkData.time;
                      latestChatMessage.content += chunkData.content;
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
        });
      })
      .catch((error) => {
        console.error('Fetch error:', error);
        latestChatMessage.content = I18n.t('抱歉！出了点问题，请稍后再试。');
        latestChatMessage.status = 'error';
      })
      .finally(() => {
        loading.value = false;
      });
  };

  return {
    loading,
    fetchContent,
  };
};
