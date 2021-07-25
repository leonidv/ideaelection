import subprocess
import sys
import textwrap
import json


def safely_exec(cmd, dry_run=False):
    if dry_run:
        print(f"dry run: {' '.join(cmd)}")
        return ""

    cp = subprocess.run(cmd, capture_output=True)
    if cp.returncode != 0:
        print()
        print(cp.args)
        print(f"exit code = {cp.returncode}")
        print("stdout:")
        print(textwrap.dedent(cp.stdout.decode()))
        print("stderr")
        print(textwrap.dedent(cp.stderr.decode()))
        sys.exit(1)
    else:
        return cp.stdout


def find_image_with_tag(image, tag):
    print(f"search {image}:{tag} in central repository")
    list_tags_output = safely_exec(["skopeo", "list-tags", f"docker://{image}"])
    tags = json.loads(list_tags_output)["Tags"]
    if tag not in tags:
        print(f"image is not found)")
        return False
    else:
        print("ok")
        return True
