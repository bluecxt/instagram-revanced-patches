# 📸 Instagram ReVanced Patches

Dedicated, lightweight, and up-to-date [ReVanced](https://revanced.app) patches specifically tailored for **Instagram**.

Tested & compatible with Instagram **v442.x** and **v443.x** (Android 9.0+, ARM64 & x86_64).

---

## ✨ Features & Included Patches

- 🚫 **Hide Ads (`Hide ads`)** : Complete ad-blocker eliminating sponsored items from the **Main Feed**, **Reels**, and **Stories** without startup or runtime crashes.
- 💾 **Download Media (`Download media`)** : Adds a dedicated "Download" button to the post and Reels "..." overflow menu to save media directly.
- 🔒 **Disable Swipe Navigation (`Disable swipe navigation`)** : Prevents accidental horizontal swiping between feed, camera, and DMs.
- 🔍 **Hide Explore Feed (`Hide explore feed`)** : Hides algorithmic explore grid/reels in the search tab.
- 🧭 **Hide Navigation Buttons (`Hide navigation buttons`)** : Allows customizing and hiding navigation bar tabs (e.g. Reels or Create buttons).
- 🧹 **Hide Suggested Content (`Hide suggested content`)** : Removes suggested posts, suggested reels, and suggested threads from your home feed.
- 🚫 **Disable Analytics (`Disable analytics`)** : Blocks periodic tracking and telemetry requests.
- ⏳ **Remove Build Expired Popup (`Remove build expired popup`)** : Disables the lockout dialog when running older or alpha builds.
- 🔗 **Sanitize Sharing Links (`Sanitize sharing links`)** : Strips tracking query parameters (`igsh`, etc.) from shared URLs.
- 👁️ **Anonymous Story Viewing (`Anonymous story viewing`)** : View stories without notifying the poster or appearing in viewer lists.
- ⏸️ **Disable Story Auto-Flipping (`Disable story auto flipping`)** : Keeps stories on screen until you manually advance.
- 🎨 **Location Sticker Redesign (`Enable location sticker redesign`)** : Unlocks full redesigned style set for location stickers.
- 🛠️ **Enable Developer Menu (`Enable developer menu`)** : Exposes internal developer options in settings.

---

## 🚀 How to Use

### Option 1: Using ReVanced CLI (Command Line)

1. Download the latest `.rvp` bundle from [Releases](https://github.com/bluecxt/instagram-revanced-patches/releases).
2. Download [ReVanced CLI](https://github.com/ReVanced/revanced-cli/releases).
3. Obtain the recommended Instagram APK (v442 or v443).
4. Run the patcher:

```bash
java -jar revanced-cli.jar patch \
  -p patches-*.rvp \
  -o instagram-patched.apk \
  --exclusive -e "Hide ads" -e "Download media" -e "Follow back indicator" \
  instagram.apk
```

5. Sign and install `instagram-patched.apk` on your device!

---

### Option 2: Using ReVanced Manager

1. Open **ReVanced Manager** on your Android device.
2. Go to **Settings > Sources**.
3. Under **Patches organization / source**, specify:
   * **Source:** `bluecxt`
   * **Repository:** `instagram-revanced-patches`
4. Select Instagram from your storage and apply the patches!

---

## 🛠️ Building from Source

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (build-tools)

### Build the `.rvp` Patch Bundle
```bash
./gradlew :patches:jar
```
The compiled bundle will be output to:
```
patches/build/libs/patches-<version>.rvp
```

---

## 📜 License
GPL-3.0 License. Based on the open-source [ReVanced Patches](https://gitlab.com/ReVanced/revanced-patches) project.
