#!usr/bin/evn python
import subprocess
import json
import argparse
import sys
from commons import safely_exec, find_image_with_tag

description = """
 This script make requirement rolling update of test deployment. Update consists from next steps:
 1. Check, that image was build in this CI process - if not do nothing
 2. Tag image with label (mark as test, for example)
 3. Push image's tag to docker hub
 4. Execute roll update of deployment
"""

arg_parser = argparse.ArgumentParser(description=description, formatter_class=argparse.RawDescriptionHelpFormatter)
arg_parser.add_argument("image",
                        metavar="image",
                        help="full name of image, ex: 'docker.io/library/alpine'",
                        )
arg_parser.add_argument("--process-tag",
                        help="tag of process (ex: commit hash)",
                        required=True)
arg_parser.add_argument("--label",
                        help="tag which should be set as target, ex: 'test' or '1.2.3'",
                        required=True)
arg_parser.add_argument("--deployment",
                        help="updated deployment",
                        required=True
                        )

arg_parser.add_argument("--container-name",
                        help="container's name in pod",
                        required=True
                        )

arg_parser.add_argument("--dry-run",
                        help="don't push tag to remote repository",
                        action='store_true'
                        )

args = arg_parser.parse_args()
process_image = f"{args.image}:{args.process_tag}"
target_image = f"{args.image}:{args.label}"

if not find_image_with_tag(image=args.image, tag=args.process_tag):
    sys.exit(0)

print(f"pull image and add tag {args.process_tag}")
safely_exec(["podman", "pull", process_image])
safely_exec(["podman", "tag", process_image, target_image])
print("ok")

print(f"push {target_image}")
safely_exec(cmd=["podman", "push", target_image],
            dry_run=args.dry_run)
print("ok")
print("start rollupdate")
safely_exec(cmd=["kubectl", "set", "image", args.deployment, f"{args.container_name}={process_image}", "--record"],
            dry_run=args.dry_run)
print("ok")

if args.dry_run:
    print("dry-run mode, check local images")
