export default {
    functional: true,
    props: {
        stepData: Object,
        isTotalBtnFixed: Boolean,
        selectBatch: Number,
    },
    render (h, context) {
        const {
            stepData,
            isTotalBtnFixed,
            selectBatch,
        } = context.props;

        // 滚动全量执行(滚动步骤)不支持查看全部批次操作
        const disabled = stepData.runMode === 2;

        const classes = {
            'all-btn': true,
            fixed: isTotalBtnFixed,
            active: selectBatch === 0,
            disabled,
        };

        const handleClick = () => {
            if (disabled) {
                return;
            }
            context.listeners['on-change'] && context.listeners['on-change'](0);
        };

        return (
            <div
                ref="allBtn"
                class={classes}
                key="all"
                onClick={handleClick}>
                全部批次
            </div>
        );
    },
};
