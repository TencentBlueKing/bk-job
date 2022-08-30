import { ref } from 'vue';

const key = ref('ip');

// 仅支持设置为 ip、ipv6
const setKey = (value) => {
    console.log('from setteatekekekek = ', value);
    if (!['ip', 'ipv6'].includes(value)) {
        return;
    }
    console.log('from setteatekekekek = ', value);
    key.value = value;
};

export default () => ({
    key,
    setKey,
});
