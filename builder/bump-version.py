import os
import re
import sys
import click


@click.command()
@click.option('-v', '--version',
              help='Version', required=True, type=str)
@click.option('--get-base',
              help='gets the base version', is_flag=True, default=False, required=False)

# sample version
# 1.2.3-alpha.0-SNAPSHOT
# 1.2.3
# 1.2.3+xy3z
# 1.2.3-rc.1
# 1.2.3-SNAPSHOT
def bumpy(version:str, get_base):
    rc_reg = '.*rc\.\d+$'
    dev_reg = '.*alpha\.\d+.*'
    snap_reg = '.*SNAPSHOT$'

    if re.match(rc_reg, version):        
        if get_base:
            return print(version.split('-rc.')[0].strip())
        else:
            return print(int(version.split('-rc.')[1]) + 1)
    elif re.match(dev_reg, version):
        if get_base:
            return print(version.split('-alpha')[0].strip())
        else:
            return print(int(version.split('-alpha.')[1].split('-')[0]) + 1)
    elif re.match(snap_reg, version):
            return print(version.split('-SNAPSHOT')[0].strip())
    else:
        if get_base:
            return print(version.strip())
        else:
            return print(None)


if __name__ == '__main__':
    bumpy()   