import _ from 'lodash';
import {
    getCurrentInstance,
    onBeforeUnmount,
    onMounted,
    ref,
} from 'vue';

import Manager from '../../../../../manager';

let widthMemo = {};

export default function (tableRef, tableColumnResizeRef) {
    const memoCacheKey = `BKIpSelectorHostTableColumnWidth${Manager.config.version}`;
    widthMemo = JSON.parse(localStorage.getItem(memoCacheKey) || '{}');
    const currentInstance = getCurrentInstance();
    let dragable = false;
    const dragging = ref(false);
    const dragState = ref({});

    const initColumnWidth = () => {
        setTimeout(() => {
            const tableEl = tableRef.value;
            const {
                width: totalWidth,
            } = tableEl.getBoundingClientRect();

            let columnWidthAdd = 0;
            let firstKeyColumnEl;
            tableEl.querySelectorAll('th').forEach((columnEl) => {
                const role = columnEl.getAttribute('role');
                let width = 60;
                if (widthMemo[role]) {
                    width = parseInt(widthMemo[role], 10);
                } else {
                    width = parseInt(columnEl.getAttribute('data-width') || '60', 10);
                }
                if (columnEl.classList.contains('host-column-first-key')) {
                    firstKeyColumnEl = columnEl;
                } else {
                    const renderWidth = Math.max(width, 60);
                    columnWidthAdd += renderWidth;
                    columnEl.style.width = `${renderWidth}px`;
                }
            });

            if (firstKeyColumnEl) {
                let width = 180;
                const role = firstKeyColumnEl.getAttribute('role');
                if (widthMemo[role]) {
                    width = Math.max(parseInt(widthMemo[role], 10), width);
                }
                const lastWidth = totalWidth - columnWidthAdd;
                firstKeyColumnEl.style.width = `${Math.max(lastWidth, width)}px`;
            }
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
                const columnWidth = parseInt(Math.max(finalLeft - startColumnLeft, 60), 10);

                columnEl.style.width = `${columnWidth}px`;
                resizeProxy.style.display = 'none';
                document.body.style.cursor = '';
                dragging.value = false;
                dragState.value = {};
                currentInstance.proxy.initalScroll();
                const columnRole = columnEl.getAttribute('role');
                if (columnRole) {
                    widthMemo[columnRole] = columnWidth;
                    localStorage.setItem(memoCacheKey, JSON.stringify(widthMemo));
                }
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
