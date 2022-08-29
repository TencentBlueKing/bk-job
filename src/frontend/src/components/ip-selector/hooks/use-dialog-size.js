import {
    toRefs,
    reactive,
    onBeforeUnmount,
} from 'vue';

export default () => {
    const size = reactive({
        width: 0,
        height: 0,
        contentHeight: 0,
    });
    const headerAndFooterHeight = 98;

    const handleResize = () => {
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
        size.contentHeight = size.height - headerAndFooterHeight;
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
