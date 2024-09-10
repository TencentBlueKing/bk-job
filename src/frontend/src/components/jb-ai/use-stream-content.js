import { ref } from 'vue';


export default (messageList) => {
  const loading = ref(false);

  const apiPath = `${window.PROJECT_CONFIG.AJAX_URL_PREFIX}/job-analysis/web/ai/scope/${window.PROJECT_CONFIG.SCOPE_TYPE}/${window.PROJECT_CONFIG.SCOPE_ID}`;

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
        });
      })
      .catch((error) => {
        console.error('Fetch error:', error);
        latestChatMessage.content = '内容生成失败！';
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
