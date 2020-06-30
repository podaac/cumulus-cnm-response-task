# `cnm-response` task

## Installation

To build the Lambda code:

```shell
mvn clean dependency:copy-dependencies
gradle build
```

The build artifact will be generated in `build/distributions` and can be uploaded directly to AWS Lambda as the source code.

### Sample workflow configuration

```yaml
CnmResponse:
  CumulusConfig:
    OriginalCNM: '{$.meta.cnm}'
    CNMResponseStream: 'ProviderResponse'
    WorkflowException: '{$.exception}'
    region: 'us-west-2'
    cumulus_message:
      outputs:
        - source: '{$}'
          destination: '{$.meta.cnmResponse}'
  Type: Task
  Resource: ${CnmResponseLambdaFunction.Arn}
  Retry:
    - ErrorEquals:
        - States.ALL
      IntervalSeconds: 5
      MaxAttempts: 3
  Catch:
    - ErrorEquals:
      - States.ALL
      ResultPath: '$.exception'
      Next: StopStatus
  Next: StopStatus
```

- OriginalCNM is the input CNM to the function. We save this during our Translate operation that we do in the beginning of the workflow.
- CNMResponseStream is the name of the stream to write out to- Note this is not an ARN right now.
- WorkflowException is the 'exception' field from the workflow. Probably don't need to change this.
- region is the region of AWS that the ResponseStream resides in.
