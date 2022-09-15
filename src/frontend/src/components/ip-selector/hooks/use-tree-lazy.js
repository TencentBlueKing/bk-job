import Manager from '../manager';
import { transformTopoTree } from '../utils';

export default () => {
    const lazyDisabledCallbak = node => !node.data.lazy && node.data.children.length < 1;

    const lazyMethodCallback = (node) => {
        if (node.data.lazy) {
            const nodeInfoData = node.data.payload;
            return Manager.service.fetchTopologyHostCount({
                [Manager.nameStyle('objectId')]: nodeInfoData.object_id,
                [Manager.nameStyle('instanceId')]: nodeInfoData.instance_id,
                [Manager.nameStyle('meta')]: nodeInfoData.meta,
            })
                .then(data => ({
                    data: transformTopoTree(data),
                    leaf: [],
                }));
        }
        return {
            data: node.data.children,
            leaf: [],
        };
    };

    return {
        lazyDisabledCallbak,
        lazyMethodCallback,
    };
};
