import Manager from '../manager';
import { transformTopoTree } from '../utils';

const getLeafIdList = childrenList => childrenList.reduce((result, item) => {
    if (item.children.length < 1) {
        result.push(item.id);
    }
    return result;
}, []);

export default (lazyLoadedSuccessCallback = () => {}) => {
    let isLazy = false;
    const lazyDisabledCallbak = (node) => {
        if (node.data.payload.lazy) {
            isLazy = true;
        }
        return !isLazy;
    };

    const lazyMethodCallback = node => Promise.resolve()
        .then(() => {
            if (node.data.payload.lazy) {
                const nodeInfoData = node.data.payload;
                return Manager.service.fetchTopologyHostCount({
                    [Manager.nameStyle('objectId')]: nodeInfoData.object_id,
                    [Manager.nameStyle('instanceId')]: nodeInfoData.instance_id,
                    [Manager.nameStyle('meta')]: nodeInfoData.meta,
                })
                    .then((data) => {
                        const childData = transformTopoTree(data);
                        return {
                            data: childData,
                            leaf: getLeafIdList(childData),
                        };
                    });
            }
            
            return {
                data: node.data.children,
                leaf: getLeafIdList(node.data.children),
            };
        })
        .finally(() => {
            setTimeout(() => {
                lazyLoadedSuccessCallback();
            });
        });

    return {
        lazyDisabledCallbak,
        lazyMethodCallback,
    };
};
