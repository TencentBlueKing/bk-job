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

package com.tencent.bk.job.execute.engine.gse;

import com.tencent.bk.gse.taskapi.api_script_file;
import com.tencent.bk.gse.taskapi.api_script_request;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

/**
 * GSE API 请求打印
 */
public class GseRequestPrinter {
    /**
     * 脚本任务ToString
     *
     * @param scriptRequest 脚本任务请求
     * @return 脚本任务ToString
     */
    public static String simplifyScriptRequest(api_script_request scriptRequest) {
        StringBuilder sb = new StringBuilder("api_script_request(");
        List<api_script_file> scripts = scriptRequest.getScripts();
        sb.append(scriptFilesToString(scripts));
        sb.append(",tasks:");
        if (scriptRequest.getTasks() == null) {
            sb.append("[]");
        } else {
            sb.append(scriptRequest.getTasks());
        }
        sb.append(")");
        return sb.toString();
    }

    private static String scriptFilesToString(List<api_script_file> scripts) {
        if (CollectionUtils.isEmpty(scripts)) {
            return "scripts:[]";
        }
        StringBuilder sb = new StringBuilder("scripts[");
        boolean first = true;
        for (api_script_file script : scripts) {
            if (first) {
                first = false;
                sb.append(scriptFileToString(script));
            } else {
                sb.append(",");
                sb.append(scriptFileToString(script));
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private static String scriptFileToString(api_script_file scriptFile) {
        StringBuilder sb = new StringBuilder("api_script_file(");
        sb.append("name:").append(scriptFile.getName());
        sb.append(",md5:").append(scriptFile.getMd5());
        sb.append(",download_path:").append(scriptFile.getDownload_path());
        sb.append(",content_length:").append(scriptFile.getContent() == null ? 0 : scriptFile.getContent().length());
        sb.append(")");
        return sb.toString();
    }

}
