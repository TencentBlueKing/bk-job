/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

export const findUsedVariable = (list) => {
    const variableSet = new Set();
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < list.length; i++) {
        const step = list[i];
        if (step.isScript) {
            // 执行脚本步骤
            const { executeTarget, scriptParam } = step.scriptStepInfo;
            // 1，脚本执行的执行目标使用全局变量
            if (executeTarget.variable) {
                variableSet.add(executeTarget.variable);
            }
            // 2，脚本执行的脚本参数使用全局变量
            const patt = /\${([^}]+)}/g;
            let scriptParamMatch;
            while ((scriptParamMatch = patt.exec(scriptParam)) !== null) {
                variableSet.add(scriptParamMatch[1]);
            }
            continue;
        }
        if (step.isFile) {
            // 分发文件步骤
            const { fileDestination, fileSourceList, destinationFileLocation } = step.fileStepInfo;
            // 检测目标服务器
            const { path, server } = fileDestination;
            if (server.variable) {
                variableSet.add(server.variable);
            }
            // 检测目标路径
            if (path) {
                const patt = /\${([^}]+)}/g;
                let destinationFileLocationMatch;
                while ((destinationFileLocationMatch = patt.exec(destinationFileLocation)) !== null) {
                    variableSet.add(destinationFileLocationMatch[1]);
                }
            }
            // 检测服务器源文件
            // eslint-disable-next-line no-plusplus
            for (let j = 0; j < fileSourceList.length; j++) {
                const currentFile = fileSourceList[j];
                if (currentFile.fileType === 1) {
                    // 服务器文件_服务器列表
                    if (currentFile.host.variable) {
                        variableSet.add(currentFile.host.variable);
                    }
                    // 服务器文件_来源路径
                    currentFile.fileLocation.forEach((fileLocation) => {
                        const patt = /\${([^}]+)}/g;
                        let fileLocationMatch;
                        while ((fileLocationMatch = patt.exec(fileLocation)) !== null) {
                            variableSet.add(fileLocationMatch[1]);
                        }
                    });
                }
            }
            
            continue;
        }
    }
    return [
        ...variableSet,
    ];
};

export const isPublicScript = (route) => {
    const { meta } = route;
    if (!meta) {
        return false;
    }
    if (meta.public) {
        return true;
    }
    return false;
};

export const checkPublicScript = (route) => {
    const { meta } = route;
    if (!meta) {
        return false;
    }
    if (meta.public) {
        return true;
    }
    return false;
};
