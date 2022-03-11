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

package com.tencent.bk.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

class ReplaceEditionTextTask extends DefaultTask {

    @InputFiles
    public FileCollection processResourcesOutputFiles;
    @Input
    public String srcStr = null;
    @Input
    public String targetStr = null;
    @Input
    public String defaultEdition = "ce";
    @Input
    public String defaultPackageType = "allInOne";
    Set<String> patternSet = new HashSet<>();

    @Inject
    public ReplaceEditionTextTask() {
    }

    @TaskAction
    public void replace() {
        Project project = getProject();
        String edition = "";
        String packageType = "";
        if (project.hasProperty("job.edition")) {
            edition = (String) project.property("job.edition");
        } else {
            edition = defaultEdition;
        }
        if (project.hasProperty("job.package.type")) {
            packageType = (String) project.property("job.package.type");
        } else {
            packageType = defaultPackageType;
        }
        System.out.println("edition=" + edition);
        System.out.println("packageType=" + packageType);
        if ("ee".equals(edition)) {
            replaceEditionText();
        } else {
            System.out.println("Skip ReplaceEditionTextTask, set build param edition=ee to enable");
        }
        System.out.println("ReplaceEditionTextTask Done");
    }

    private boolean match(String pattern, String targetStr) {
        if (!pattern.startsWith("^")) {
            pattern = "^" + pattern;
        }
        if (!pattern.endsWith("$")) {
            pattern += "$";
        }
        return Pattern.matches(pattern, targetStr);
    }

    private void replaceEditionText() {
        for (File file : processResourcesOutputFiles) {
            String absPath = file.getAbsolutePath();
            System.out.println("check file:" + absPath);
            String fileName = file.getName();
            for (String pattern : patternSet) {
                if (match(pattern, fileName)) {
                    System.out.println(fileName + " matched pattern:" + pattern);
                    System.out.println("processing " + fileName);
                    replaceStrToFile(file);
                    break;
                }
            }
        }
    }

    private void replaceStrToFile(File file) {
        String content = readFileAsString(file);
        content = content.replaceAll(srcStr, targetStr);
        writeStringToFile(content, file);
    }

    private String readFileAsString(File file) {
        StringBuilder contentBuilder = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            String line = null;

            while ((line = br.readLine()) != null) {
                contentBuilder.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return contentBuilder.toString();
    }

    private void writeStringToFile(String str, File file) {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            bw.write(str);
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void process(String pattern) {
        patternSet.add(pattern);
    }

    public FileCollection getProcessResourcesOutputFiles() {
        return processResourcesOutputFiles;
    }

    public void setProcessResourcesOutputFiles(FileCollection processResourcesOutputFiles) {
        this.processResourcesOutputFiles = processResourcesOutputFiles;
    }

    public String getSrcStr() {
        return srcStr;
    }

    public void setSrcStr(String srcStr) {
        this.srcStr = srcStr;
    }

    public String getTargetStr() {
        return targetStr;
    }

    public void setTargetStr(String targetStr) {
        this.targetStr = targetStr;
    }

    public String getDefaultEdition() {
        return defaultEdition;
    }

    public void setDefaultEdition(String defaultEdition) {
        this.defaultEdition = defaultEdition;
    }

    public String getDefaultPackageType() {
        return defaultPackageType;
    }

    public void setDefaultPackageType(String defaultPackageType) {
        this.defaultPackageType = defaultPackageType;
    }
}
