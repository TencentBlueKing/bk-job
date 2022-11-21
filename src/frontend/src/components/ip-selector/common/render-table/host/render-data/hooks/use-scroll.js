import _ from 'lodash';
import {
    onBeforeUnmount,
    onMounted,
    shallowRef,
    useSlots,
} from 'vue';

export default function (tableContentRef) {
    const leftFixedStyles = shallowRef({});
    const rightFixedStyles = shallowRef({});

    const slots = useSlots();
    
    const handleHorizontalScroll = _.throttle(() => {
        const tableEl = tableContentRef.value;
        const { scrollLeft } = tableEl;
        const tableContentWidth = tableEl.getBoundingClientRect().width;
        const tableWidth = tableEl.querySelector('table').getBoundingClientRect().width;
        if (scrollLeft === 0) {
            leftFixedStyles.value = {
                display: 'none',
            };
        } else {
            const fixedColumns = tableEl.querySelectorAll('th.columu-fixed');
            const fixedWidth = Array.from(fixedColumns)
                .reduce((result, itemEl) => result + itemEl.getBoundingClientRect().width, 0);
            leftFixedStyles.value = {
                width: `${fixedWidth}px`,
            };
        }
        
        if (tableContentWidth + scrollLeft >= tableWidth) {
            rightFixedStyles.value = {
                display: 'none',
            };
        } else if (slots.action) {
            const fixedRightColumns = tableEl.querySelectorAll('th.columu-fixed-right');
            const fixeRightdWidth = Array.from(fixedRightColumns)
                .reduce((result, itemEl) => result + itemEl.getBoundingClientRect().width, 0);
            rightFixedStyles.value = {
                width: `${fixeRightdWidth}px`,
            };
        }
    }, 30);

    onMounted(() => {
        const tableEl = tableContentRef.value;
        tableEl.addEventListener('scroll', handleHorizontalScroll);
        onBeforeUnmount(() => {
            tableEl.removeEventListener('scroll', handleHorizontalScroll);
        });
    });

    return {
        leftFixedStyles,
        rightFixedStyles,
        initalScroll: handleHorizontalScroll,
    };
}
