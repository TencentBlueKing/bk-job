/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
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

package com.tencent.bk.job.analysis.service.ai;

import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface ChatService {

    /**
     * 与AI聊天并处理聊天记录保存等逻辑
     *
     * @param username  用户名
     * @param appId     Job业务ID
     * @param userInput 用户输入
     * @return AI对话记录
     */
    AIChatRecord chatWithAI(String username, Long appId, String userInput);

    /**
     * 获取最近的聊天记录列表
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近的聊天记录列表
     */
    List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length);

    /**
     * 获取聊天流式数据
     *
     * @param username 用户名
     * @param recordId 对话记录ID
     * @return 流式数据
     */
    StreamingResponseBody generateChatStream(String username, Long recordId);

    /**
     * 终止当前实例中的对话
     *
     * @param username 用户名
     * @param recordId 对话记录ID
     * @return 是否终止成功
     */
    boolean terminateChat(String username, Long recordId);

    /**
     * 触发终止对话
     *
     * @param username 用户名
     * @param recordId 对话记录ID
     * @return 是否终止成功
     */
    boolean triggerTerminateChat(String username, Long recordId);
}
