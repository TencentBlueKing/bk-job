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
            active: isTotalBtnFixed || selectBatch === 0,
            disabled,
        };

        const clickHandler = () => {
            if (disabled) {
                return;
            }
            context.listeners['on-change'] && context.listeners['on-change'](0);
        };

        return (
            <div
                ref="allBtn"
                class="all-btn"
                class={classes}
                key="all"
                onClick={clickHandler}>
                全部批次
            </div>
        );
    },
};
