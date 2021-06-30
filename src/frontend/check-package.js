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

const xlsx = require('node-xlsx').default;
const fs = require('fs');
const path = require('path');

const licenseMap = {
    MIT: 'https://opensource.org/licenses/MIT',
    ISC: 'https://opensource.org/licenses/ISC',
    BSD: 'https://opensource.org/licenses/0BSD',
    'BSD-1-Clause': 'https://opensource.org/licenses/BSD-1-Clause',
    'BSD-2-Clause': 'https://opensource.org/licenses/BSD-2-Clause',
    'BSD-3-Clause': 'https://opensource.org/licenses/BSD-3-Clause',
    'Apache-2.0': 'https://opensource.org/licenses/Apache-2.0',
    WTFPL: 'https://directory.fsf.org/wiki/License:WTFPL-2',
    'CC-BY-3.0': 'https://directory.fsf.org/wiki/License:CC0',
    'CC0-1.0': 'https://directory.fsf.org/wiki/License:CC0',
    unlicense: 'https://choosealicense.com/licenses/unlicense/',
};

const sheetData = [
    [
        '开源软件名称',
        '开源软件版本号',
        '开源软件的下载链接地址',
        '是否对开源软件做出修改',
        '是否对开源软件进行了分发',
        '开源协议',
        'Copyright@<year> <owner>',
        '开源协议的链接地址',
        '备注',
    ],
];

const nodeModulesPath = path.resolve(__dirname, './node_modules');

const getLicenseLink = (license) => {
    if (licenseMap[license]) {
        return licenseMap[license];
    }
    // eslint-disable-next-line no-restricted-syntax
    for (const key in licenseMap) {
        const reg = new RegExp(key, 'i');
        if (reg.test(license)) {
            return licenseMap[key];
        }
    }
    return '';
};
const isHideDir = target => /^\./.test(target);
const isScopedDir = target => /^@/.test(target);

const checkPackage = (entryPath) => {
    const packages = fs.readdirSync(entryPath);
    // eslint-disable-next-line no-plusplus
    for (let i = 0; i < packages.length; i++) {
        const currentDir = packages[i];
        if (isHideDir(currentDir)) {
            continue;
        }
        if (isScopedDir(currentDir)) {
            checkPackage(path.resolve(entryPath, currentDir));
            continue;
        }
        const packageJsonStr = fs.readFileSync(path.resolve(entryPath, currentDir, './package.json'));

        const packageJson = JSON.parse(packageJsonStr.toString());

        const {
            name,
            version,
            repository,
        } = packageJson;

        let { license, homepage } = packageJson;

        if (typeof license === 'object') {
            license = license.type;
        }

        let repositoryUrl = '';
        if (/^https?:\/\/github.com/.test(homepage)) {
            const hashIndex = homepage.indexOf('#');
            repositoryUrl = hashIndex > -1 ? homepage.slice(0, hashIndex) : homepage;
        } else if (typeof repository === 'object' && repository.url) {
            if (/^git/.test(repository.url)) {
                repositoryUrl = repository.url.substr(4);
            } else {
                repositoryUrl = repository.url;
            }
        }

        if (!homepage) {
            homepage = repositoryUrl;
        }
            
        let licenseUrl = getLicenseLink(license);
        if (!licenseUrl) {
            licenseUrl = homepage;
            if (fs.existsSync(path.resolve(entryPath, currentDir, './LICENSE.md'))
                || fs.existsSync(path.resolve(entryPath, currentDir, './LICENSE'))) {
                licenseUrl = `${repositoryUrl}/blob/master/LICENSE`;
            }
        }

        let remark = '';
        if (/@blueking/.test(name) || (/@tencent/.test(name) && /bk-/.test(name))) {
            remark = '蓝鲸';
        }
            
        sheetData.push([
            name,
            version,
            homepage || `https://www.npmjs.com/package/${name}`,
            '否',
            '否',
            '',
            '',
            '',
            remark,
        ]);
    }
};

checkPackage(nodeModulesPath);

const options = {
    '!cols': [
        { wch: 50 },
        { wch: 20 },
        { wch: 20 },
        { wch: 30 },
        { wch: 70 },
        { wch: 70 },
        { wch: 30 },
    ],
};

const buffer = xlsx.build([{
    name: 'sheet',
    data: sheetData,
}], options);

fs.writeFile('./job_frontend_library.xlsx', buffer, (err) => {
    if (err) {
        console.log(err);
    }
    fs.unlink('./~$job_frontend_library.xlsx', (error) => {
        console.log(error);
    });
});
