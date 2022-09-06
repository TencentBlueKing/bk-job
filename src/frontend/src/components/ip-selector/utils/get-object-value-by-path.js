export const getObjectValueByPath = (obj, path) => {
    const stack = path.split('.');
    let result = obj;
    while (stack.length && result) {
        const key = stack.shift();
        result = result[key];
    }
    return result;
};
