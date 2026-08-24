#!/usr/bin/env python3
import json
import os
import shutil
import sys
from datetime import datetime, timezone

def main():
    tag = os.environ.get("RELEASE_TAG", "v1.0.1")
    repo = os.environ.get("GITHUB_REPOSITORY", "bluecxt/instagram-revanced-patches")
    user = repo.split("/")[0] if "/" in repo else "bluecxt"
    reponame = repo.split("/")[1] if "/" in repo else "instagram-revanced-patches"
    now_iso = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S")

    out_dir = sys.argv[1] if len(sys.argv) > 1 else "public"
    os.makedirs(out_dir, exist_ok=True)

    rvp_filename = "patches-6.1.0.rvp"
    # Find actual rvp file in patches/build/libs if exists
    libs_dir = os.path.join("patches", "build", "libs")
    if os.path.isdir(libs_dir):
        for f in os.listdir(libs_dir):
            if f.endswith(".rvp"):
                rvp_filename = f
                src_path = os.path.join(libs_dir, f)
                dst_path = os.path.join(out_dir, f)
                shutil.copy2(src_path, dst_path)
                print(f"Copied {src_path} to {dst_path} ({os.path.getsize(dst_path)} bytes)")
                break

    download_url = f"https://{user}.github.io/{reponame}/{rvp_filename}"

    descriptions = {
        "Hide ads": "Complete ad-blocker eliminating sponsored items from the Main Feed, Reels, and Stories without crashes.",
        "Download media": "Adds a Download option to the post and Reels menu to save photos and videos.",
        "Disable swipe navigation": "Disables swiping between the main navigation tabs and swiping to the camera.",
        "Disable analytics": "Disables analytics that are sent periodically.",
        "Remove build expired popup": "Removes the popup that appears after a while, when the app version ages.",
        "Sanitize sharing links": "Removes the tracking query parameters from shared links.",
        "Change link sharing domain": "Replaces the domain name of shared links.",
        "Enable developer menu": "Enables the internal developer options in settings.",
        "Open links externally": "Changes links to always open in your external browser, instead of the in-app browser.",
        "Hide explore feed": "Hides posts and reels from the explore/search page.",
        "Hide navigation buttons": "Hides navigation bar buttons, such as the Reels and Create button.",
        "Hide suggested content": "Hides suggested stories, reels, threads and survey from feed.",
        "Hide highlights tray": "Hides the highlights tray in profile section.",
        "Hide Stories from Home": "Hides Stories from the main page, by removing the buttons.",
        "Limit feed to followed profiles": "Filters the home feed to display only content from profiles you follow.",
        "Disable Reels scrolling": "Disables the endless scrolling behavior in Instagram Reels.",
        "Disable Reels auto-scroll": "Removes the auto-scroll toggle and prevents Reels from scrolling automatically.",
        "Disable story auto flipping": "Disable stories automatically flipping/skipping after some seconds.",
        "Enable location sticker redesign": "Unlocks the redesigned location sticker with additional style options.",
        "Anonymous story viewing": "View stories without sending any information to the server.",
        "Disable signature check": "Disables the signature check that can cause the app to crash on startup.",
        "Remove screenshot restriction": "Removes the restriction of taking screenshots in disappearing messages and media that normally wouldn't allow it.",
        "Prevent screenshot detection": "Removes the registration of screen capture callbacks, preventing Instagram from detecting screenshots or notifying the sender."
    }

    patches = []
    for name, desc in descriptions.items():
        use = name in ["Hide ads", "Disable analytics", "Remove build expired popup", "Sanitize sharing links"]
        patches.append({
            "name": name,
            "description": desc,
            "version": tag.lstrip("v"),
            "use": use,
            "compatiblePackages": [
                {
                    "name": "com.instagram.android",
                    "versions": ["443.0.0.48.82"]
                }
            ]
        })

    manifest = {
        "name": "Instagram ReVanced Patches - bluecxt",
        "description": "Dedicated ReVanced patches for Instagram v443.0.0.48.82",
        "version": tag,
        "created_at": now_iso,
        "download_url": download_url,
        "patches": patches
    }

    for filename in ["patches.json", "index.json"]:
        with open(os.path.join(out_dir, filename), "w") as fp:
            json.dump(manifest, fp, indent=2)

    with open(os.path.join(out_dir, "patches_array.json"), "w") as fp:
        json.dump(patches, fp, indent=2)

    # Simple HTML landing page
    with open(os.path.join(out_dir, "index.html"), "w") as fp:
        fp.write(f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Instagram ReVanced Patches - bluecxt</title>
    <style>
        body {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 40px auto; padding: 0 20px; line-height: 1.6; color: #333; }}
        code {{ background: #f4f4f4; padding: 4px 8px; border-radius: 4px; font-size: 0.95em; word-break: break-all; }}
        .box {{ background: #eef6ff; border-left: 4px solid #0366d6; padding: 15px; margin: 20px 0; border-radius: 4px; }}
        a {{ color: #0366d6; text-decoration: none; }}
        a:hover {{ text-decoration: underline; }}
    </style>
</head>
<body>
    <h1>📸 Instagram ReVanced Patches - bluecxt</h1>
    <p>Official ReVanced Manager source endpoint for Instagram patches.</p>
    <div class="box">
        <strong>ReVanced Manager Source URL:</strong><br>
        <code>https://{user}.github.io/{reponame}/patches.json</code>
    </div>
    <p>Latest Version: <b>{tag}</b></p>
    <p><a href="{download_url}">Download latest .rvp bundle</a></p>
    <p><a href="https://github.com/{repo}">View GitHub Repository</a></p>
</body>
</html>
""")

    print(f"Generated manifest, pages, and bundled rvp in {out_dir}/ for release {tag}")

if __name__ == "__main__":
    main()
