# FeatureIDE-Docker
A Docker container providing a CLI to convert feature models using the FeatureIDE library.

## Installation
### Building

1. Download the [FeatureIDE](https://featureide.github.io/) jar [3.8.2](https://github.com/FeatureIDE/FeatureIDE/releases/download/v3.8.2/de.ovgu.featureide.lib.fm-v3.8.2.jar) and save it in the `lib` folder.
2. Download the needed jars (currently found at [GitHub](https://github.com/FeatureIDE/FeatureIDE/tree/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib)):
   the [antlr-3.4.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/antlr-3.4.jar),
   [org.sat4j.core.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/org.sat4j.core.jar),
   and [uvl-parser.jar](https://github.com/FeatureIDE/FeatureIDE/raw/3373c95f3d3f2b09557241b854044409c958681d/plugins/de.ovgu.featureide.fm.core/lib/uvl-parser.jar)
   into the `lib` folder.
3. In the `build.gradle.kts` file check that the version numbers of the libraries match.

Building the jar takes some time (1-2 minutes).

### Build yourself

You do not have to do anything.

## How to use

If you are root use the `converter-root.py` file, otherwise use the `converter.py` file.
You still need to be able to run sudo.

### Arguments

Mandatory:
1. input path: If the path points to a directory all containing files will be used. Only files ending with `.dimacs`, `.uvl`, and `.xml` will be considered.
2. output path: The program creates a converted folder in the output path, where the program stores all converted files.  

Optional:
* --all: Convert the given files to all available formats.
* --dimacs: Convert the given files to the dimacs format.
* --uvl: Convert the given files to the uvl format.
* --featureIde: Convert the given files to the featureIde format.
* --sxfm: Convert the given files to the sxfm format.
* --check: Checks if the conversion was successful, by converting the file back to the original format and comparing both hashes. (The first time the file gets converted the check often fails, because the program uses different spacing or order of the items. Therefore, the converted can still be semantically correct, when the check fails.)
* --no-build: Use this argument, when you built the code yourself. Otherwise, the code gets built everytime, taking ~1 minute. 
