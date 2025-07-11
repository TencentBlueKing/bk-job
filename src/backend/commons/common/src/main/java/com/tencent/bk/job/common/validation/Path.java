/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.common.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示对象图中从一个对象到另一个对象的导航路径, 比如 root.first.second[0].third[key]
 */
public class Path {
    /**
     * 字段名；当 PATH 为位置索引的时候，值为""
     */
    private final String name;
    /**
     * 如果 name == ""，这是前一个元素的下标（索引或映射键）
     */
    private final String index;

    /**
     * 根路径
     */
    private Path parent;

    private Path(String name, String index, Path parent) {
        this.name = name;
        this.index = index;
        this.parent = parent;
    }

    /**
     * 创建一个根对象导航路径
     *
     * @param name      根路径名
     * @param moreNames 子路径名
     */
    public static Path newPath(String name, String... moreNames) {
        Path r = new Path(name, null, null);
        for (String moreName : moreNames) {
            r = new Path(moreName, null, r);
        }
        return r;
    }

    /**
     * 返回根路径
     */
    public Path root() {
        Path p = this;
        while (p.parent != null) {
            p = p.parent;
        }
        return p;
    }

    /**
     * 返回父路径
     */
    public Path parent() {
        return this.parent;
    }

    /**
     * 在当前路径下创建子路径
     *
     * @param name      路径名
     * @param moreNames 子路径名列表
     */
    public Path child(String name, String... moreNames) {
        Path r = newPath(name, moreNames);
        r.root().parent = this;
        return r;
    }

    /**
     * 在当前路径下创建索引路径(array)
     *
     * @param index 索引
     */
    public Path index(int index) {
        return new Path("", String.valueOf(index), this);
    }

    /**
     * 在当前路径下创建映射键路径(map)
     *
     * @param key 映射键
     */
    public Path key(String key) {
        return new Path("", key, this);
    }

    @Override
    public String toString() {

        List<Path> elements = new ArrayList<>();
        Path p = this;
        while (p != null) {
            elements.add(p);
            p = p.parent;
        }

        StringBuilder buf = new StringBuilder();
        for (int i = elements.size() - 1; i >= 0; i--) {
            p = elements.get(i);
            if (p.parent != null && p.name.length() > 0) {
                buf.append(".");
            }
            if (p.name.length() > 0) {
                buf.append(p.name);
            } else {
                buf.append("[").append(p.index).append("]");
            }
        }
        return buf.toString();
    }
}

