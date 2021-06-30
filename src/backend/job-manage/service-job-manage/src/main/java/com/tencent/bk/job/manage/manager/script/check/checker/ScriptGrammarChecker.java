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

package com.tencent.bk.job.manage.manager.script.check.checker;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.StopWatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bash 语法检查器
 */
@Slf4j
public class ScriptGrammarChecker implements ScriptChecker {
    private ScriptTypeEnum type;
    private String content;

    public ScriptGrammarChecker(ScriptTypeEnum type, String content) {
        this.type = type;
        this.content = content;
    }

    @Override
    public List<ScriptCheckResultItemDTO> call() throws Exception {
        StopWatch watch = new StopWatch();
        watch.start("checkGrammar");

        File tmpFile = File.createTempFile(JobUUID.getUUID(), type.getName());
        FileUtils.write(tmpFile, content, "UTF-8");

        String cmdline = "bash -n " + tmpFile.getAbsolutePath();
        // 执行命令，读取命令输出，并转换为字符串
        Process process = Runtime.getRuntime().exec(cmdline);
        Map<Integer, ScriptCheckResultItemDTO> result = Maps.newHashMap();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream(),
            StandardCharsets.UTF_8))) {
            String retLine;
            Pattern lineMatch = Pattern.compile(".*?line (\\d+):(.*)");
            int lineCount = 0;
            while ((retLine = br.readLine()) != null) {
                lineCount += 1;
                log.debug("error line {}:{}", lineCount, retLine);
                if (retLine.length() == 0) {
                    continue;
                }

                final ScriptCheckResultItemDTO checkResult = new ScriptCheckResultItemDTO();
                Matcher matcher = lineMatch.matcher(retLine);
                checkResult.setLevel(level());
                if (matcher.find()) {
                    String line = matcher.group(1);
                    checkResult.setLine(Integer.parseInt(line));
                    checkResult.setDescription(matcher.group(2));
                }

                if (result.containsKey(checkResult.getLine())) {
                    ScriptCheckResultItemDTO old = result.get(checkResult.getLine());
                    old.setDescription(old.getDescription() + "\n" + checkResult.getDescription());
                } else {
                    result.put(checkResult.getLine(), checkResult);
                }
            }

        } catch (IOException e) {
            log.error("Check script grammar fail", e);
        } finally {
            tmpFile.deleteOnExit();
            watch.stop();
            log.debug("watch={}", watch);
        }
        ArrayList<ScriptCheckResultItemDTO> checkResults = Lists.newArrayList();
        checkResults.addAll(result.values());

        return checkResults;
    }

    @Override
    public ScriptCheckErrorLevelEnum level() {
        return ScriptCheckErrorLevelEnum.ERROR;
    }
}
