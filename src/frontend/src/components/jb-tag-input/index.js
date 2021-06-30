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

const getValue = (list, tags) => {
    const newTagsBak = new Set(tags);
    const newTags = [];
    list.forEach((tag) => {
        if (newTagsBak.has(parseInt(tag.id, 10))) {
            newTags.push(tag);
            newTagsBak.delete(parseInt(tag.id, 10));
        }
    })
    ;[
        ...newTagsBak,
    ].forEach((tag) => {
        newTags.push({
            id: '',
            name: tag,
        });
    });
    return newTags;
};

export default {
    name: 'jb-tag-input',
    props: {
        list: {
            type: Array,
            default: () => [],
        },
        value: {
            type: Array,
            default: () => [],
        },
    },
    render (h) {
        return h('bk-tag-input', {
            on: {
                ...this.$listeners,
                change: (tags) => {
                    const value = getValue(this.list, tags);
                    this.$emit('change', value);
                    this.$emit('input', value);
                },
            },
            props: {
                ...this.$attrs,
                hasDeleteIcon: true,
                list: this.list,
                contentMaxHeight: 180,
                trigger: 'focus',
                clearable: false,
                allowCreate: false,
                allowAutoMatch: true,
                value: this.value.map(tag => tag.id || tag.name),
            },
            ref: 'bkTagInput',
        });
    },
};
