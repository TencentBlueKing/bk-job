export const encodeRegexp = (paramStr) => {
    const regexpKeyword = [
        '\\', '.', '*', '-', '{', '}', '[', ']', '^', '(', ')', '$', '+', '?', '|',
    ];
    const res = regexpKeyword.reduce(
        (result, charItem) => result.replace(new RegExp(`\\${charItem}`, 'g'), `\\${charItem}`),
        paramStr,
    );
    return res;
};
