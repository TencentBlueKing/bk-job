import GlobalVariableModel from '@model/task/global-variable';

export const createVariable = (() => {
    let id = -1;
    return (defaultId, type = GlobalVariableModel.TYPE_STRING) => new GlobalVariableModel({
        id: defaultId || id--, // eslint-disable-line no-plusplus,
        type,
    });
})();
