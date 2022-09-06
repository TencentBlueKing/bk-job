import { nextTick } from 'vue';

export default callback => (value, event) => {
    if (event.isComposing) {
        // 跳过输入法复合事件
        return;
    }
    
    // 输入框的值被清空直接触发搜索
    // enter键开始搜索
    if (event.keyCode === 13
     || event.type === 'click') {
        nextTick(() => {
            callback();
        });
    }
};
