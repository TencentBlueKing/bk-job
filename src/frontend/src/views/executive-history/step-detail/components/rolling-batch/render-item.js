const BATCH_STATUS_RUNNING = 2;
const BATCH_STATUS_FAIL = 4;
const BATCH_STATUS_MANUAL_CONFIRM = 7;
    
export default {
    functional: true,
    props: {
        selectBatch: Number,
        data: Object,
        currentRunningBatch: Number,
    },
    render (h, context) {
        const {
            data,
            selectBatch,
            currentRunningBatch,
        } = context.props;

        const active = data.batch === selectBatch;
        const disabled = data.batch > currentRunningBatch;

        const clasess = {
            'batch-item': true,
            active,
            disabled,
            confirm: data.status === BATCH_STATUS_MANUAL_CONFIRM,
            fail: data.status === BATCH_STATUS_FAIL,
        };

        const handleClick = (event) => {
            if (disabled || active) {
                return;
            }
            console.log('from render item handleclic = ', event);
            context.listeners['on-change'] && context.listeners['on-change'](data.batch, event);
        };

        const renderConfirmStatus = () => {
            if (data.batch === currentRunningBatch
                && data.status === BATCH_STATUS_MANUAL_CONFIRM) {
                return (
                    <div class="batch-item-status">
                        <Icon type="stop-2" style="color: #FF9C01" />
                    </div>
                );
            }
            return null;
        };

        const renderFailedStatus = () => {
            if (data.batch === currentRunningBatch
                && data.status === BATCH_STATUS_FAIL) {
                return (
                    <div class="batch-item-status">
                        <Icon type="wrong" style="color: #FF5656" />
                    </div>
                );
            }
            return null;
        };
        const renderExecutingStatus = () => {
            if (data.batch === currentRunningBatch
                && data.status === BATCH_STATUS_RUNNING) {
                return (
                    <div class="batch-item-status rotate-loading">
                        <Icon type="batch-loading" style="color: #3a84ff" />
                    </div>
                );
            }
            return null;
        };
        return (
            <div
                class={clasess}
                key={data.batch}
                onClick={handleClick}>
                第 { data.batch } 批
                {renderConfirmStatus()}
                {renderFailedStatus()}
                {renderExecutingStatus()}
            </div>
        );
    },
};
