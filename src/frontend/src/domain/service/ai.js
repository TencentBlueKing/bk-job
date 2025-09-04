/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

import AiSource from '../source/ai';

export default {
  fetchAnalyzeError(params = {}) {
    return AiSource.getAnalyzeError(params)
      .then(({ data }) => data);
  },

  fetchCheckScript(params = {}) {
    return AiSource.getCheckScript(params)
      .then(({ data }) => data);
  },

  fetchConfig(params = {}) {
    return AiSource.getConfig(params)
      .then(({ data }) => data);
  },

  fetchGeneraChat(params = {}) {
    return AiSource.getGeneraChat(params)
      .then(({ data }) => data);
  },

  fetchLatestChatHistoryList(params = {}) {
    return AiSource.getLatestChatHistoryList(params)
      .then(({ data }) => data.reduce((result, item) => {
        if (item.userInput.content) {
          result.push({
            role: 'user',
            content: item.userInput.content,
            status: '',
            time: item.userInput.time,
          });
        }

        result.push({
          role: 'assistant',
          content: item.aiAnswer.content,
          status: item.aiAnswer.errorCode === '0' ? 'success' : 'error',
          time: item.aiAnswer.time,
        });

        return result;
      }, []));
  },
  deleteChatHistory() {
    return AiSource.deleteChatHistory();
  },
  fetchChatStream(params) {
    return AiSource.getChatStream(params);
  },
  terminateChat(params) {
    return AiSource.terminateChat(params);
  },
};
