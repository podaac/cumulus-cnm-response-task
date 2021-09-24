# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# [v2.0.0] - 2021-09-24
### Added
### Changed
- **CUMULUS-2388**
  - Updated CNMResponse task to use fileName, bucket and key from Cumulus granule files object.
### Deprecated
### Removed
### Fixed
### Security

# [v1.4.1] - 2021-07-21
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core:1.11.1013 -> 1.12.28
  - Upgrade com.amazonaws:aws-java-sdk-kinesis:1.11.1013 -> 1.12.28
  - Upgrade com.amazonaws:aws-java-sdk-sns:1.11.1013 -> 1.12.28
  - Upgrade com.amazonaws:amazon-kinesis-client:1.14.0 -> 1.14.4

# [v1.4.0] - 2021-05-06
### Added
### Changed
- **PCESA-2418**
    - Updated CNMResponse task to not require cmrConceptId and cmrLink in granule input.
### Deprecated
### Removed
### Fixed
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core:1.11.955 -> 1.11.1013
  - Upgrade com.amazonaws:aws-java-sdk-kinesis:1.11.955 -> 1.11.1013
  - Upgrade com.amazonaws:aws-java-sdk-sns:1.11.955 -> 1.11.1013
  - Upgrade commons-io:commons-io:2.6 -> 2.7

# [v1.3.1] - 2021-02-17
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core:1.11.922 -> 1.11.955
  - Upgrade com.amazonaws:aws-java-sdk-kinesis:1.11.924 -> 1.11.955
  - Upgrade com.amazonaws:aws-java-sdk-sns:1.11.924 -> 1.11.955
  
# [v1.3.0] - 2020-12-22
### Added
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-2552**
    - Catch exceptions during 'PerformFunction' and always send a failure response message, before re-throwing the 
      exception.
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core:1.11.903 -> 1.11.922
  - Upgrade com.amazonaws:aws-java-sdk-kinesis:1.11.903 -> 1.11.924
  - Upgrade com.amazonaws:aws-java-sdk-sns:1.11.903 -> 1.11.924
    
# [v1.2.0] - 2020-12-15
### Added
- **PODAAC-2783**
  - Added following SNS Message Attributes to CNMResponse
     - COLLECTION
     - CNM_RESPONSE_STATUS
     - DATA_VERSION
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core:1.11.660 -> 1.11.903
  - Upgrade com.amazonaws:aws-java-sdk-kinesis:1.11.439 -> 1.11.903
  - Upgrade com.amazonaws:aws-java-sdk-sns:1.11.342 -> 1.11.903
  
# [v1.1.1] - 2020-11-19
### Added
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-2775**
  - Upgrade to CMA-java v1.3.2 to fix timeout on large messages.
### Security

# [1.1.0] - 2020-11-08

### Added
- **PODAAC-2541**
  - fix high severity findings reported in Snyk.
- **PODAAC-2546**
  - CNM schema change which requires CNM-R to include product object.
- **PODAAC-2551**
  - a build script under /builder directory, and a jenkins job to build and push release to public github.
  
### Changed
- **PODAAC-2641**
  - Added UTC timezone to processCompleteTime.
- **PODAAC-2549**
  - Update unit tests to mock AWS.

### Deprecated

### Removed

### Fixed

### Security

# [1.0.8]  06-30-2020

### Added

### Changed
- **PCESA-2166**
  - Updated lambda to use CMA AdapterLogger for logging to Elasticsearch.
### Deprecated

### Removed

### Fixed

### Security

## [1.0.7] - 2020-05-13

### Added

- **PCESA-1801**
  - Added support for sending CNM response to 1 or more SNS topics or Kinesis streams.

### Changed

### Deprecated

### Removed

### Fixed

### Security
