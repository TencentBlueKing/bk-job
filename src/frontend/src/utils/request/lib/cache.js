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

import _ from 'lodash'
const cacheMap = new Map()
const cacheExpireMap = new Map()

class Cache {
    set (name, value, expire) {
        if (_.isNumber(expire)) {
            cacheExpireMap.set(name, Date.now() + expire)
        }
        if (!cacheMap.has(name)) {
            return cacheMap.set(name, value)
        }
        return true
    }
    get (name) {
        if (cacheMap.has(name)) {
            return cacheMap.get(name)
        }
        return false
    }
    has (name) {
        if (!cacheMap.has(name)) {
            return false
        }
        const expire = cacheExpireMap.get(name)
        if (!expire) {
            return true
        }
        if (Date.now() > expire) {
            cacheMap.delete(name)
            cacheExpireMap.delete(name)
            return false
        }
        return true
    }
    delete (name) {
        if (cacheMap.has(name)) {
            return cacheMap.delete(name)
        }
        return true
    }
    clear () {
        cacheMap.clear()
        return cacheMap.size < 1
    }
}

export default new Cache()
