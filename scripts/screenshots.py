#!/usr/bin/env python3
"""Tours every screen of the debug build on a running emulator and saves a PNG of each.

Run by .github/workflows/screenshots.yml on an Android 11 emulator set up like the
Xteink S4 (480 x 800 px, 220 dpi). Needs `adb root` (to open activities directly) and the
debug APK installed. Usage: scripts/screenshots.py <output dir>
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "club.readme.android.debug"
APP = "club.readme.android"
OUT = sys.argv[1] if len(sys.argv) > 1 else "screenshots"
count = 0


def adb(*args, check=False):
    return subprocess.run(["adb", *args], capture_output=True, check=check)


def dump():
    """The current view tree, as uiautomator sees it."""
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        raw = adb("exec-out", "cat", "/sdcard/ui.xml").stdout
        try:
            return ET.fromstring(raw)
        except ET.ParseError:
            time.sleep(1)
    return ET.Element("hierarchy")


def center(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    return (x1 + x2) // 2, (y1 + y2) // 2


def tap_node(node, wait=2.5):
    x, y = center(node)
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(wait)


def by_id(view_id, root=None):
    root = root if root is not None else dump()
    return [n for n in root.iter("node") if n.get("resource-id") == f"{PKG}:id/{view_id}"]


def tap_id(view_id, wait=2.5):
    nodes = by_id(view_id)
    if not nodes:
        print(f"  no view {view_id}")
        return False
    tap_node(nodes[0], wait)
    return True


def tap_text(text, wait=2.5):
    for n in dump().iter("node"):
        if n.get("text", "").startswith(text):
            tap_node(n, wait)
            return True
    print(f"  no text {text!r}")
    return False


def tap_in(view_id, index=0, wait=2.5):
    """Taps the [index]th clickable view inside [view_id] (a list, a grid, the choices)."""
    nodes = by_id(view_id)
    if not nodes:
        print(f"  no view {view_id}")
        return False
    inside = [n for n in nodes[0].iter("node") if n is not nodes[0] and n.get("clickable") == "true"]
    if index >= len(inside):
        print(f"  {view_id} has {len(inside)} clickable views")
        return False
    tap_node(inside[index], wait)
    return True


def texts():
    return [n.get("text", "") for n in dump().iter("node")]


def back(wait=2.5):
    adb("shell", "input", "keyevent", "4")
    time.sleep(wait)


def start(activity, wait=4):
    adb("shell", "am", "start", "-n", f"{PKG}/{APP}.{activity}")
    time.sleep(wait)


def shot(name):
    global count
    count += 1
    time.sleep(1)
    png = adb("exec-out", "screencap", "-p").stdout
    path = os.path.join(OUT, f"{count:02d}-{name}.png")
    with open(path, "wb") as f:
        f.write(png)
    print(f"{path}")


def step(title, fn):
    print(f"== {title}")
    try:
        fn()
    except Exception as e:  # one broken step must not end the tour
        print(f"  failed: {e}")


# ---- The tour -------------------------------------------------------------------------------

def home():
    adb("shell", "am", "force-stop", PKG)
    start("MainActivity", wait=25)  # first launch syncs news, guides and wallpapers
    shot("home")


def news():
    tap_id("tile_news", wait=4)
    shot("news")
    if tap_in("items", 0, wait=5):
        shot("news-article")
        tap_id("bar_menu")
        shot("reader-menu")
        back()
        back()
    tap_id("back")


def guides():
    tap_id("tile_guides", wait=4)
    shot("guides")
    if tap_in("grid", 0, wait=5):
        shot("guide-reader")
        back()
    tap_id("back")


def wallpapers():
    tap_id("tile_wallpapers", wait=10)
    shot("wallpapers")
    if tap_in("grid", 0, wait=6):
        shot("wallpaper-detail")
        tap_id("back")
    tap_id("back")


def settings():
    tap_id("tile_settings", wait=3)
    shot("settings-1")
    tap_id("next")
    shot("settings-2")
    tap_id("next")
    shot("settings-3")
    tap_id("back")


def member():
    tap_id("tile_member", wait=3)
    shot("member")
    tap_id("back")


def about():
    start("AboutActivity")
    for page in range(1, 5):
        shot(f"about-{page}")
        tap_id("next")
    back()


def games():
    tap_id("tile_games", wait=3)
    shot("games-play")
    tap_id("tab_learn")
    shot("games-learn")
    if tap_id("learn_general", wait=3):
        shot("quiz-question")
        tap_in("answers", 0)
        shot("quiz-feedback")
        back()
    if tap_id("learn_all", wait=8):
        shot("packs")
        if tap_in("list", 0, wait=3):
            shot("pack-detail")
        back()
    if tap_id("learn_missed", wait=3):
        shot("flashcards")
        back()


def board_games():
    start("SudokuActivity")
    shot("sudoku")
    start("MinesActivity")
    tap_in("board", 24, wait=3)
    shot("mines")
    start("LightsOutActivity")
    shot("lights-out")


def stacks():
    adb("shell", "run-as", PKG, "rm", "-f", "files/stacks-save.json")
    adb("shell", "rm", "-f", f"/data/data/{PKG}/files/stacks-save.json")
    adb("shell", "am", "force-stop", PKG)
    start("game.stacks.StacksActivity")
    shot("stacks-intro")
    tap_text("Margin Scribbler")
    shot("stacks-class")
    tap_id("primary", wait=3)
    shot("stacks-room")
    tap_id("primary")
    shot("stacks-hero")
    tap_in("choices", 0)
    shot("stacks-hero-item")
    tap_id("primary")
    shot("stacks-achievements")
    tap_id("back")
    seen = set()
    for _ in range(40):
        t = texts()
        if "Attack\nGUTS" in t or any(x.startswith("Attack") for x in t):
            if "fight" not in seen:
                shot("stacks-fight")
                seen.add("fight")
                tap_text("Attack")
                shot("stacks-fight-turn")
            else:
                tap_text("Attack")
            continue
        primary = [n.get("text") for n in by_id("primary")]
        if primary and primary[0] in ("Continue", "Go down", "Try again"):
            name = {"Continue": "outcome", "Go down": "floor-cleared", "Try again": "death"}[primary[0]]
            if name not in seen:
                shot(f"stacks-{name}")
                seen.add(name)
            tap_id("primary")
            continue
        if not tap_in("choices", 0):
            break
        if len(seen) >= 4:
            break


def tools():
    start("ComponentsActivity")
    shot("components")
    start("DiagnosticActivity")
    shot("diagnostic")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    adb("root")
    time.sleep(3)
    adb("wait-for-device")
    for title, fn in [
        ("Home", home), ("News", news), ("Guides", guides), ("Wallpapers", wallpapers),
        ("Settings", settings), ("Member", member), ("Games", games),
    ]:
        step(title, fn)
    for title, fn in [("About", about), ("Board games", board_games), ("The Stacks", stacks), ("Tools", tools)]:
        step(title, fn)
    print(f"{count} screenshots in {OUT}")
