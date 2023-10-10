# `cnm-response` task

## Installation

To build the Lambda code, Refer to following Confluence page:
https://wiki.jpl.nasa.gov/pages/viewpage.action?spaceKey=PD&title=SonarQube%2C+Jacoco+and+Java+17+upgrade

```shell
* Build with sonarQube and Jacoco report
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=cnm-response-opensource \
  -Dsonar.projectName='cnm-response-opensource' \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=sqp_fc2271c4ddba6507fb38ea64d52e6912e2c6e14d
  
* Makde sure using java 17 and gradle 8.3
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
