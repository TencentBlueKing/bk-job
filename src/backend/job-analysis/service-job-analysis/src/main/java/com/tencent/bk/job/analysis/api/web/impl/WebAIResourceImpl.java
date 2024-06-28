/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.analysis.api.web.impl;

import com.tencent.bk.job.analysis.api.web.WebAIResource;
import com.tencent.bk.job.analysis.model.web.req.AIAnalyzeErrorReq;
import com.tencent.bk.job.analysis.model.web.req.AICheckScriptReq;
import com.tencent.bk.job.analysis.model.web.req.AIGeneralChatReq;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.model.web.resp.UserInput;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController("jobAnalysisWebAIResource")
@Slf4j
public class WebAIResourceImpl implements WebAIResource {
    @Override
    public Response<Map<String, Object>> getAIConfig(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId) {
        Map<String, Object> map = new HashMap<>();
        map.put("analyzeErrorLogMaxLength", 5 * 1024 * 1024L);
        return Response.buildSuccessResp(map);
    }

    @Override
    public Response<List<AIChatRecord>> getLatestChatHistoryList(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
                                                                 Integer start,
                                                                 Integer length) {
        List<AIChatRecord> aiChatRecordList = new ArrayList<>();
        AIChatRecord record = new AIChatRecord();
        UserInput userInput = new UserInput();
        userInput.setContent("你是谁");
        userInput.setTime(System.currentTimeMillis());
        record.setUserInput(userInput);
        ThreadUtils.sleep(1000);
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setContent("我是AI小鲸");
        aiAnswer.setErrorCode(0);
        aiAnswer.setErrorMessage(null);
        aiAnswer.setTime(System.currentTimeMillis());
        record.setAiAnswer(aiAnswer);
        aiChatRecordList.add(record);

        AIChatRecord record2 = new AIChatRecord();
        UserInput userInput2 = new UserInput();
        userInput2.setContent("Hello");
        userInput2.setTime(System.currentTimeMillis());
        record2.setUserInput(userInput2);
        ThreadUtils.sleep(1000);
        AIAnswer aiAnswer2 = new AIAnswer();
        aiAnswer2.setContent("World");
        aiAnswer2.setErrorCode(0);
        aiAnswer2.setErrorMessage(null);
        aiAnswer2.setTime(System.currentTimeMillis());
        record2.setAiAnswer(aiAnswer2);
        aiChatRecordList.add(record2);
        return Response.buildSuccessResp(aiChatRecordList);
    }

    @Override
    public Response<AIAnswer> generalChat(String username,
                                          AppResourceScope appResourceScope,
                                          String scopeType,
                                          String scopeId,
                                          AIGeneralChatReq req) {
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setContent("我是AI小鲸");
        aiAnswer.setErrorCode(0);
        aiAnswer.setErrorMessage(null);
        aiAnswer.setTime(System.currentTimeMillis());
        return Response.buildSuccessResp(aiAnswer);
    }

    @Override
    public Response<AIAnswer> checkScript(String username,
                                          AppResourceScope appResourceScope,
                                          String scopeType,
                                          String scopeId,
                                          AICheckScriptReq req) {
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setContent("没什么问题");
        aiAnswer.setErrorCode(0);
        aiAnswer.setErrorMessage(null);
        aiAnswer.setTime(System.currentTimeMillis());
        return Response.buildSuccessResp(aiAnswer);
    }

    @Override
    public Response<AIAnswer> analyzeError(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           AIAnalyzeErrorReq req) {
        AIAnswer aiAnswer = new AIAnswer();
        aiAnswer.setContent("让我想想...");
        aiAnswer.setErrorCode(0);
        aiAnswer.setErrorMessage(null);
        aiAnswer.setTime(System.currentTimeMillis());
        return Response.buildSuccessResp(aiAnswer);
    }
}
