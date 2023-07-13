import _ from 'lodash';
import Vue from 'vue';

import GlobalVariableModel from '@model/task/global-variable';

import I18n from '@/i18n';

const VALID_HOST_ID_FLAG = 0;

const instance = new Vue();
let messageInstance;

const checkIllegalHost = hostNodeInfo => _.find(hostNodeInfo.hostList, item => item.hostId < VALID_HOST_ID_FLAG);

// !important
// 全局变量中有两个字段存储主机信息
// defaultTragetValue 模版中存储变量的默认值
// targetValue 去执行时变量的实际值

// 执行场景检测全局变量的主机就行，这时使用不到步骤
export const checkIllegalHostFromVariableTargetValue = (
  variableList = [],
  clearCallback = () => {},
) => {
  // 检测全局变量
  const variableStack = [];
  variableList.forEach((variableItem) => {
    if (variableItem.type !== GlobalVariableModel.TYPE_HOST) {
      return;
    }
    if (checkIllegalHost(variableItem.targetValue.hostNodeInfo)) {
      variableStack.push(variableItem);
    }
  });

  messageInstance && messageInstance.close();
  if (variableStack.length > 0) {
    const variableNameList = variableStack.map(variableItem => variableItem.name);

    const handleClear = () => {
      messageInstance && messageInstance.close();
      instance.messageSuccess(I18n.t('清除成功'));
      clearCallback();
    };

    messageInstance = instance.$bkMessage({
      message: (() => {
        const h = instance.$createElement;
        return (
          <div>
            <div>
              <span>{I18n.t('变量.illegal')} [{variableNameList.join(',')}] {I18n.t('中存在失效主机，请处理后重试')}</span>
              <bk-button
                text
                theme="primary"
                onClick={handleClear}>
                {I18n.t('一键清除')}
              </bk-button>
            </div>
          </div>
        );
      })(),
      ellipsisLine: 0,
      delay: 0,
      theme: 'error',
      onClose: () => {
        messageInstance = null;
      },
    });

    return true;
  }

  return false;
};

// 检测全局变量和步骤
export const checkIllegalHostFromVariableStep = (
  globalVariableList = [],
  stepList = [],
  clearCallback = () => {},
) => {
  // 检测全局变量
  const variableStack = [];
  globalVariableList.forEach((variableItem) => {
    if (!variableItem.isHost) {
      return;
    }
    if (checkIllegalHost(variableItem.defaultTargetValue.hostNodeInfo)) {
      variableStack.push(variableItem);
    }
  });

  // 检测步骤
  const stepStask = [];
  stepList.forEach((stepItem) => {
    // 文件分发步骤
    // 需要判断源文件里面的主机和目标服务器里面的主机
    if (stepItem.isFile) {
      // 判断源文件
      let hasIllegalHost = _.some(stepItem.fileStepInfo.fileSourceList, (fileSourceItem) => {
        // 使用的是全局变量
        if (fileSourceItem.host.variable) {
          return false;
        }
        return checkIllegalHost(fileSourceItem.host.hostNodeInfo);
      });
      if (hasIllegalHost) {
        stepStask.push(stepItem);
        return;
      }
      // 目标服务器
      const { server } = stepItem.fileStepInfo.fileDestination;
      hasIllegalHost = server.variable ? false : checkIllegalHost(server.hostNodeInfo);
      if (hasIllegalHost) {
        stepStask.push(stepItem);
        return;
      }
    }

    // 执行脚本步骤
    if (stepItem.isScript) {
      // 目标服务器
      const {
        variable,
        hostNodeInfo,
      } = stepItem.scriptStepInfo.executeTarget;

      const hasIllegalHost = variable ? false : checkIllegalHost(hostNodeInfo);
      if (hasIllegalHost) {
        stepStask.push(stepItem);
      }
    }
  });

  messageInstance && messageInstance.close();
  if (variableStack.length > 0 || stepStask.length > 0) {
    const variableNameList = variableStack.map(variableItem => variableItem.name);
    const stepNameList = stepStask.map(stepItem => stepItem.name);

    const handleClear = () => {
      messageInstance && messageInstance.close();
      instance.messageSuccess(I18n.t('清除成功'));
      clearCallback();
    };

    messageInstance = instance.$bkMessage({
      message: (() => {
        const h = instance.$createElement;
        return (
          <div>
            <div>
              <span>{I18n.t('变量.illegal')} [{variableNameList.join(',')}] {I18n.t('中存在失效主机，请处理后重试')}</span>
              {
                stepNameList.length < 1 && (
                  <bk-button
                    text
                    theme="primary"
                    onClick={handleClear}>
                    {I18n.t('一键清除')}
                  </bk-button>
                )
              }
            </div>
            {
              stepNameList.length > 0 && (
                <div>
                  <span>{I18n.t('步骤.illegal')}[{stepNameList.join(',')}]{I18n.t('中存在失效主机，请处理后重试')}</span>
                  <bk-button
                    text
                    theme="primary"
                    onClick={handleClear}>
                      {I18n.t('一键清除')}
                    </bk-button>
                </div>
              )
            }
          </div>
        );
      })(),
      ellipsisLine: 0,
      delay: 0,
      theme: 'error',
      onClose: () => {
        messageInstance = null;
      },
    });

    return true;
  }

  return false;
};

export const removeIllegalHostFromHostNodeInfo = hostNodeInfo => ({
  ...hostNodeInfo,
  hostList: _.filter(hostNodeInfo.hostList, item => item.hostId > VALID_HOST_ID_FLAG),
});

// 移除变量中的异常主机
export const removeIllegalHostFromVariable = (variableList = []) => {
  const newVariableList = _.cloneDeepWith(variableList);
  // 处理全局变量
  newVariableList.forEach((variableItem) => {
    if (variableItem.isHost) {
      variableItem.defaultTargetValue.hostNodeInfo = removeIllegalHostFromHostNodeInfo(variableItem.defaultTargetValue.hostNodeInfo);
    }
  });
  return newVariableList;
};

// 移除步骤中的异常主机
export const removeIllegalHostFromStep = (stepList = []) => {
  const newStepList = _.cloneDeep(stepList);
  // 处理步骤
  newStepList.forEach((stepItem) => {
    // 文件分发步骤
    // 需要判断源文件里面的主机和目标服务器里面的主机
    if (stepItem.isFile) {
      // 判断源文件
      stepItem.fileStepInfo.fileSourceList.forEach((fileSourceItem) => {
        // 使用的是全局变量
        if (fileSourceItem.host.variable) {
          return false;
        }
        fileSourceItem.host.hostNodeInfo = removeIllegalHostFromHostNodeInfo(fileSourceItem.host.hostNodeInfo);
      });

      // 目标服务器
      const { server } = stepItem.fileStepInfo.fileDestination;
      if (!server.variable) {
        server.hostNodeInfo = removeIllegalHostFromHostNodeInfo(server.hostNodeInfo);
      }
    }

    // 执行脚本步骤
    if (stepItem.isScript) {
      // 目标服务器
      const {
        executeTarget,
      } = stepItem.scriptStepInfo;

      if (!executeTarget.variable) {
        executeTarget.hostNodeInfo = removeIllegalHostFromHostNodeInfo(executeTarget.hostNodeInfo);
      }
    }
  });

  return newStepList;
};
