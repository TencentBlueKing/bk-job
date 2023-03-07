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

/* eslint-disable no-param-reassign */

import './index.css';

const DEFAULT_OPTIONS = {
  active: true,
  offset: [
    12, 0,
  ],
  cls: 'cursor-element',
};

const init = function (el, binding) {
  el.mouseEnterHandler = function () {
    const element = document.createElement('div');
    element.id = 'directive-ele';
    element.style.position = 'absolute';
    element.style.zIndex = '999999';
    element.style.width = '18px';
    element.style.height = '18px';
    el.element = element;
    document.body.appendChild(element);

    element.classList.add(binding.value.cls || DEFAULT_OPTIONS.cls);
    el.addEventListener('mousemove', el.mouseMoveHandler);
  };
  el.mouseMoveHandler = function (event) {
    const { pageX, pageY } = event;
    const elLeft = pageX + DEFAULT_OPTIONS.offset[0];
    const elTop = pageY + DEFAULT_OPTIONS.offset[1];
    el.element.style.left = `${elLeft}px`;
    el.element.style.top = `${elTop}px`;
  };
  el.mouseLeaveHandler = function (event) {
    el.element && el.element.remove();
    el.element = null;
    el.removeEventListener('mousemove', el.mouseMoveHandler);
  };
  if (binding.value.active) {
    el.addEventListener('mouseenter', el.mouseEnterHandler);
    el.addEventListener('mouseleave', el.mouseLeaveHandler);
  }
};

const destroy = function (el) {
  el.element && el.element.remove();
  el.element = null;
  el.removeEventListener('mouseenter', el.mouseEnterHandler);
  el.removeEventListener('mousemove', el.mouseMoveHandler);
  el.removeEventListener('mouseleave', el.mouseLeaveHandler);
};

export default {
  install(Vue) {
    Vue.directive('cursor', {
      bind(el, binding) {
        binding.value = Object.assign({}, DEFAULT_OPTIONS, binding.value);
        init(el, binding);
      },
      update(el, binding) {
        binding.value = Object.assign({}, DEFAULT_OPTIONS, binding.value);
        destroy(el);
        init(el, binding);
      },
      unbind(el) {
        destroy(el);
      },
    });
  },
};
