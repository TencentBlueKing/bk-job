import { ref } from 'vue';

export default (treeRef) => {
    const expanded = ref(false);

    const toggleExpanded = () => {
        expanded.value = !expanded.value;
        treeRef.value.nodes.forEach((node) => {
            if (!node.isLeaf) {
                treeRef.value.setExpanded(node.id, {
                    expanded: expanded.value,
                    emitEvent: false,
                });
            }
        });
    };

    return {
        expanded,
        toggleExpanded,
    };
};
