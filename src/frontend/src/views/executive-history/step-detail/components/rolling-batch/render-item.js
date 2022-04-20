const BATCH_STATUS_RUNNING = 2;
const BATCH_STATUS_SUCCESS = 3;
const BATCH_STATUS_FAIL = 4;
const BATCH_STATUS_INGORE_ERROR = 6;
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
        const will = data.batch > currentRunningBatch;

        const clasess = {
            'batch-item': true,
            active,
            will,
            confirm: data.status === BATCH_STATUS_MANUAL_CONFIRM,
            fail: data.status === BATCH_STATUS_FAIL,
        };

        const handleClickSelect = (event) => {
            context.listeners['on-change']
             && context.listeners['on-change'](data.batch, event);
        };

        const renderSuccessIcon = () => {
            if (data.batch === selectBatch
                 && data.status === BATCH_STATUS_SUCCESS) {
                return (
                    <div class="batch-item-status">
                        <Icon type="check-line" style="color: #2dc89d" />
                    </div>
                );
            }
        };

        const renderConfirmIcon = () => {
            if (data.status !== BATCH_STATUS_MANUAL_CONFIRM) {
                return null;
            }
            if (data.status === selectBatch
                || data.batch === currentRunningBatch) {
                return (
                    <div class="batch-item-status">
                        <Icon type="stop-2" style="color: #FF9C01" />
                    </div>
                );
            }
            return null;
        };

        const renderFailedIcon = () => {
            if (![
                BATCH_STATUS_FAIL,
                BATCH_STATUS_INGORE_ERROR,
            ].includes(data.status)) {
                return null;
            }
            if (data.batch === selectBatch
                || data.batch === currentRunningBatch) {
                return (
                    <div class="batch-item-status">
                        <Icon type="wrong" style="color: #FF5656" />
                    </div>
                );
            }
            return null;
        };

        const renderExecutingIcon = () => {
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
                onClick={handleClickSelect}>
                第 { data.batch } 批
                {renderSuccessIcon()}
                {renderConfirmIcon()}
                {renderFailedIcon()}
                {renderExecutingIcon()}
            </div>
        );
    },
};
