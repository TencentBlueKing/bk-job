import {
    onBeforeUnmount,
    reactive,
    toRefs,
} from 'vue';

import useIpSelector from './use-ip-selector';

const size = reactive({
    width: 0,
    height: 0,
    contentHeight: 0,
    panelHeight: 0,
});

export default () => {
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
            const {
                width,
                height,
            } = context.rootRef.value.getBoundingClientRect();
            size.width = width;
            size.height = Math.max(height, 300);
        }
        size.contentHeight = size.height - headerHeight - footerHeight;
    };

    if (size.width < 1) {
        handleResize();

        window.addEventListener('resize', handleResize);

        onBeforeUnmount(() => {
            window.removeEventListener('resize', handleResize);
        });
    }

    return {
        ...toRefs(size),
    };
};
