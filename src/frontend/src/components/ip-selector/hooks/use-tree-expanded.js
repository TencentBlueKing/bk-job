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
        const expanded = !node.expanded;
        treeRef.value.setExpanded(node.id, {
            expanded,
            emitEvent: false,
        });
        const childIdList = getChildrenIdList(node);
        childIdList.forEach((nodeId) => {
            treeRef.value.setExpanded(nodeId, {
                expanded,
                emitEvent: false,
            });
        });
    };

    return {
        toggleExpanded,
    };
};
