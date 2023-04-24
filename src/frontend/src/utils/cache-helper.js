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

import _ from 'lodash';

export const scopeCache = {
  key: 'scope',
  setItem(value) {
    localStorage.setItem(scopeCache.key, JSON.stringify(value));
  },
  getItem() {
    return JSON.parse(localStorage.getItem(scopeCache.key) || '{}');
  },
};

export const routerCache = {
  key: 'router_history',
  setItem(key, value) {
    let cacheMap = JSON.parse(localStorage.getItem(routerCache.key));
    if (_.isPlainObject(cacheMap)) {
      cacheMap[key] = value;
    } else {
      cacheMap = {
        [key]: value,
      };
    }
    localStorage.setItem(routerCache.key, JSON.stringify(cacheMap));
  },
  getItem(key) {
    const cacheMap = JSON.parse(localStorage.getItem(routerCache.key));
    if (_.isPlainObject(cacheMap)) {
      if (!key) {
        return cacheMap;
      }
      if (cacheMap[key]) {
        return cacheMap[key];
      }
    }
    return '';
  },
  clearItem(key) {
    if (!key) {
      return false;
    }
    const cacheMap = JSON.parse(localStorage.getItem(routerCache.key));
    if (!cacheMap) {
      return false;
    }
    if (!cacheMap[key]) {
      return true;
    }
    delete cacheMap[key];
    localStorage.setItem(routerCache.key, JSON.stringify(cacheMap));
    return true;
  },
};

/* 作业导出本地缓存
{
    id: '',
    ids: '',
    templateInfo: []
}
*/
export const taskExport = {
  key: 'task_export_info',
  setItem(key, value) {
    const oldValue = taskExport.getItem() || {};
    localStorage.setItem(taskExport.key, JSON.stringify({
      ...oldValue,
      [key]: value,
    }));
  },
  getItem(key) {
    try {
      const value = JSON.parse(localStorage.getItem(taskExport.key));
      if (!_.isPlainObject(value)) {
        return false;
      }
      if (key) {
        return value[key];
      }
      return value;
    } catch {
      return false;
    }
  },
  clearItem() {
    localStorage.removeItem(taskExport.key);
  },
};

/**
 * @desc 作业导入本地缓存
*/
export const taskImport = {
  key: 'task_import_info',
  setItem(key, value) {
    const oldValue = taskImport.getItem() || {};
    localStorage.setItem(taskImport.key, JSON.stringify({
      ...oldValue,
      [key]: value,
    }));
  },
  getItem(key) {
    try {
      const value = JSON.parse(localStorage.getItem(taskImport.key));
      if (!_.isPlainObject(value)) {
        return false;
      }
      if (key) {
        return value[key];
      }
      return value;
    } catch {
      return false;
    }
  },
  clearItem() {
    localStorage.removeItem(taskImport.key);
  },
};

/**
 * @desc 快速执行脚本——操作记录
*/
export const execScriptHistory = {
  key: `exec_script_history_${Date.now()}`,
  write: false,
  getItem() {
    try {
      const record = JSON.parse(localStorage.getItem(execScriptHistory.key));
      if (!record) {
        return [];
      }
      return record;
    } catch {
      return [];
    }
  },
  setItem(value) {
    localStorage.setItem(execScriptHistory.key, JSON.stringify(value.slice(0, 5)));
    if (!execScriptHistory.write) {
      window.addEventListener('unload', () => {
        execScriptHistory.clearItem();
      });
    }
    execScriptHistory.write = true;
  },
  clearItem() {
    localStorage.removeItem(execScriptHistory.key);
  },
};

/**
 * @desc 快速分发文件——执行记录
*/
export const pushFileHistory = {
  key: `push_file_history_${Date.now()}`,
  write: false,
  getItem() {
    try {
      const record = JSON.parse(localStorage.getItem(pushFileHistory.key));
      if (!record) {
        return [];
      }
      return record;
    } catch {
      return [];
    }
  },
  setItem(value) {
    localStorage.setItem(pushFileHistory.key, JSON.stringify(value.slice(0, 5)));
    if (!pushFileHistory.write) {
      window.addEventListener('unload', () => {
        pushFileHistory.clearItem();
      });
    }
    pushFileHistory.write = true;
  },
  clearItem() {
    localStorage.removeItem(pushFileHistory.key);
  },
};

/**
 * @desc 列表列显示缓存
*/
export const listColumnsCache = {
  key: 'list_column_display',
  setItem(key, value) {
    const lastValue = listColumnsCache.getItem() || {};
    localStorage.setItem(listColumnsCache.key, JSON.stringify({
      ...lastValue,
      [key]: value,
    }));
  },
  getItem(key) {
    try {
      const allCache = JSON.parse(localStorage.getItem(listColumnsCache.key));
      if (!_.isPlainObject(allCache)) {
        return false;
      }
      if (!key) {
        return allCache;
      }
      if (!allCache[key]) {
        return false;
      }
      if (!allCache[key].columns || !allCache[key].size) {
        return false;
      }
      return allCache[key];
    } catch {
      return false;
    }
  },
  clearItem() {
    localStorage.removeItem(listColumnsCache.key);
  },
};

/**
 * @desc debug脚本
*/
export const debugScriptCache = {
  key: 'debug_script',
  setItem(value) {
    localStorage.setItem(debugScriptCache.key, JSON.stringify(value));
  },
  getItem() {
    const scriptInfo = JSON.parse(localStorage.getItem(debugScriptCache.key));
    if (!_.isPlainObject(scriptInfo)) {
      return null;
    }
    return scriptInfo;
  },
  clearItem() {
    localStorage.removeItem(debugScriptCache.key);
  },
};

/**
 * @desc IP 选择器拓扑树显示隐藏主机为空的节点
*/
export const topoNodeCache = {
  key: 'ip_selector_topo_node',
  setItem(username = 'local') {
    localStorage.setItem(topoNodeCache.key, JSON.stringify({
      [username]: Date.now(),
    }));
  },
  getItem(username = 'local') {
    const target = localStorage.getItem(topoNodeCache.key);
    if (!target) {
      return true;
    }
    const targetObj = JSON.parse(target);
    if (!targetObj[username]) {
      return true;
    }
    return false;
  },
  clearItem() {
    localStorage.removeItem(topoNodeCache.key);
  },
};

/**
 * @desc 列表搜索时筛选用户缓存
*/
export const userSearchCache = {
  key: 'user_search_record',
  setItem(value) {
    const records = userSearchCache.getItem();
    records.unshift(value);
    localStorage.setItem(userSearchCache.key, JSON.stringify([...new Set(records)].slice(0, 5)));
  },
  getItem() {
    return JSON.parse(localStorage.getItem(userSearchCache.key) || '[]');
  },
};
