import Manager from '../manager';
import { transformTopoTree } from '../utils';

export default (lazyLoadedSuccessCallback = () => {}) => {
    const lazyDisabledCallbak = node => !(node.data.payload.lazy && node.data.children.length < 1);

    const lazyMethodCallback = node => Promise.resolve()
        .then(() => {
            if (node.data.payload.lazy) {
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
