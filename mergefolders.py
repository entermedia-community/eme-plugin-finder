#!/usr/bin/env python3

import shutil
from pathlib import Path

# Change these
SRC = Path("plugins/finder/html/find/authentication")
DST = Path("plugins/community/html/default/authentication")


def move_merge(src: Path, dst: Path):
    dst.mkdir(parents=True, exist_ok=True)

    for item in src.iterdir():
        dst_item = dst / item.name

        if item.is_dir():
            # Merge directory
            move_merge(item, dst_item)

            # Remove source dir if empty
            try:
                item.rmdir()
                print(f"Removed empty directory: {item}")
            except OSError:
                pass

        else:
            if dst_item.exists():
                print(f"Skipping existing file: {dst_item}")
                continue

            print(f"Moving: {item} -> {dst_item}")
            shutil.move(str(item), str(dst_item))


if __name__ == "__main__":
    if not SRC.exists():
        raise FileNotFoundError(f"Source does not exist: {SRC}")

    move_merge(SRC, DST)

    # Try removing the root source directory if empty
    try:
        SRC.rmdir()
        print(f"Removed empty source directory: {SRC}")
    except OSError:
        print("Source directory still contains skipped files.")