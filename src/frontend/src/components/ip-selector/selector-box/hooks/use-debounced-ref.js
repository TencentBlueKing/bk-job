import { customRef } from 'vue';

export default (value, delay = 200) => {
    let timeout;
    let localValue = value;
    return customRef((track, trigger) => ({
        get () {
            track();
            return localValue;
        },
        set (newValue) {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                localValue = newValue;
                trigger();
            }, delay);
        },
    }));
};
