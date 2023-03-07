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

const noop = () => {};

const parseVariableValue = (variableExpression, data) => {
  const reg = /\$\{([^}]+)\}+/g;
  return variableExpression.replace(reg, (match, p1) => data[p1]);
};

export const parseCondition = (action, data) => {
  if (!action) {
    return false;
  }
  if (!action.rely) {
    return true;
  }

  const checkExpressionOperation = {
    AND: (pre, last) => pre && last,
    OR: (pre, last) => pre || last,
  };

  const {
    operation,
    expressions,
  } = action.rely;

  let result = operation === 'AND';
  // eslint-disable-next-line no-plusplus
  for (let i = 0; i < expressions.length; i++) {
    const {
      key,
      value,
    } = expressions[i];
    result = checkExpressionOperation[operation](result, data[key] === value);
    if (result) {
      break;
    }
  }
  return result;
};

const parseTarget = (target, data) => parseVariableValue(target, data);

const parseParams = (params, data) => {
  const paramsTemplate = JSON.parse(params);

  Object.keys(paramsTemplate).forEach((key) => {
    paramsTemplate[key] = parseVariableValue(paramsTemplate[key], data);
  });
  return paramsTemplate;
};

export const checkIsCheckboxColumn = type => type === 'checkbox';
const checkIsButtonGroupColumn = type => type === 'buttonGroup';
const checkIsLinkAction = type => [
  'FILE_LINK',
].includes(type);

const actionConfirmInfo = {
  DELETE: {
    title: I18n.t('确定删除该文件(夹)？'),
    content: I18n.t('文件源本体的该文件(夹)也会被删除，请谨慎操作！'),
  },
};

export default {
  functional: true,
  /**
     * props
     *  - fileSourceId 文件源id
     *  - column 列展示信息
     *  - rowSelection 行选择信息
     *  - renderHeader 选择列表格头渲染
     *  - linkHandler 链接回调
     *  - actionHandler 文件操作回调
     *  - selectHandler 行选择回调
    */
  render(h, context) {
    const {
      fileSourceId = '',
      column = {},
      linkHandler = noop,
      actionHandler = noop,
      selectHandler = noop,
      renderHeader = noop,
      rowSelection = {},
    } = context.props;

    const {
      type,
      actions,
      label,
      field,
    } = column;

    // 渲染选择列
    if (checkIsCheckboxColumn(type)) {
      return (
        <bk-table-column
          key={`${context.data.key}_section`}
          width="50"
          renderHeader={renderHeader}
          scopedSlots={{
            default: ({ row }) => (
              <bk-checkbox
                disabled={!parseCondition(column.enable, row)}
                checked={!!rowSelection[row.completePath]}
                onChange={value => selectHandler(row.completePath, value)} />
            ),
          }} />
      );
    }
    // 渲染操作列
    if (checkIsButtonGroupColumn(type)) {
      return (
        <bk-table-column
          label={label}
          key={`${context.data.key}_${field}`}
          align="left"
          width={120}
          scopedSlots={{
            default: ({ row }) => actions.map((action) => {
              if (!parseCondition(action, row)) {
                return '';
              }
              // 跳转链接按钮
              if (checkIsLinkAction(action.type)) {
                return (
                  <bk-button
                    text
                    onClick={() => linkHandler(parseTarget(action.target, row))
                    }>
                    { action.label || row[field] || '--' }
                  </bk-button>
                );
              }
              // 按钮权限
              let ButtonTag = 'bk-button';
              let buttonAuth = {};
              if (action.actionType === 'DELETE') {
                ButtonTag = 'auth-button';
                buttonAuth = {
                  props: {
                    auth: 'file_source/delete',
                    permission: row.canManage,
                    resourceId: fileSourceId,
                  },
                };
              }

              // 需要二次确认按钮
              if (action.displayType === 'Confirm') {
                return (
                  <jb-popover-confirm
                    title={actionConfirmInfo[action.actionType].title}
                    content={actionConfirmInfo[action.actionType].content}
                    confirm-handler={() => actionHandler(
                      action.actionCode,
                      parseParams(action.params, row),
                    )
                    }>
                    <ButtonTag
                      { ...buttonAuth }
                      text>
                      { action.label || row[field] || '--' }
                    </ButtonTag>
                  </jb-popover-confirm>
                );
              }
              return (
                <ButtonTag
                  { ...buttonAuth }
                  text
                  onClick={() => actionHandler(action.actionCode, parseParams(action.params, row))}>
                  { action.label || row[field] || '--' }
                </ButtonTag>
              );
            }),
          }} />
      );
    }

    return (
      <bk-table-column
        label={label}
        key={`${context.data.key}_${field}`}
        align="left"
        scopedSlots={{
          default: ({ row }) => {
            if (!actions) {
              return <span>{ row[field] || '--' }</span>;
            }
            return actions.map((action) => {
              if (!parseCondition(action, row)) {
                return (
                  <div>
                    <icon type="file-full" class="mr10" style="color: #C4C6CC" />
                    <span>{ row[field] || '--' }</span>
                  </div>
                );
              }

              return (
                <bk-button
                  text
                  onClick={() => linkHandler(parseTarget(action.target, row))}>
                  <icon type="folder-full" class="mr10" style="color: #699DF4" />
                  { action.label || row[field] || '--' }
                </bk-button>
              );
            });
          },
        }} />
    );
  },
};
