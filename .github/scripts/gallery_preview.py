#!/usr/bin/env python3
"""Capture the scrollable Gallery content area from a running JVM Desktop window."""

from collections import Counter
from pathlib import Path
import re
import subprocess
import tempfile
import time

from PIL import Image, ImageChops

WINDOW_TITLE = "Rachel UI Gallery"
SIDEBAR_WIDTH = 257  # GallerySidebar is 256dp plus the 1dp divider at 1x CI density.
OUTPUT = Path("build/gallery-preview/gallery-preview.png")


def command(*args, check=True):
    return subprocess.run(args, check=check, capture_output=True, text=True)


def wait_for_window(timeout=900):
    start = time.monotonic()
    while time.monotonic() - start < timeout:
        result = command("xdotool", "search", "--onlyvisible", "--name", WINDOW_TITLE, check=False)
        if result.returncode == 0 and result.stdout.strip():
            return result.stdout.splitlines()[-1]
        time.sleep(2)
    raise RuntimeError("Gallery window did not appear. Inspect build/gallery-preview/gallery.log.")


def get_geometry(window):
    # xwininfo reports the X11 client rectangle, excluding Openbox decorations.
    result = command("xwininfo", "-id", window)
    matches = [re.search(rf"{field}:\s*(-?\d+)", result.stdout) for field in (
        "Absolute upper-left X", "Absolute upper-left Y", "Width", "Height"
    )]
    if any(match is None for match in matches):
        raise RuntimeError(f"Cannot read Gallery client geometry: {result.stdout}")
    return tuple(int(match.group(1)) for match in matches)


def capture(directory, number, region):
    path = directory / f"frame-{number:03d}.png"
    # Root capture + explicit client coordinates excludes the title bar and frame.
    command("scrot", "-z", str(path))
    with Image.open(path) as screenshot:
        if not (0 <= region[0] < region[2] <= screenshot.width and 0 <= region[1] < region[3] <= screenshot.height):
            raise RuntimeError(f"Gallery client rectangle {region} outside display {screenshot.size}")
        return screenshot.crop(region).convert("RGB")


def row_fingerprints(image):
    left, right = 16, image.width - 16
    return [hash(image.crop((left, y, right, y + 1)).tobytes()) for y in range(image.height)]


def scroll_delta(previous, current):
    if ImageChops.difference(previous, current).getbbox() is None:
        return 0
    previous_rows = row_fingerprints(previous)
    current_rows = row_fingerprints(current)
    frequencies = Counter(previous_rows)
    positions = {fingerprint: y for y, fingerprint in enumerate(previous_rows) if frequencies[fingerprint] == 1}
    candidates = Counter()
    for y, fingerprint in enumerate(current_rows):
        previous_y = positions.get(fingerprint)
        if previous_y is not None and previous_y > y:
            delta = previous_y - y
            if delta <= previous.height - 80:
                candidates[delta] += 1
    if not candidates:
        raise RuntimeError("Could not align scrolling frames; refusing to produce a misleading long screenshot.")
    delta, matched_rows = candidates.most_common(1)[0]
    if matched_rows < 8:
        raise RuntimeError(f"Unreliable scroll alignment: only {matched_rows} matching rows.")
    return delta


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    window = wait_for_window()
    command("xdotool", "windowactivate", "--sync", window)
    command("xdotool", "windowsize", "--sync", window, "1280", "768")
    command("xdotool", "windowmove", "--sync", window, "0", "0")
    time.sleep(3)  # Allow Compose's initial layout and page transition to settle.

    x, y, width, height = get_geometry(window)
    if width <= SIDEBAR_WIDTH:
        raise RuntimeError(f"Unexpected Gallery window size: {width}x{height}")
    region = (x + SIDEBAR_WIDTH, y, x + width, y + height)

    # Scroll events go to the right-hand Compose content, not the sidebar.
    command("xdotool", "mousemove", "--sync", str(x + width - 80), str(y + height // 2))
    with tempfile.TemporaryDirectory() as temporary:
        directory = Path(temporary)
        previous = capture(directory, 0, region)
        segments = [previous]
        for number in range(1, 41):
            command("xdotool", "click", "--repeat", "4", "--delay", "80", "5")
            time.sleep(0.85)
            current = capture(directory, number, region)
            delta = scroll_delta(previous, current)
            if delta == 0:
                break
            segments.append(current.crop((0, current.height - delta, current.width, current.height)))
            print(f"Frame {number}: scroll {delta}px", flush=True)
            previous = current
        else:
            raise RuntimeError("Gallery did not reach the bottom within 40 scroll steps.")

        total_height = sum(segment.height for segment in segments)
        result = Image.new("RGB", (segments[0].width, total_height))
        offset = 0
        for segment in segments:
            result.paste(segment, (0, offset))
            offset += segment.height
        result.save(OUTPUT, optimize=True)
        print(f"Gallery preview saved: {OUTPUT} ({result.width}x{result.height})", flush=True)


if __name__ == "__main__":
    main()
