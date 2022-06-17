import argparse
import os
from os.path import join, isfile
from os import listdir


def makeDir(path):
    try:
        os.mkdir(path)
    except FileExistsError:
        pass


def changeDirToThisFilesDir():
    """
    Change the working directory to the directory this file is in.
    """

    os.chdir(getPathOfThisFile())


def getPathOfThisFile():
    """
    Get the absolute path of this file.
    """
    filePath = __file__
    nameOfFile = filePath.split('/')[-1]
    return filePath[:(-1 * len(nameOfFile))]


parser = argparse.ArgumentParser(description="Choose a conversion")

parser.add_argument('input', help='The INPUT path to the file. If INPUT is a folder all files in the folder are used.')
parser.add_argument('output', help='The OUTPUT path where to store the results.')

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

makeDir(outputPath + "/converted")

changeDirToThisFilesDir()

pythonFilePath = getPathOfThisFile()

if os.path.isdir(inputPath):

    isDirectory = True

    files = [f for f in listdir(inputPath) if isfile(join(inputPath, f))]
    pathOfResult = outputPath
else:
    isDirectory = False

    files = [inputPath.split('/')[-1]]
    fileLength = -1 * len(files[0])
    pathOfResult = outputPath[:fileLength]

pathOfResult += 'converted/'

# prepare docker
volumeName = 'featureIde-converter'
dockerVolumePath = '/var/lib/docker/volumes/{0}/_data/'.format(volumeName)
inputFolder = '{0}input/'.format(dockerVolumePath)
containerName = 'converter'

# determine Dockerfile
dockerfile = "-f "
if noBuild:
    dockerfile += "Dockerfile_No_Build"
else:
    dockerfile += "Dockerfile_Build"

# build docker image
os.system('docker image build . -t {0} {1}'.format(containerName, dockerfile))
# create volume
os.system('docker volume create {0}'.format(volumeName))
# create input folder
makeDir(inputFolder)
# create output folder
makeDir(pathOfResult)

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

# remove trailing whitespace
args.rstrip()

for file in files:
    fileName = file.split('.')[0]
    fileExtension = file.split('.')[-1]

    # skip not supported files
    if not (fileExtension in 'dimacs' or fileExtension in 'uvl' or fileExtension in 'xml'):
        continue

    if isDirectory:
        filePath = inputPath + file
    else:
        filePath = inputPath

    # copy file to input folder
    os.system('cp {0} {1}{2}'.format(filePath, inputFolder, file))

# run docker
os.system('docker run -e "ARGS={0}" -v {1}:/app/files {2}'.format(args, volumeName, containerName))

for file in files:
    fileName = file.split('.')[0]
    fileExtension = file.split('.')[-1]

    # skip not supported files
    if not (fileExtension in 'dimacs' or fileExtension in 'uvl' or fileExtension in 'xml'):
        continue

    if isDirectory:
        filePath = inputPath + file
    else:
        filePath = inputPath

    # make result folder on host machine
    localResultFolder = '{0}{1}/'.format(pathOfResult, fileName)
    makeDir(localResultFolder)

    # get all result files in docker volume
    resultFolder = '{0}first/{1}/'.format(dockerVolumePath, fileName)
    resultFiles = [f for f in listdir(resultFolder) if isfile(join(resultFolder, f))]
    for result in resultFiles:
        os.system('chmod o+r {0}{1}'.format(resultFolder, result))
        os.system('cp {0}{1} {2}{3}'.format(resultFolder, result, localResultFolder, result))
