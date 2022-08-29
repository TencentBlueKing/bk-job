export const getDiffNewNum = diffMap => Object.values(diffMap).reduce((result, item) => {
    if (item === 'new') {
        return result + 1;
    }
    return result;
}, 0);
