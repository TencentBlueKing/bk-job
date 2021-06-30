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

/* eslint-disable */
import _ from 'lodash'

export const parseURL = url => {
    const a = document.createElement('a')
    a.href = url
    
    return {
        source: url,
        protocol: a.protocol.replace(':', ''),
        host: a.hostname,
        pathname: a.pathname.replace(/\/?$/, '/'),
        port: a.port,
        search: a.search.replace(/^\?/, ''),
        hash: a.hash.replace('#', '')
    }
}

export const buildURLParams = (params) => {
    function forEach (obj, fn) {
        // Don't bother if no value provided
        if (obj === null || typeof obj === 'undefined') {
            return
        }
      
        // Force an array if not already something iterable
        if (typeof obj !== 'object') {
            /* eslint no-param-reassign:0*/
            obj = [obj]
        }
      
        if (_.isArray(obj)) {
            // Iterate over array values
            for (let i = 0, l = obj.length; i < l; i++) {
                fn(obj[i], i, obj)
            }
        } else {
            // Iterate over object keys
            for (const key in obj) {
                if (Object.prototype.hasOwnProperty.call(obj, key)) {
                    fn(obj[key], key, obj)
                }
            }
        }
    }
    function encode (val) {
        return encodeURIComponent(val)
            .replace(/%40/gi, '@')
            .replace(/%3A/gi, ':')
            .replace(/%24/g, '$')
            .replace(/%2C/gi, ',')
            .replace(/%20/g, '+')
            .replace(/%5B/gi, '[')
            .replace(/%5D/gi, ']')
    }
    /* eslint no-param-reassign:0*/
    if (!params) {
        return ''
    }
  
    const parts = []
  
    forEach(params, function serialize (val, key) {
        if (val === null || typeof val === 'undefined') {
            return
        }
  
        if (_.isArray(val)) {
            key = key + '[]'
        } else {
            val = [val]
        }
  
        forEach(val, function parseValue (v) {
            if (_.isDate(v)) {
                v = v.toISOString()
            } else if (_.isObject(v)) {
                v = JSON.stringify(v)
            }
            parts.push(encode(key) + '=' + encode(v))
        })
    })
    return parts.join('&')
}
