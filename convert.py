#!/usr/bin/env python3

# -----------------------------------------------------------------------------

import argparse
import docker
from docker.errors import ImageNotFound

import os
from os import path

import shutil

# -----------------------------------------------------------------------------
# -----------------------------------------------------------------------------

tmp_dir = "/tmp/featureide-converter-tmp"
cli = docker.from_env()

# -----------------------------------------------------------------------------


def init(force = False):

    if path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)

    try:
        os.mkdir(tmp_dir)
        os.mkdir(path.join(tmp_dir, "input"))
    except OSError as ose:
        print(ose)
        exit(2)

    try:
        image = cli.images.get("featureide-converter:latest")
    except ImageNotFound:
        print("Image \"featureide-converter:latest\" not found, building.")
        image = build()     

    return image


def build():
    image, _ = cli.images.build(path = ".", tag = "featureide-converter", rm = True)   
    return image


def start():

    parser = argparse.ArgumentParser(description="Choose a conversion")

    parser.add_argument('input', nargs = "+", help='File(s) to convert.')

    parser.add_argument('--outdir', help='Path to output files if specified.')

    parser.add_argument('--all', help='Select ALL conversions', action='store_true')
    parser.add_argument('--dimacs', help='Select dimacs format', action='store_true')
    parser.add_argument('--uvl', help='Select uvl format', action='store_true')
    parser.add_argument('--fide', help='Select featureIde format', action='store_true')
    parser.add_argument('--sxfm', help='Select sxfm format', action='store_true')

    parser.add_argument('--rebuild', help='Rebuild Docker image', action='store_true')

    return parser.parse_args()


def run(args, image):

    if args.rebuild:
        print("Rebuilding image")
        build()
        print("Done rebuilding image")

    files_in = args.input

    files_in = [path.abspath(x) for x in files_in]

    file2dir = dict()

    for file in files_in:
        file2dir[path.splitext(path.basename(file))[0]] = path.dirname(file)

    files_tmp = []

    for file in files_in:
        file_tmp = path.join(tmp_dir, "input", path.basename(file))
        files_tmp.append(file_tmp)
        shutil.copyfile(file, file_tmp)

    if args.outdir:
        dir_out = path.abspath(args.output)
        if not path.exists(dir_out):
            print(f"Path \"{dir_out}\" does not exist")
            exit(2)

    else:
        dir_out = None

    env = []

    if args.all:
        env.append("--all")
    else:
        if args.dimacs:
            env.append("--dimacs")
        if args.uvl:
            env.append("--uvl")
        if args.fide:
            env.append("--fide")
        if args.sxfm:
            env.append("--sxfm")
        
    env = [f'ARGS={" ".join(env)}']

    cli.containers.run(image, environment = env, volumes = {tmp_dir: {"bind": "/app/files", "mode": "rw"}})
    
    count = 0
    for f in os.listdir(path.join(tmp_dir, "first")):
        file = path.join(tmp_dir, "first", f)
        if path.isfile(file):
            count += 1

            if dir_out:
                file_out = path.join(dir_out, path.basename(file))
            else:
                dir_out = file2dir[path.splitext(path.basename(file))[0]]
                file_out = path.join(dir_out, path.basename(file))
            
            shutil.copyfile(file, file_out)

    if path.exists(tmp_dir):
        shutil.rmtree(tmp_dir)


if __name__ == '__main__':
    image = init()
    args = start()
    run(args, image)