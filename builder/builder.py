import click
import os
import sys
import logging

TIMESTAMP_FORMAT = '%Y-%m-%dT%H:%M:%S'
SHORT_TIMESTAMP_FORMAT = '%Y-%m-%d'

logger:object = logging.getLogger('Artifact Builder ===>')
logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
class builder:
    def __init__(self, *args, **kwargs) -> None:
        print('Builder started')

    def find_executable(self, executable, path=None):
        """Tries to find 'executable' in the directories listed in 'path'.

        A string listing directories separated by 'os.pathsep'; defaults to
        os.environ['PATH'].  Returns the complete filename or None if not found.
        """
        if path is None:
            path = os.environ.get('PATH', os.defpath)

        paths = path.split(os.pathsep)
        base, ext = os.path.splitext(executable)

        if (sys.platform == 'win32' or os.name == 'os2') and (ext != '.exe'):
            executable = executable + '.exe'

        if not os.path.isfile(executable):
            for p in paths:
                f = os.path.join(p, executable)
                if os.path.isfile(f):
                    # the file exists, we have a shot at spawn working
                    return f
            return None
        else:
            return executable

    def clean_up(self, project_dir:str):
        target_dir = os.path.join(project_dir, 'target')
        build_dir = os.path.join(project_dir, 'build')
        os.system('rm -Rf {}'.format(target_dir))
        os.system('rm -Rf {}'.format(build_dir))

@click.command()
@click.option('-d', '--project-dir',
              help='password for git', required=True, type=str)
@click.option('-a', '--artifact-base-name',
              help='artifact base name without .zip. Ex. cnmToGranule', required=True, type=str)
def process(project_dir:str, artifact_base_name:str) -> None:
    '''
        this entire process is meant to run either through command line or inside a docker container
        which contains java 8, python 3 , pipe and zip utilities.
        The process will call a lot of os.system to similar command line maven and gradle build.
        As well as manipulating pom file by reading and changing the <version>
        As long as the version is ending with -SNAPSHOT, the build will be started
    '''
    logger.info('project directory:{}'.format('project_dir'))
    logger.info('artifact name: {}'.format(artifact_base_name))
    builder_o = builder()
    mvn_executable:str = builder_o.find_executable('mvn')
    gradle_executable:str = builder_o.find_executable('gradle')
    logger.info('maven executable: {}'.format(mvn_executable))
    logger.info('gradle executable:{}'.format(gradle_executable))
    os.system('pwd')
    os.chdir(project_dir)
    os.system('pwd')
    # zip and tar.gz the source before build
    logger.info('Initial cleaning up directory')
    builder_o.clean_up(project_dir)
    '''
        Read maven pom.xml <version>
    '''
    stream_pom_version = os.popen('mvn help:evaluate -Dexpression=project.version -q -DforceStdout')
    pom_version: str = stream_pom_version.read()
    logger.info('Read version from pom.xml:{}'.format(pom_version))
    if pom_version.lower().find('snapshot') == -1:
        logger.info('There is no SNAPSHOT in pom version. Stopping build ...')
        exit(0)
    else:
        release_version:str = pom_version.lower().replace('-snapshot','')
        logger.info('After removing SNAPSHOT, release version:{}'.format(release_version))

    # Maven clean and TEST
    # find if TEST has Failue case equals 0 then advance otherwise, exit the program
    os.system('{} clean'.format(mvn_executable))
    stream_mvn_test = os.popen('{} test'.format(mvn_executable))
    str_test_result: str = stream_mvn_test.read()
    logger.info('Entire MVN TEST output: {}'.format(str_test_result))
    if str_test_result.find('Failures: 0') != -1:
        logger.info('MAVEN TEST SUCCEEDED.  Continue ...')
    else:
        logger.error('MAVEN TEST FAILURE.  Existing ...')
        exit(1)
    # Build artifact using maven and gradle commands
    logger.info('Artifact is creating')
    os.system('mvn dependency:copy-dependencies')
    os.system('gradle -x test build')
    logger.info('Artifact created')
    # Check if ./releases directory existed, otherwise, create
    release_dir: str = os.path.join(project_dir, 'release')
    if not os.path.isdir(release_dir):
        os.mkdir(release_dir)
        logger.info('release directory created')
    # Move build artifact to release directory.
    logger.info('Moving built artifact to /release')
    build_zip:str = os.path.join(project_dir,'build/distributions/{}.zip'.format(artifact_base_name))
    release_zip:str = os.path.join(release_dir,artifact_base_name)
    release_zip = os.path.join(release_dir,'{}-{}.zip'.format(artifact_base_name, release_version))
    os.system('mv {} {}'.format(build_zip, release_zip))
    logger.info('finished moving built artifact to /release')

    # create version.txt
    logger.info('Opening and writing version.txt with release version: '.format(release_version))
    f = open(os.path.join(release_dir,'version.txt'), "w")
    f.write(release_version)
    f.close()
    logger.info('Version.txt created')

    # Modify the pom file and commit/push
    logger.info('Modifying pom file version to : {}'.format(release_version))
    pom_modify_version:str = 'mvn versions:set -DnewVersion={} versions:commit'.format(release_version)
    os.system(pom_modify_version)
    logger.info('Finished modifying pom file version to : {}'.format(release_version))

    # Clean up target directory
    logger.info('Final cleaning up and openup directories')
    os.system('chmod -R 777 {}'.format(release_dir))
    builder_o.clean_up(project_dir)
    os.system('cd {}'.format(os.path.join(project_dir, 'builder')))
    os.system('pwd')
    os.system('chmod -Rf 777 *')
    os.system('chmod -Rf 777 .*')

if __name__ == '__main__':
    builder_obj:object = builder()
    process()
