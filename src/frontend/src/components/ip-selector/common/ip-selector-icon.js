export default {
    name: 'Icon',
    props: {
        type: {
            type: String,
            required: true,
        },
        svg: {
            type: Boolean,
            default: false,
        },
    },
    
    render (h) {
        if (this.svg) {
            return h('svg', {
                class: {
                    'job-svg-icon': true,
                },
                style: {
                    width: '1em',
                    height: '1em',
                    'box-sizing': 'content-box',
                    fill: 'currentColor',
                },
                props: this.$attrs,
                on: this.$listeners,
            }, [
                h('use', {
                    attrs: {
                        'xlink:href': `#bk-ipselector-${this.type}`,
                    },
                }),
            ]);
        }
        const classes = {
            'bk-ipselector-icon': true,
            [`bk-ipselector-${this.type}`]: true,
        };
        return h('i', {
            class: classes,
            props: this.$attrs,
            on: this.$listeners,
        });
    },
};
