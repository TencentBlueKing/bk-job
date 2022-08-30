import _ from 'lodash';

export const getObjectValueByPath = (obj, path) => _.get(obj, path);
