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

/* eslint-disable max-len */
const path = require('path');
const fs = require('fs');

const ignoreList = [
    path.join(__dirname, './lib'),
    path.join(__dirname, './node_modules'),
    path.join(__dirname, './src/css/icon-cool'),
    path.join(__dirname, './src/utils/cron/parser'),
];

const javascriptCopyright = [
    '/*',
    ' * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.',
    ' *',
    ' * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.',
    ' *',
    ' * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.',
    ' *',
    ' * License for BK-JOB蓝鲸智云作业平台:',
    ' *',
    ' * ---------------------------------------------------',
    ' * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated',
    ' * documentation files (the "Software"), to deal in the Software without restriction, including without limitation',
    ' * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and',
    ' * to permit persons to whom the Software is furnished to do so, subject to the following conditions:',
    ' *',
    ' * The above copyright notice and this permission notice shall be included in all copies or substantial portions of',
    ' * the Software.',
    ' *',
    ' * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO',
    ' * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE',
    ' * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF',
    ' * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS',
    ' * IN THE SOFTWARE.',
    '*/',
];

const vueCopyright = [
    '<!--',
    ' * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.',
    ' *',
    ' * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.',
    ' *',
    ' * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.',
    ' *',
    ' * License for BK-JOB蓝鲸智云作业平台:',
    ' *',
    ' *',
    ' * Terms of the MIT License:',
    ' * ---------------------------------------------------',
    ' * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated',
    ' * documentation files (the "Software"), to deal in the Software without restriction, including without limitation',
    ' * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and',
    ' * to permit persons to whom the Software is furnished to do so, subject to the following conditions:',
    ' *',
    ' * The above copyright notice and this permission notice shall be included in all copies or substantial portions of',
    ' * the Software.',
    ' *',
    ' * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT',
    ' * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE',
    ' * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF',
    ' * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS',
    ' * IN THE SOFTWARE.',
    '-->',
];

const lincense = 'BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.';

const readFileLines = (filePath, callback) => {
    const fileContent = fs.readFileSync(filePath, {
        encoding: 'utf8',
    });
    const lines = fileContent.split(/\n/);
    if (lines.length < 1) {
        return;
    }
    callback(lines, fileContent.indexOf(lincense) > 0);
};
const writeFileLines = (target, copyright, fileLines) => {
    fs.writeFileSync(target, `${copyright.join('\n')}\n\n${fileLines.join('\n')}`);
};

const copyright = (target) => {
    if (ignoreList.includes(target)) {
        return;
    }
    const state = fs.statSync(target);
    if (state.isFile()) {
        if (/.js$/.test(target)) {
            console.log(`JAVASCRIPT: ${target}`);
            // javascript 文件
            readFileLines(target, (fileLines, oldLincense) => {
                let endIndex = 0; // copyright end
                if (/\/\*/.test(fileLines[0]) && oldLincense) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < fileLines.length; i++) {
                        if (/\*\//.test(fileLines[i])) {
                            endIndex = i + 2;
                            break;
                        }
                    }
                }
                writeFileLines(target, javascriptCopyright, fileLines.slice(endIndex));
            });
        } else if (/.(vue|html)$/.test(target)) {
            console.log(`VUE: ${target}`);
            // vue 文件
            readFileLines(target, (fileLines, oldLincense) => {
                let endIndex = 0; // copyright end
                if (/<!--/.test(fileLines[0]) && oldLincense) {
                    // eslint-disable-next-line no-plusplus
                    for (let i = 0; i < fileLines.length; i++) {
                        if (/-->/.test(fileLines[i])) {
                            endIndex = i + 2;
                            break;
                        }
                    }
                }
                writeFileLines(target, vueCopyright, fileLines.slice(endIndex));
            });
        }
    } else if (state.isDirectory()) {
        const dirList = fs.readdirSync(target);
        dirList.forEach((item) => {
            copyright(path.join(target, item));
        });
    }
};

copyright(path.join(__dirname, './'));
