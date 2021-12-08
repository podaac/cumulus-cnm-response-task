import click
import os
import sys
import logging

TIMESTAMP_FORMAT = '%Y-%m-%dT%H:%M:%S'
SHORT_TIMESTAMP_FORMAT = '%Y-%m-%d'

logger:object = logging.getLogger('Artifact Builder ===>')
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)

stream_pom_version = os.popen('mvn help:evaluate -Dexpression=project.version -q -DforceStdout')
pom_version: str = stream_pom_version.read()

base_version = pom_version.split('-')[0]

logger.info('Base version: {}'.format(base_version))

logger.info('Read version from pom.xml: {}'.format(pom_version))