import { ref } from 'vue';

const key = ref('ip');

// 仅支持设置为 ip、ipv6
const setKey = (value) => {
    if (!['ip', 'ipv6'].includes(value)) {
        return;
    }
    key.value = value;
};

export default () => ({
    key,
    setKey,
});
