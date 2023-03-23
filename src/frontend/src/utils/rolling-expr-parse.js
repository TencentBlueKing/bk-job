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

import I18n from '@/i18n';
import { ordinalSuffixOf } from '@utils/assist';

const fixed = /^([1-9]\d*)$/;
const fixedIn = /^\+([1-9]\d*)$/;
const fixedMu = /^\*([1-9]\d*)$/;
const per = /^([1-9]\d?)%$/;
const all = /^100%$/;

const translateCN = (value) => {
  let lastFixedNum = 0;
  let lastPerNum = '';

  const lastBatchPre = value.length > 1 ? '后面' : '';

  const batchTotal = value.length;

  const parse = (atoms, batchNum) => {
    const fixedData = atoms.match(fixed);
    if (fixedData) {
      const fixedNum = parseInt(fixedData[1], 10);

      lastPerNum = '';
      lastFixedNum = fixedNum;

      if (batchNum === batchTotal) {
        return [`${lastBatchPre}按每${fixedNum}台一批直至结束`];
      }
      return [`第${batchNum}批${fixedNum}台`];
    }

    const perData = atoms.match(per);
    if (perData) {
      const perNum = parseInt(perData[1], 10);

      lastFixedNum = 0;
      lastPerNum = perNum;

      if (batchNum === batchTotal) {
        return [`${lastBatchPre}按每${perNum}%台一批直至结束`];
      }
      return [`第${batchNum}批${perNum}%台`];
    }

    const fixedInData = atoms.match(fixedIn);
    if (fixedInData) {
      if (batchNum === 1) {
        throw new Error(`${atoms} 不能出现在开头`);
      }
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} 必须出现在最后一位`);
      }

      const step = parseInt(fixedInData[1], 10);

      const textQueue = [];
      if (lastPerNum) {
        textQueue.push(`第${batchNum}批${lastPerNum}%+${step}台`);
        textQueue.push(`第${batchNum + 1}批${lastPerNum}%+${step + step}台`);
      } else if (lastFixedNum) {
        textQueue.push(`第${batchNum}批${step + lastFixedNum}台`);
        textQueue.push(`第${batchNum + 1}批${step + step + lastFixedNum}台`);
      }
      textQueue.push(`...之后“每批增加${step}”台直至结束`);
      return textQueue;
    }

    const fixedMuData = atoms.match(fixedMu);
    if (fixedMuData) {
      if (batchNum === 1) {
        throw new Error(`${atoms} 不能出现在开头`);
      }
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} 必须出现在最后一位`);
      }

      const rate = parseInt(fixedMuData[1], 10);
      const textQueue = [];
      if (lastPerNum) {
        textQueue.push(`第${batchNum}批${rate * lastPerNum}%台`);
        textQueue.push(`第${batchNum + 1}批${rate * rate * lastPerNum}%台`);
      } else if (lastFixedNum) {
        textQueue.push(`第${batchNum}批${rate * lastFixedNum}台`);
        textQueue.push(`第${batchNum + 1}批${rate * rate * lastFixedNum}台`);
      }
      textQueue.push(`...之后“每批乘于${rate}”台直至结束`);
      return textQueue;
    }

    if (all.test(atoms)) {
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} 必须出现在最后一位`);
      }
      if (batchNum === 1) {
        return ['全部执行'];
      }

      return [`第${batchNum}批执行所有剩余主机`];
    }

    throw new Error(`不支持的配置规则 ${atoms}`);
  };
  const result = [];
  value.forEach((atoms, index) => {
    result.push.apply(result, parse(atoms, index + 1));
  });
  return result.join('，');
};

const translateEN = (value) => {
  let lastFixedNum = 0;
  let lastPerNum = '';

  const lastBatchPre = value.length > 1 ? '..., ' : '';

  const batchTotal = value.length;

  const parse = (atoms, batchNum) => {
    const fixedData = atoms.match(fixed);
    if (fixedData) {
      const fixedNum = parseInt(fixedData[1], 10);

      lastPerNum = '';
      lastFixedNum = fixedNum;

      if (batchNum === batchTotal) {
        // eslint-disable-next-line max-len
        return [`${lastBatchPre}followed by a batch of every ${fixedNum} host(s) until the rolling of all remaining hosts.`];
      }
      return [`${ordinalSuffixOf(batchNum)} batch ${fixedNum} hosts(s)`];
    }

    const perData = atoms.match(per);
    if (perData) {
      const perNum = parseInt(perData[1], 10);

      lastFixedNum = 0;
      lastPerNum = perNum;

      if (batchNum === batchTotal) {
        // eslint-disable-next-line max-len
        return [`${lastBatchPre}${lastBatchPre}${perNum}% host(s) per batch until the rolling of all remaining hosts.`];
      }
      return [`${ordinalSuffixOf(batchNum)} batch ${perNum}% hosts(s)`];
    }

    const fixedInData = atoms.match(fixedIn);
    if (fixedInData) {
      if (batchNum === 1) {
        throw new Error(`${atoms} formula is only allowed in the end of the expression.`);
      }
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} formula is only allowed in the end of the expression.`);
      }

      const step = parseInt(fixedInData[1], 10);

      const textQueue = [];
      if (lastPerNum) {
        textQueue.push(`${ordinalSuffixOf(batchNum)} batch ${lastPerNum}%+${step} host(s)`);
        textQueue.push(`${ordinalSuffixOf(batchNum + 1)} batch ${lastPerNum}%+${step + step} host(s)`);
      } else if (lastFixedNum) {
        textQueue.push(`${ordinalSuffixOf(batchNum)} batch ${step + lastFixedNum} host(s)`);
        textQueue.push(`${ordinalSuffixOf(batchNum + 1)} batch${step + step + lastFixedNum} host(s)`);
      }
      textQueue.push(`...and so on "increment ${step} host(s) per batch" until the rolling of all remaining hosts.`);
      return textQueue;
    }

    const fixedMuData = atoms.match(fixedMu);
    if (fixedMuData) {
      if (batchNum === 1) {
        throw new Error(`${atoms} formula is only allowed in the end of the expression.`);
      }
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} formula is only allowed in the end of the expression.`);
      }

      const rate = parseInt(fixedMuData[1], 10);
      const textQueue = [];
      if (lastPerNum) {
        textQueue.push(`${ordinalSuffixOf(batchNum)} batch ${rate * lastPerNum}% host(s)`);
        textQueue.push(`${ordinalSuffixOf(batchNum + 1)} batch ${rate * rate * lastPerNum}% host(s)`);
      } else if (lastFixedNum) {
        textQueue.push(`${ordinalSuffixOf(batchNum)} batch${rate * lastFixedNum} host(s)`);
        textQueue.push(`${ordinalSuffixOf(batchNum + 1)} batch ${rate * rate * lastFixedNum} host(s)`);
      }
      textQueue.push(`...and so on "times ${rate} host(s) per batch" until the rolling of all remaining hosts.`);
      return textQueue;
    }

    if (all.test(atoms)) {
      if (batchNum < batchTotal) {
        throw new Error(`${atoms} formula is only allowed in the end of the expression.`);
      }
      if (batchNum === 1) {
        return ['All of the hosts in one batch.'];
      }

      return [`"${ordinalSuffixOf(batchNum)} batch" all the remaining hosts.`];
    }

    throw new Error(`${atoms} is not allowed.`);
  };
  const result = [];
  value.forEach((atoms, index) => {
    result.push.apply(result, parse(atoms, index + 1));
  });
  return result.join(', ');
};

export default function (exprStr) {
  const batchStack = exprStr.trim().split(' ');
  if (batchStack.length < 1) {
    return '';
  }
  if (I18n.locale === 'en-US') {
    return translateEN(batchStack);
  }

  return translateCN(batchStack);
}
