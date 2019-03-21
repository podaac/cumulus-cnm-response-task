mvn dependency:copy-dependencies
gradle -x test build
curl -u jenkins:${JENKINS_ARTIFACT_TOKEN} -X PUT https://podaac-ci.jpl.nasa.gov:8443/artifactory/ext-release-local/gov/nasa/cumulus/cnmResponse/1.0.4/cnmResponse-1.0.4.zip  -T build/distributions/cnmResponse-1.0.4.zip
