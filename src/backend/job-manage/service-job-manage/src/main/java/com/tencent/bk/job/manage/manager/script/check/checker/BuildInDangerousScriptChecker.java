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
import com.google.common.collect.Sets;
import com.tencent.bk.job.manage.common.consts.script.ScriptCheckErrorLevelEnum;
import com.tencent.bk.job.manage.manager.script.check.ScriptCheckParam;
import com.tencent.bk.job.manage.model.dto.ScriptCheckResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tencent.bk.job.manage.manager.script.check.consts.ScriptCheckItemCode.DANGER_RM_FORCE_ALL;


/**
 * Job内置高危脚本检查
 */
@Slf4j
public class BuildInDangerousScriptChecker extends DefaultChecker {

    private static final Pattern rm = Pattern.compile("^((\\s*?[^#r].*)(;|[|]{1,2}|[&]{1,2}))?(\\s*)(?<rm>rm(\\s+(-" +
        "([rfv])+))*\\s+(?<dir>[\"']?(\\s*[\"']?[$]?[{]?[^;&|\"'\\s}]+[}]?[\"']?)+[\"']?))[;&|\\s]*");
    private static final Set<String> sysDirs =
        Sets.newHashSet("/", "/bin",
            "/boot",
            "/dev",
            "/etc",
            "/home",
            "/initrd",
            "/lib",
            "/lib32",
            "/lib64",
            "/proc",
            "/root",
            "/sbin",
            "/sys",
            "/usr",
            "/usr/bin",
            "/usr/include",
            "/usr/lib",
            "/usr/local",
            "/usr/local/bin",
            "/usr/local/include",
            "/usr/local/sbin",
            "/usr/local/share",
            "/usr/sbin",
            "/usr/share",
            "/usr/src",
            "/var",
            "/data",
            "/data/home",
            "/data1",
            "/data1/mysqldata",
            "/data1/mongodata");


    public BuildInDangerousScriptChecker(ScriptCheckParam param) {
        super(param);
    }

    @Override
    public ScriptCheckErrorLevelEnum level() {
        return ScriptCheckErrorLevelEnum.ERROR;
    }

    @Override
    public List<ScriptCheckResultItemDTO> call() {
        StopWatch watch = new StopWatch();
        ArrayList<ScriptCheckResultItemDTO> checkResults = Lists.newArrayList();
        try {
            String[] lines = param.getLines();
            watch.start("checkRM");
            checkRM(checkResults, lines);
            watch.stop();
        } finally {
            log.debug("watch={}", watch);
        }
        return checkResults;
    }

    private void checkRM(ArrayList<ScriptCheckResultItemDTO> checkResults, String[] lines) {
        Matcher matcher;
        int lineNumber = 1;
        while (lineNumber <= lines.length) {
            String lineContent = lines[lineNumber - 1];
            if (isComment(param.getScriptType(), lineContent) || StringUtils.isBlank(lineContent)) {
                lineNumber++;
                continue;
            }

            matcher = rm.matcher(lineContent);
            while (matcher.find()) {
                String dir = matcher.group("dir").replaceAll("[\"]", "").trim();
                if (dir.contains(" ")) {
                    String[] dirs = dir.split(" ");
                    for (String tDir : dirs) {
                        if (dangerDir(tDir.trim())) {
                            ScriptCheckResultItemDTO rm = createResult(lineNumber, DANGER_RM_FORCE_ALL, null,
                                lineContent, lineContent);
                            if (rm != null) {
                                checkResults.add(rm);
                                break;
                            }
                        }
                    }
                } else if (dir.contains("$")) {
                    Matcher p = Pattern.compile("[$]?[{]?[^${}]+[}]?").matcher(dir);
                    StringBuilder fullPath = new StringBuilder(dir.length());
                    while (p.find()) {
                        fullPath.append(trimDir(p.group().trim()));
                    }
                    if (dangerDir(fullPath.toString())) {
                        checkResults.add(createResult(lineNumber, DANGER_RM_FORCE_ALL,
                            null, lineContent, lineContent));
                    }
                } else {
                    if (dangerDir(dir)) {
                        checkResults.add(createResult(lineNumber, DANGER_RM_FORCE_ALL,
                            null, lineContent, lineContent));
                    }
                }
            }
            lineNumber++;
        }
    }

    private String trimDir(String dir) {
        if (dir.length() > 1) {
            dir = dir.replaceAll("/{2,}", "/");
            if (dir.startsWith("$")) {
                dir = param.getVarMap().get(dir.replaceAll("[${}]", ""));
                if (dir == null) {
                    return "";
                }
            } else if (dir.equals("/*")) {
                dir = "/";
            } else if (dir.endsWith("/") || dir.endsWith("/*")) {
                dir = dir.substring(0, dir.lastIndexOf("/"));
            } else if (dir.endsWith("\\") || dir.endsWith("\\*")) {
                dir = dir.substring(0, dir.lastIndexOf("\\"));
            }
            if (dir.contains("/"))
                dir = dirLoop(dir);
        }
        return dir;
    }

    private boolean dangerDir(String dir) {
        return sysDirs.contains(trimDir(dir));
    }

    /**
     * 解析 /home/../bin 或者 /./bin 这种循环返回上一级的情况转换成正确的目录
     * eg: /home/../bin  =  /bin
     * /./bin = /bin
     * 会排除 dir2 这种只有相对路径并且只有一级的目录参数，并立即返回。
     *
     * @param dir 目录
     * @return 会排除 dir2 这种只有相对路径并且只有一级的目录参数，并立即返回。
     */
    private String dirLoop(String dir) {
        StringBuilder dirs = new StringBuilder();
        // 排除非目录写法的dir参数： 目录写法 /dir1/dir2 之种，排除 dir2 这种只有相对路径并且只有一级的。
        if (dir.contains("/") && (dir.contains("..") || dir.contains("."))) {
            Pattern dirLoopFind = Pattern.compile("/[^/]+");
            Matcher matcher = dirLoopFind.matcher(dir);
            Stack<String> dirComp = new Stack<>();
            int deep = 0;
            while (matcher.find()) {
                String currDir = matcher.group().replaceAll("[\\\\\\s]", "");

                if (currDir.equals("/..")) {
                    if (deep > 0) {
                        deep--;
                        dirComp.pop();
                    }
                } else if (!currDir.equals("/.")) {
                    dirComp.push(currDir);
                    deep++;
                }
            }
            if (deep <= 0) {
                dirs.append("/");
            } else {

                for (int i = 0; i < deep; i++) {
                    dirs.append(dirComp.get(i));
                }
            }
        } else {
            return dir;
        }
        return dirs.toString();
    }
}
