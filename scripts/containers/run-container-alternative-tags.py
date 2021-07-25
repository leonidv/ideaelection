import subprocess
import json
import argparse
import sys
import textwrap

from commons import safely_exec, find_image_with_tag

description="""
Run podman image with options

It's try to find tag in central repository, if not found used tag-backup for run. 
"""

arg_parser = argparse.ArgumentParser(description=description, formatter_class=argparse.RawDescriptionHelpFormatter)
arg_parser.add_argument("image",
                        help="full name of image (without tag), ex: 'docker.io/library/alpine'")
arg_parser.add_argument("--run-options",
                        help="options of podman run (use quotes!), ex: '\"-p 8091-8094:8091-8094 -p 11210:11210\"'",
                        )
arg_parser.add_argument("--tag",
                        help="main tag of image, ex: 'test' or 'abcd'",
                        required=True
                        )
arg_parser.add_argument("--tag-backup",
                        help="alternative tag of image, used if tag is not found",
                        required=True
                        )
arg_parser.add_argument("--dry-run",
                        help="don't run image, just find it",
                        action="store_true"                        )

args=arg_parser.parse_args()
print(args)

if find_image_with_tag(args.image, args.tag):
    image = f"{args.image}:{args.tag}"
else:
    image = f"{args.image}:{args.tag_backup}"

print(f"run {image}")

run_cmd = ["podman", "run"] + args.run_options.split(" ") + [image]

safely_exec(cmd=run_cmd,
            dry_run=args.dry_run)