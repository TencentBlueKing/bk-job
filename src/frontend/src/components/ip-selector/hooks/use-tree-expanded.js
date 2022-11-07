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

    const calcShowExpanded = (node) => {
        if (node.isLeaf) {
            return false;
        }
        // eslint-disable-next-line no-plusplus
        for (let i = 0; i < node.children.length; i++) {
            if (!node.children[i].isLeaf) {
                return true;
            }
        }
        return false;
    };

    return {
        toggleExpanded,
        calcShowExpanded,
    };
};
