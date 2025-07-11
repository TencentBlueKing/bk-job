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

package com.tencent.bk.job.analysis.dao;

import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;

import java.util.List;

public interface AIChatHistoryDAO {
    /**
     * 插入聊天记录
     *
     * @param aiChatHistoryDTO AI聊天记录
     * @return 插入记录的id
     */
    Long insertAIChatHistory(AIChatHistoryDTO aiChatHistoryDTO);

    /**
     * 判断用户是否存在聊天记录
     *
     * @param username 用户名
     * @return 是否存在聊天记录
     */
    boolean existsChatHistory(String username);

    /**
     * 设置聊天记录状态
     *
     * @param historyId AI聊天记录ID
     * @param status    AI聊天记录状态
     * @return 受影响的行数
     */
    int updateChatHistoryStatus(Long historyId, int status);

    /**
     * 更新聊天记录状态
     *
     * @param historyId    AI聊天记录ID
     * @param status       AI聊天记录状态
     * @param aiAnswer     AI回答
     * @param errorCode    错误码
     * @param errorMessage 错误信息
     * @param aiAnswerTime AI回答时间
     * @return 受影响的行数
     */
    int updateChatHistoryStatusAndAIAnswer(Long historyId,
                                           Integer status,
                                           String aiAnswer,
                                           String errorCode,
                                           String errorMessage,
                                           Long aiAnswerTime);

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
     * 获取最近已完成的聊天记录列表
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近已完成的聊天记录列表
     */
    List<AIChatHistoryDTO> getLatestFinishedChatHistoryList(String username, Integer start, Integer length);

    /**
     * 获取聊天记录
     *
     * @param username 用户名
     * @param id       聊天记录ID
     * @return 聊天记录
     */
    AIChatHistoryDTO getChatHistory(String username, Long id);

    /**
     * 软删除聊天记录（优先删除创建时间较早的）
     *
     * @param username 用户名
     * @param limit    最大删除数量
     * @return 删除的记录条数
     */
    int softDeleteChatHistory(String username, Integer limit);

    /**
     * 硬删除id小于指定id的聊天记录（按id从小到大的顺序删除）
     *
     * @param maxStartTime 最大开始时间
     * @param limit        最大删除数量
     * @return 删除的记录条数
     */
    int deleteChatHistory(long maxStartTime, int limit);

    /**
     * 硬删除某个用户的id小于指定id的聊天记录（按id从小到大的顺序删除）
     *
     * @param username 用户名
     * @param maxId    最大id
     * @param limit    最大删除数量
     * @return 删除的记录条数
     */
    int deleteChatHistory(String username, long maxId, int limit);

    /**
     * 获取所有有聊天记录（未删除）的用户
     *
     * @return 所有有聊天记录（未删除）的用户
     */
    List<String> listAllUserOfChatHistory();

    /**
     * 获取offset后第一条记录的Id
     *
     * @param username 用户名
     * @param offset   偏移量
     * @return 第一条记录的Id
     */
    Long getFirstIdAfterOffset(String username, int offset);
}
