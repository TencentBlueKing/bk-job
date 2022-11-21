export const makeMap = list => list.reduce((result, item) => ({
    ...result,
    [item]: true,
}), {});
