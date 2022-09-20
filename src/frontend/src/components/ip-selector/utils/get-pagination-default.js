export const getPaginationDefault = (height) => {
    const defaultLimit = Math.floor(height / 41) - 2;
    const pagination = {
        count: 0,
        current: 1,
        limit: defaultLimit,
        limitList: [...new Set([10, 20, 50, 100, defaultLimit])].sort((a, b) => a - b),
    };

    return pagination;
};
