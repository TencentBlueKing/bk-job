import {
    onBeforeUnmount,
    reactive,
    toRefs,
} from 'vue';

import useIpSelector from './use-ip-selector';

export default () => {
    const size = reactive({
        width: 0,
        height: 0,
        contentHeight: 0,
        panelHeight: 0,
    });

    const context = useIpSelector();

    const headerHeight = 41;
    const footerHeight = 57;

    const handleResize = () => {
        if (context.mode === 'dialog') {
            const windowWidth = window.innerWidth;
            if (windowWidth <= 1440) {
                size.width = 1266;
                size.height = 570;
            } else if (windowWidth <= 1680) {
                size.width = 1340;
                size.height = 670;
            } else {
                size.width = 1600;
                size.height = 800;
            }
        } else {
            size.height = Math.max(context.rootRef.value.getBoundingClientRect().height, 570);
        }
        size.contentHeight = size.height - headerHeight - footerHeight;
    };

    window.addEventListener('resize', handleResize);
    
    handleResize();

    onBeforeUnmount(() => {
        window.removeEventListener('resize', handleResize);
    });

    return {
        ...toRefs(size),
    };
};
