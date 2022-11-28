import _ from 'lodash';
import {
    getCurrentInstance,
    onBeforeUnmount,
    onMounted,
    ref,
} from 'vue';

export default function (tableRef, tableColumnResizeRef) {
    const currentInstance = getCurrentInstance();
    let dragable = false;
    const dragging = ref(false);
    const dragState = ref({});

    const initColumnWidth = () => {
        setTimeout(() => {
            const tableEl = tableRef.value;
            tableEl.querySelectorAll('th').forEach((columnEl) => {
                const { width } = columnEl.getBoundingClientRect();
                if (columnEl.classList.contains('host-column-first-key')) {
                    columnEl.style.width = `${Math.max(width, 180)}px`;
                } else {
                    columnEl.style.width = `${Math.max(width, 60)}px`;
                }
            });
        });
    };

    const handleMouseDown = (event, columnKey) => {
        if (!dragable) {
            return;
        }
        dragging.value = true;

        const tableEl = tableRef.value;
        const tableLeft = tableEl.getBoundingClientRect().left;
        const columnEl = tableEl.querySelector(`th.host-column-${columnKey}`);
        const columnRect = columnEl.getBoundingClientRect();
        const minLeft = columnRect.left - tableLeft + 30;

        dragState.value = {
            startMouseLeft: event.clientX,
            startLeft: columnRect.right - tableLeft,
            startColumnLeft: columnRect.left - tableLeft,
            tableLeft,
        };
        const resizeProxy = tableColumnResizeRef.value;
        resizeProxy.style.display = 'block';
        resizeProxy.style.left = `${dragState.value.startLeft}px`;

        document.onselectstart = function () {
            return false;
        };
        document.ondragstart = function () {
            return false;
        };

        const handleMouseMove = (event) => {
            const deltaLeft = event.clientX - (dragState.value).startMouseLeft;
            const proxyLeft = (dragState.value).startLeft + deltaLeft;
            resizeProxy.style.display = 'block';
            resizeProxy.style.left = `${Math.max(minLeft, proxyLeft)}px`;
        };

        const handleMouseUp = () => {
            if (dragging.value) {
                const { startColumnLeft } = dragState.value;
                const finalLeft = Number.parseInt(resizeProxy.style.left, 10);
                const columnWidth = Math.max(finalLeft - startColumnLeft, 60);

                columnEl.style.width = `${columnWidth}px`;
                resizeProxy.style.display = 'none';
                document.body.style.cursor = '';
                dragging.value = false;
                dragState.value = {};
                currentInstance.proxy.initalScroll();
            }
            dragable = false;

            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
            document.onselectstart = null;
            document.ondragstart = null;
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);
    };

    const handleMouseMove = (event) => {
        const target = event.target.closest('th');

        const rect = target.getBoundingClientRect();

        const bodyStyle = document.body.style;
        if (rect.width > 12 && rect.right - event.pageX < 8) {
            bodyStyle.cursor = 'col-resize';
            dragable = true;
        } else if (!dragging.value) {
            bodyStyle.cursor = '';
            dragable = false;
        }
    };

    const handleOuterMousemove = _.throttle((event) => {
        let i = event.path.length - 1;
        while (i >= 0) {
            if (event.path[i].id === 'bkIPSelectorHostTableHead') {
                return;
            }
            i = i - 1;
        }
        document.body.style.cursor = '';
    }, 500);

    onMounted(() => {
        document.addEventListener('mousemove', handleOuterMousemove);
    });

    onBeforeUnmount(() => {
        document.removeEventListener('mousemove', handleOuterMousemove);
    });
    return {
        initColumnWidth,
        handleMouseDown,
        handleMouseMove,
    };
}
