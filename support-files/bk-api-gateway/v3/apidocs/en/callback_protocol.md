### Function Description

This component is only used to present the callback protocol when job finished.

### Request Headers
Content-Type: application/json

Compatibility Note:

To ensure backward compatibility, previous versions of Job used Content-Type: application/x-www-form-urlencoded for callback requests, and some existing clients have integrated based on that format. In the current version, Job will prioritize using Content-Type: application/json for callbacks. If the callback fails, it will retry once with Content-Type: application/x-www-form-urlencoded to accommodate legacy clients. New clients must parse callback requests using Content-Type: application/json.

### Request Parameters

| Fields             | Type  | Required | Description                                                                                                                                                                                                                          |
|--------------------|-------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| job_instance_id    | long  | yes      | Job instance ID                                                                                                                                                                                                                      |
| status             | int   | yes      | Job status code. 1 - Pending; 2 - Running 3 - Successful; 4 - Failed; 5 - Skipped; 6 - Ignore Error; 7 - Waiting; 8 - Terminated; 9 - Abnormal; 10 - Terminating; 11 - Terminate Success; 13 - Termination Confirmed; 14 - Abandoned |
| step_instance_list | array | yes      | The execution result of steps                                                                                                                                                                                                        |

#### step_instances

| Fields           | Type | Required | Description                                                                                                                                                                                                                               |
|------------------|------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| step_instance_id | long | yes      | Job step instance ID                                                                                                                                                                                                                      |
| status           | int  | yes      | Job step status code: 1 - Pending; 2 - Running 3 - Successful; 4 - Failed; 5 - Skipped; 6 - Ignore Error; 7 - Waiting; 8 - Terminated; 9 - Abnormal; 10 - Terminating; 11 - Terminate Success; 13 - Termination Confirmed; 14 - Abandoned |

### Example of request

```json
{
  "job_instance_id": 12345,
  "status": 2,
  "step_instance_list": [
    {
      "step_instance_id": 16271,
      "status": 3
    },
    {
      "step_instance_id": 16272,
      "status": 2
    }
  ]
}
```

### Callback response

The successful callback is based on the HTTP status. If successful, the status code is 200, while others indicate failure.
The current version of the job will prioritize using Content-Type: application/json executes callback. If successful, the callback ends. If failed, job will try again with Content-Type: application/x-www-form-urlencoded to be compatible with clients parsing according to the old protocol.
