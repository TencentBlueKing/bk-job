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

// 账号分类
const OS = 1;
const DB = 2;

// 账号类型
const TYPE_LINUX = 1;
const TYPE_WINDOWS = 2;
const TYPE_MYSQL = 9;
const TYPE_ORACLE = 10;
const TYPE_DB2 = 11;

export default class Account {
    static OS = OS
    static DB = DB

    static TYPE_LINUX = TYPE_LINUX
    static TYPE_WINDOWS = TYPE_WINDOWS
    static TYPE_MYSQL = TYPE_MYSQL
    static TYPE_ORACLE = TYPE_ORACLE
    static TYPE_DB2 = TYPE_DB2

    static accountType = {
        liunx: TYPE_LINUX,
        windows: TYPE_WINDOWS,
        mysql: TYPE_MYSQL,
        oracle: TYPE_ORACLE,
        db2: TYPE_DB2,
    }

    constructor (payload) {
        this.id = payload.id;
        this.account = payload.account;
        this.alias = payload.alias;
        this.categoryName = payload.categoryName;
        this.category = payload.category;
        this.createTime = payload.createTime;
        this.creator = payload.creator;
        this.dbPassword = payload.dbPassword;
        this.dbPort = payload.dbPort;
        this.dbSystemAccountId = payload.dbSystemAccountId;
        this.lastModifyTime = payload.lastModifyTime;
        this.lastModifyUser = payload.lastModifyUser;
        this.os = payload.os;
        this.ownerUsers = payload.ownerUsers || [];
        this.password = payload.password;
        this.remark = payload.remark || '';
        this.type = payload.type;
        this.typeName = payload.typeName;
        // 权限
        this.canUse = payload.canUse;
        this.canManage = payload.canManage;
    }

    get isSystem () {
        return this.category === 1;
    }
}
