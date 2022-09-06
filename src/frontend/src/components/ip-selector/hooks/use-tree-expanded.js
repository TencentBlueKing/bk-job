const getChildrenIdList = (node) => {
    const result = [];
    node.children.forEach((child) => {
        result.push(child.id);
        result.push(...getChildrenIdList(child));
    });

    return result;
};

export default (treeRef) => {
    const toggleExpanded = (node) => {
        const childIdList = getChildrenIdList(node);
        childIdList.forEach((nodeId) => {
            treeRef.value.setExpanded(nodeId, {
                expanded: true,
                emitEvent: false,
            });
        });
    };

    return {
        toggleExpanded,
    };
};
