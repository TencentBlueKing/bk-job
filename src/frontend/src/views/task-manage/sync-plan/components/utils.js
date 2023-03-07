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

/* eslint-disable no-param-reassign,no-plusplus */
import _ from 'lodash';

const isSameNode = (pre, next) => `${pre.realId}` === `${next.realId}`;

const createRealIdToIndex = (target, start, end) => {
  const stack = [];
  while (start <= end) {
    stack.push(target[start].realId);
    start++;
  }
  return stack;
};

const isValueEqual = (pre, next) => {
  if (_.isArray(pre)) {
    if (pre.length !== next.length) {
      return false;
    }
    for (let i = 0; i < pre.length; i++) {
      if (!isValueEqual(pre[i], next[i])) {
        return false;
      }
    }
    return true;
  }
  if (Object.prototype.toString.call(pre) === '[object Object]'
        && Object.prototype.toString.call(next) === '[object Object]') {
    /* eslint-disable no-iterator, no-restricted-syntax */
    for (const key in pre) {
      if (key === 'id') {
        continue;
      }
      if (!isValueEqual(pre[key], next[key])) {
        return false;
      }
    }
    return true;
  }
  return pre === next;
};
const compare = (pre, next, keys = []) => {
  const result = {};
  const allKeys = keys.length > 0 ? keys : Object.keys(pre);
  for (let i = 0; i < allKeys.length; i++) {
    const curKey = allKeys[i];
    result[curKey] = isValueEqual(pre[curKey], next[curKey]) ? '' : 'changed';
  }
  return Object.freeze(result);
};

const patchVariable = (pre, next) => compare(pre, next, [
  'name', 'type', 'defaultValue', 'defaultTargetValue', 'description', 'changeable', 'required',
]);

const patchStep = (pre, next) => {
  const base = compare(pre, next, [
    'name', 'type',
  ]);
  let step = {};
  switch (pre.type) {
    case 1:
      step = compare(pre.scriptStepInfo, next.scriptStepInfo);
      break;
    case 2:
      step = compare(pre.fileStepInfo, next.fileStepInfo);
      break;
    case 3:
      step = compare(pre.approvalStepInfo, next.approvalStepInfo);
      break;
    default:
      step = {};
  }
  return {
    ...base,
    ...step,
  };
};

const createCollectFromRealId = (list) => {
  const map = {};
  for (let i = 0; i < list.length; i++) {
    map[list[i].realId] = list[i];
  }
  return map;
};

const getDiffKeyFromNode = node => `${node.realId}`;

const findNodeIndex = (list, node) => _.findIndex(list, _ => _.realId === node.realId);

export const mergeList = (target, source) => {
  const realIdMap = {};
  const result = [];
  for (let i = 0; i < target.length; i++) {
    const cur = target[i];
    realIdMap[cur.realId] = 1;
    result.push(cur);
  }
  for (let j = 0; j < source.length; j++) {
    const cur = source[j];
    if (!realIdMap[cur.realId]) {
      result.push(cur);
    }
  }
  return result;
};
export const diffStepSimple = (template, plan) => {
  template = _.cloneDeep(template);
  plan = _.cloneDeep(plan);
  // debugger

  let templateStartIndex = 0;
  const templateEndIndex = template.length - 1;
  let planStartIdex = 0;
  const planEndIndex = plan.length - 1;
  const hasDelete = 0;

  const stack = [];
  const diff = {};
  // debugger
  while (templateStartIndex <= templateEndIndex) {
    planStartIdex = 0;
    const curTemplateNode = template[templateStartIndex];
    const planNodeIndex = findNodeIndex(plan, curTemplateNode);
    ++templateStartIndex;
    // 存在模板中不在执行方案中表示新增
    if (planNodeIndex < 0) {
      stack.push(curTemplateNode);
      diff[curTemplateNode.id] = {
        type: 'new',
      };
      continue;
    }
    // 遍历执行方案中的步骤
    while (planStartIdex <= planNodeIndex) {
      const curPlanNode = plan[planStartIdex];
      ++planStartIdex;

      const templateNodeIndex = findNodeIndex(template, curPlanNode);
      // 如果执行方案中的步骤在模板中也存在以模板的处理逻辑为准
      if (templateNodeIndex > -1) {
        continue;
      }
      // 执行方案中的步骤不存在于模板中，表示被删掉了
      if (!diff[curPlanNode.id]) {
        stack.push(curPlanNode);
        diff[curPlanNode.id] = {
          type: 'delete',
        };
      }
    }
    const length = stack.push(curTemplateNode);
    const sort = planNodeIndex - (length - hasDelete) + 1;
    if (sort === 0) {
      const stepPath = patchStep(curTemplateNode, plan[planNodeIndex]);
      if (Object.values(stepPath).join('') !== '') {
        diff[curTemplateNode.id] = {
          type: 'different',
        };
      } else {
        diff[curTemplateNode.id] = {
          type: 'same',
        };
      }
    } else {
      diff[curTemplateNode.id] = {
        type: 'move',
        value: sort,
      };
    }
  }
  planStartIdex = 0;
  while (planStartIdex <= planEndIndex) {
    const curPlanNode = plan[planStartIdex];
    ++planStartIdex;
    if (findNodeIndex(stack, curPlanNode) < 0) {
      stack.push(curPlanNode);
      diff[curPlanNode.id] = {
        type: 'delete',
      };
    }
  }
  return [
    stack, diff,
  ];
};

export const diffVariableSimple = (template, plan) => {
  template = _.cloneDeep(template);
  plan = _.cloneDeep(plan);
  let templateStartIndex = 0;
  let templateEndIndex = template.length - 1;
  let [templateStartNode] = template;
  let templateEndNode = template[templateEndIndex];
  let planStartIndex = 0;
  let planEndIndex = plan.length - 1;
  let [planStartNode] = plan;
  let planEndNode = plan[planEndIndex];

  const getDiffKey = node => `${node.realId}`;
  const patchNode = (pre, next) => {
    const diff = patchVariable(pre, next);
    if (Object.values(diff).join('') === '') {
      return {
        type: 'same',
      };
    }
    return {
      type: 'different',
    };
  };

  const result = {};
  const compareReadId = item => item.realId === planStartNode.realId;
  while (templateStartIndex <= templateEndIndex && planStartIndex <= planEndIndex) {
    if (isSameNode(templateStartNode, planStartNode)) {
      result[getDiffKey(templateStartNode)] = patchNode(templateStartNode, planStartNode);
      templateStartNode = template[++templateStartIndex];
      planStartNode = plan[++planStartIndex];
    } else if (isSameNode(templateEndNode, planEndNode)) {
      result[getDiffKey(templateEndNode)] = patchNode(templateEndNode, planEndNode);
      templateEndNode = template[--templateEndIndex];
      planEndNode = plan[--planEndIndex];
    } else if (isSameNode(templateStartNode, planEndNode)) {
      result[getDiffKey(templateStartNode)] = patchNode(templateStartNode, planEndNode);
      templateStartNode = template[++templateStartIndex];
      planEndNode = plan[--planEndIndex];
    } else if (isSameNode(templateEndNode, planStartNode)) {
      result[getDiffKey(templateEndNode)] = patchNode(templateEndNode, planStartNode);
      templateEndNode = template[--templateEndNode];
      planStartNode = plan[++planStartIndex];
    } else {
      const templateRealIdToIndex = createRealIdToIndex(template, templateStartIndex, templateEndIndex);
      if (templateRealIdToIndex.includes(planStartNode.realId)) {
        const templateStepIndex = _.findIndex(template, compareReadId);
        result[getDiffKey(planStartNode)] = patchNode(template[templateStepIndex], planStartNode);
        template.splice(templateStepIndex, 1);
        --templateEndIndex;
      } else {
        result[getDiffKey(planStartNode)] = {
          type: 'delete',
        };
      }
      planStartNode = plan[++planStartIndex];
    }
  }
  while (templateStartIndex <= templateEndIndex) {
    const curTemplateNode = template[templateStartIndex];
    ++templateStartIndex;
    if (!curTemplateNode) {
      continue;
    }

    result[getDiffKey(curTemplateNode)] = {
      type: 'new',
    };
  }
  while (planStartIndex <= planEndIndex) {
    const curPlanNode = plan[planStartIndex];
    ++planStartIndex;
    if (!curPlanNode) {
      continue;
    }
    result[getDiffKey(curPlanNode)] = {
      type: 'delete',
    };
  }
  return result;
};

export const composeList = (template, plan) => {
  const templateLength = template.length;
  const planLength = plan.length;

  const templateStack = [];
  const planStask = [];

  const templateRealIdCollect = createCollectFromRealId(template);
  const planRealIdCollect = createCollectFromRealId(plan);

  let i = 0;
  let j = 0;
  while (i < templateLength && j < planLength) {
    const curTemplate = template[i];
    const curPlan = plan[j];
    if (curPlan.realId === curTemplate.realId) {
      planStask.push(curPlan);
      templateStack.push(curTemplate);
      delete templateRealIdCollect[curTemplate.realId];
      delete planRealIdCollect[curPlan.realId];
    } else {
      if (templateRealIdCollect[curTemplate.realId]) {
        // 在模板中存在
        templateStack.push(curTemplate);

        if (planRealIdCollect[curTemplate.realId]) {
          // 同样存在执行方案中存在
          planStask.push(planRealIdCollect[curTemplate.realId]);
          delete planRealIdCollect[curTemplate.realId];
        } else {
          // 在执行方案中不存在——添加一个空
          planStask.push('');
        }
        delete templateRealIdCollect[curTemplate.realId];
      }
      if (planRealIdCollect[curPlan.realId]) {
        // 在执行方案中存在
        planStask.push(curPlan);

        if (templateRealIdCollect[curPlan.realId]) {
          // 同样存在与模板
          templateStack.push(templateRealIdCollect[curPlan.realId]);
          delete templateRealIdCollect[curPlan.realId];
        } else {
          // 在作业模板中不存（已经被删除）——将执行方案中的数据复制一份到模板中
          templateStack.push(planRealIdCollect[curPlan.realId]);
        }
        delete planRealIdCollect[curPlan.realId];
      }
    }
    i++;
    j++;
  }
  while (j < planLength) {
    const curPlan = plan[j];
    if (planRealIdCollect[curPlan.realId]) {
      planStask.push(curPlan);
      templateStack.push(curPlan);
    }

    j++;
  }
  while (i < templateLength) {
    const curTemplate = template[i];
    if (templateRealIdCollect[curTemplate.realId]) {
      planStask.push('');
      templateStack.push(curTemplate);
    }

    i++;
  }
  return [
    templateStack, planStask,
  ];
};

const checkChild = (template, plan, patchNode) => {
  template = [
    ...template,
  ];
  plan = [
    ...plan,
  ];

  let templateStartIndex = 0;
  let templateEndIndex = template.length - 1;
  let [templateStartNode] = template;
  let templateEndNode = template[templateEndIndex];
  let planStartIndex = 0;
  let planEndIndex = plan.length - 1;
  let [planStartNode] = plan;
  let planEndNode = plan[planEndIndex];

  const result = {};
  const compareByReadId = item => item.realId === planStartNode.realId;
  while (templateStartIndex <= templateEndIndex && planStartIndex <= planEndIndex) {
    if (isSameNode(templateStartNode, planStartNode)) {
      result[getDiffKeyFromNode(templateStartNode)] = {
        type: 'different',
        value: patchNode(templateStartNode, planStartNode),
      };
      templateStartNode = template[++templateStartIndex];
      planStartNode = plan[++planStartIndex];
    } else if (isSameNode(templateEndNode, planEndNode)) {
      result[getDiffKeyFromNode(templateEndNode)] = {
        type: 'different',
        value: patchNode(templateEndNode, planEndNode),
      };
      templateEndNode = template[--templateEndIndex];
      planEndNode = plan[--planEndIndex];
    } else if (isSameNode(templateStartNode, planEndNode)) {
      result[getDiffKeyFromNode(templateStartNode)] = {
        type: 'different',
        value: patchNode(templateStartNode, planEndNode),
      };
      templateStartNode = template[++templateStartIndex];
      planEndNode = plan[--planEndIndex];
    } else if (isSameNode(templateEndNode, planStartNode)) {
      result[getDiffKeyFromNode(templateEndNode)] = {
        type: 'different',
        value: patchNode(templateEndNode, planStartNode),
      };
      templateEndNode = template[--templateEndIndex];
      planStartNode = plan[++planStartIndex];
    } else {
      const templateRealIdToIndex = createRealIdToIndex(template, templateStartIndex, templateEndIndex);
      if (templateRealIdToIndex.includes(planStartNode.realId)) {
        const templateNodeIndex = _.findIndex(template, compareByReadId);
        result[getDiffKeyFromNode(template[templateNodeIndex])] = {
          type: 'different',
          value: patchNode(template[templateNodeIndex], planStartNode),
        };
        template.splice(templateNodeIndex, 1);
        --templateEndIndex;
      } else {
        result[getDiffKeyFromNode(planStartNode)] = {
          type: 'delete',
        };
      }
      planStartNode = plan[++planStartIndex];
    }
  }
  while (templateStartIndex <= templateEndIndex) {
    const curTemplateNode = template[templateStartIndex];
    ++templateStartIndex;
    if (!curTemplateNode) {
      continue;
    }

    result[getDiffKeyFromNode(curTemplateNode)] = {
      type: 'new',
    };
  }
  while (planStartIndex <= planEndIndex) {
    const curPlanNode = plan[planStartIndex];
    ++planStartIndex;
    if (!curPlanNode) {
      continue;
    }
    result[getDiffKeyFromNode(curPlanNode)] = {
      type: 'delete',
    };
  }
  return result;
};

// 深层对比全局变量
export const diffVariable = (template, plan) => checkChild(template, plan, patchVariable);
// 深层对比作业步骤
export const diffStep = (template, plan) => checkChild(template, plan, patchStep);
