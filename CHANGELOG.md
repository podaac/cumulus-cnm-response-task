# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

# [v1.5.0] - 2022-04-28
### Added
- **PODAAC-4445**
  - Add in new field `dataProcessingType` in the `MessageAttributes` section for SNS topics; would only populate when field existsq
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core@1.12.144 to com.amazonaws:aws-java-sdk-core@1.12.201
  - Upgrade com.amazonaws:amazon-kinesis-client@1.14.7 to com.amazonaws:amazon-kinesis-client@1.14.8

# [v1.4.4] - 2021-01-21
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **PODAAC-4095**
  - Upgrade to cumulus-message-adapter-java 1.3.9 to address [log4j vulnerability](https://nvd.nist.gov/vuln/detail/CVE-2021-44832)
- **Snyk**
  - Upgrade com.amazonaws:aws-java-sdk-core@1.12.28 to com.amazonaws:aws-java-sdk-core@1.12.144
  - Upgrade com.amazonaws:aws-java-sdk-sns@1.12.28 to com.amazonaws:aws-java-sdk-sns@1.12.144
  - Upgrade com.amazonaws:aws-java-sdk-kinesis@1.12.28 to com.amazonaws:aws-java-sdk-kinesis@1.12.144
  - Upgrade com.amazonaws:amazon-kinesis-client@1.14.4 to com.amazonaws:amazon-kinesis-client@1.14.7
  - Upgrade com.google.code.gson:gson@2.8.2 to com.google.code.gson:gson@2.8.9
  - Upgrade commons-io@2.7 to commons-io@2.11.0

# [v1.4.3] - 2021-12-22
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **PODAAC-4059**
  - Upgrade to cumulus-message-adapter-java 1.3.7 to address [log4j vulnerability](https://nvd.nist.gov/vuln/detail/CVE-2021-45105)

# [v1.4.2] - 2021-12-15
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **PODAAC-4046**
  - Upgrade to cumulus-message-adapter-java 1.3.5 to address log4j vulnerability

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
