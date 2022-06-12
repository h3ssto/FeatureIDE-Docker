import argparse
import os

parser = argparse.ArgumentParser(description="Choose  a conversion")

parser.add_argument('input', help='The INPUT path to the file. If INPUT is a folder all files in the folder are used.')
parser.add_argument('output', help='The OUTPUT path where to store the results.')

# optional
parser.add_argument('--all', help='Select ALL conversions', action='store_true')
parser.add_argument('--dimacs', help='Select dimacs format', action='store_true')
parser.add_argument('--uvl', help='Select uvl format', action='store_true')
parser.add_argument('--featureIde', help='Select featureIde format', action='store_true')
parser.add_argument('--sxfm', help='Select sxfm format', action='store_true')
parser.add_argument('--check',
                    help='check if conversion was successful, by converting the file back to the input format.',
                    action='store_true')
parser.add_argument('--no-build',
                    help='Select that you do not want to build the converter from the source files again. Building it is the default.',
                    action='store_true')

args = vars(parser.parse_args())

inputPath = args['input']
outputPath = args['output']

doAll = args['all']
doDimacs = args['dimacs']
doUvl = args['uvl']
doFeatureIde = args['featureIde']
doSxfm = args['sxfm']
doCheck = args['check']
noBuild = args['no_build']

if outputPath[-1] != '/':
    outputPath += '/'

args = ''

if doAll:
    args = '--all '
else:
    if doDimacs:
        args += '--dimacs '
    if doUvl:
        args += '--uvl '
    if doFeatureIde:
        args += '--featureIde '
    if doSxfm:
        args += '--sxfm '

if doCheck:
    if len(args) == 0:
        print('To use check you need to specify a conversion.')
        exit(-1)
    args += '--check '

if noBuild:
    args += '--no-build '

os.system('sudo python3 ./converter-root.py {0} {1} {2}'.format(inputPath, outputPath, args))

uid = os.getuid()
gid = os.getgid()

os.system('sudo chown --recursive {0}:{1} {2}'.format(uid, gid, outputPath))
