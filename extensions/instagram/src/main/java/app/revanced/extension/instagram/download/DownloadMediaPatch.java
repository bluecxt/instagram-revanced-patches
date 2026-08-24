package app.revanced.extension.instagram.download;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.revanced.extension.shared.Logger;
import app.revanced.extension.shared.Utils;

/**
 * Adds a "Download" entry to the post ("...") overflow menu and downloads the media.
 */
@SuppressWarnings("unused")
public final class DownloadMediaPatch {

    private static final String T_IMAGE_INFO = "mediasize.ImageInfo";
    private static final String T_USER = "user.model.User";
    private static final String T_VIDEO_VERSION = "VideoVersion";
    private static final String T_MEDIA = "feed.media.Media";

    /**
     * Injection point. Returns {@code true} exactly once per menu build, so the patch can append the
     * synthetic "Download" row from a method that runs for every option without duplicating it.
     */
    public static boolean shouldAddDownloadRow(List<?> rows) {
        try {
            for (Object row : rows) {
                if (row == null) continue;
                for (Field f : row.getClass().getDeclaredFields()) {
                    try {
                        f.setAccessible(true);
                        Object value = f.get(row);
                        if (value instanceof Enum && "DOWNLOAD".equals(((Enum<?>) value).name())) {
                            return false;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (Throwable ex) {
            Logger.printException(() -> "shouldAddDownloadRow failure", ex);
            return false;
        }
        return true;
    }

    /**
     * Injection point. Invoked when the "Download" row in a post overflow menu is tapped.
     *
     * @param media         the {@code com.instagram.feed.media.Media} of the post.
     * @param activity      the activity hosting the menu, used to show the carousel chooser dialog.
     * @param indexA00 the overflow helper's {@code A00} int field (the live carousel position).
     * @param indexA01 the overflow helper's {@code A01} int field (fallback carousel position).
     */
    public static void onPostDownloadClick(Object media, Object activity, int indexA00, int indexA01) {
        // Media extraction can block (lazy live-tree fetches), so keep it off the main thread.
        Utils.runOnBackgroundThread(() -> {
            try {
                Object info = mediaInfoOf(media);
                if (info == null) {
                    Utils.showToastShort("Download failed: media unavailable");
                    return;
                }

                final String username = getUsername(info);
                final List<Object> carousel = getCarousel(info);

                if (carousel != null && !carousel.isEmpty()) {
                    int current = pickCarouselIndex(carousel.size(), indexA00, indexA01);
                    showCarouselDialog(activity, carousel, username, current);
                } else {
                    downloadSingle(info, username);
                }
            } catch (Throwable ex) {
                Logger.printException(() -> "onPostDownloadClick failure", ex);
                Utils.showToastShort("Download failed");
            }
        });
    }

    /** Picks the visible carousel position, preferring the live field and requiring it in range. */
    private static int pickCarouselIndex(int size, int indexA00, int indexA01) {
        if (indexA00 >= 0 && indexA00 < size) return indexA00;
        if (indexA01 >= 0 && indexA01 < size) return indexA01;
        return 0;
    }

    private static void showCarouselDialog(
            Object activity, List<Object> carousel, String username, int current) {
        if (!(activity instanceof Activity)) {
            downloadCarousel(carousel, username);
            return;
        }

        Utils.runOnMainThread(() -> {
            try {
                CharSequence[] items = {"Current media", "All media"};
                new AlertDialog.Builder((Activity) activity)
                        .setTitle("Download")
                        .setItems(items, (dialog, which) -> {
                            if (which == 0) {
                                downloadSingle(carousel.get(current), username);
                            } else {
                                downloadCarousel(carousel, username);
                            }
                        })
                        .show();
            } catch (Throwable ex) {
                Logger.printException(() -> "showCarouselDialog failure", ex);
                downloadCarousel(carousel, username);
            }
        });
    }

    private static void downloadSingle(Object mediaOrInfo, String username) {
        Utils.showToastShort("Downloading…");
        Utils.runOnBackgroundThread(() -> {
            Item item = resolveItem(mediaOrInfo);
            if (item == null) {
                Utils.showToastShort("Download failed: no media URL");
                return;
            }
            download(item, username, 0);
        });
    }

    private static void downloadCarousel(List<Object> carousel, String username) {
        Utils.showToastShort("Downloading " + carousel.size() + " items…");
        Utils.runOnBackgroundThread(() -> {
            int index = 1;
            int ok = 0;
            for (Object child : carousel) {
                Item item = resolveItem(child);
                if (item != null && download(item, username, index)) ok++;
                index++;
            }
            final int saved = ok;
            Utils.showToastShort("Saved " + saved + " of " + carousel.size());
        });
    }

    // region Story overflow menu

    private static final String T_REEL_ITEM = "reels.ReelItem";

    /**
     * Injection point. Appends a "Download" entry to a story ("...") options dialog before it is
     * shown, returning a new array with the extra label.
     */
    public static CharSequence[] appendStoryDownloadLabel(CharSequence[] labels) {
        try {
            if (labels == null) return new CharSequence[]{"Download"};
            CharSequence[] out = Arrays.copyOf(labels, labels.length + 1);
            out[labels.length] = "Download";
            return out;
        } catch (Throwable ex) {
            Logger.printException(() -> "appendStoryDownloadLabel failure", ex);
            return labels;
        }
    }

    /** Injection point: story menu dispatch, when the "Download" label is selected. */
    public static void onStoryDownloadClick(Object helper) {
        Utils.runOnBackgroundThread(() -> {
            try {
                Object reelItem = firstFieldOfType(helper, T_REEL_ITEM);
                Object media = (reelItem == null) ? null : mediaFromReelItem(reelItem);
                if (media == null) {
                    Utils.showToastShort("Download failed: media unavailable");
                    return;
                }
                Object info = mediaInfoOf(media);
                String username = getUsername(info);
                downloadSingle(media, username);
            } catch (Throwable ex) {
                Logger.printException(() -> "onStoryDownloadClick failure", ex);
                Utils.showToastShort("Download failed");
            }
        });
    }

    /** The {@code Media} of a story item, from a field or accessor. */
    private static Object mediaFromReelItem(Object reelItem) {
        Object media = firstFieldOfType(reelItem, T_MEDIA);
        if (media != null) return media;
        for (Method m : reelItem.getClass().getMethods()) {
            if (m.getParameterTypes().length != 0) continue;
            if (!m.getReturnType().getName().contains(T_MEDIA)) continue;
            try {
                Object value = invoke(reelItem, m);
                if (value != null) return value;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /** The first non-null public field of {@code obj} whose value's class name contains the marker. */
    private static Object firstFieldOfType(Object obj, String marker) {
        for (Field f : obj.getClass().getFields()) {
            try {
                Object value = f.get(obj);
                if (value != null && value.getClass().getName().contains(marker)) return value;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    // endregion

    // region Reels (clips) overflow menu

    /**
     * Injection point. Returns a click listener (held by an injected "Download" row in the reels
     * "..." menu) that downloads the reel's media.
     */
    public static View.OnClickListener createReelDownloadListener(Object media) {
        return view -> onPostDownloadClick(media, null, 0, 0);
    }

    // endregion

    // region Media extraction (type-driven)

    /** A single downloadable media (image or video) with its source URL. */
    private static final class Item {
        final String url;
        final boolean isVideo;

        Item(String url, boolean isVideo) {
            this.url = url;
            this.isVideo = isVideo;
        }
    }

    /**
     * Resolves the media-data object (which exposes the typed accessors) reachable from a feed
     * {@code Media}. Falls back to the given object if it already is the media-data object.
     */
    private static Object mediaInfoOf(Object obj) {
        if (obj == null) return null;
        if (methodReturning(obj.getClass(), T_IMAGE_INFO) != null) return obj;
        for (Field f : obj.getClass().getFields()) {
            try {
                Object value = f.get(obj);
                if (value != null && methodReturning(value.getClass(), T_IMAGE_INFO) != null) {
                    return value;
                }
            } catch (Throwable ignored) {
            }
        }
        return obj;
    }

    private static Item resolveItem(Object mediaOrInfo) {
        Object info = mediaInfoOf(mediaOrInfo);
        if (info == null) return null;
        try {
            String videoUrl = firstVideoUrl(info);
            if (videoUrl != null) return new Item(videoUrl, true);

            String imageUrl = bestImageUrl(info);
            if (imageUrl != null) return new Item(imageUrl, false);

            // Fallback: any cached image URL reachable from the media data object or the media
            // itself (e.g. the pre-loaded display image), so a download still succeeds.
            String fallback = anyImageUrl(info);
            if (fallback == null) fallback = anyImageUrl(mediaOrInfo);
            if (fallback != null) return new Item(fallback, false);

            Logger.printException(() -> "resolveItem: no url on " + className(info)
                    + " (imageInfoMethods=" + countMethodsReturning(info.getClass(), T_IMAGE_INFO)
                    + ", listMethods=" + noArgListMethods(info.getClass()).size() + ")");
        } catch (Throwable ex) {
            Logger.printException(() -> "resolveItem failure on " + className(info), ex);
        }
        return null;
    }

    /** Best-effort: the widest {@code getUrl()}-bearing object among the fields of {@code obj}. */
    private static String anyImageUrl(Object obj) {
        if (obj == null) return null;
        String best = null;
        int bestWidth = -1;
        for (Field f : obj.getClass().getFields()) {
            Object value;
            try {
                value = f.get(obj);
            } catch (Throwable ignored) {
                continue;
            }
            String url = urlOf(value);
            if (url == null) continue;
            int width = intWidthOf(value);
            if (best == null || width > bestWidth) {
                best = url;
                bestWidth = width;
            }
        }
        return best;
    }

    /** Returns the http(s) URL of an object exposing {@code getUrl()}, or null. */
    private static String urlOf(Object value) {
        if (value == null) return null;
        try {
            Object url = invokeNamed(value, "getUrl");
            if (url instanceof String && ((String) url).startsWith("http")) return (String) url;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static int intWidthOf(Object value) {
        try {
            return asInt(invokeNamed(value, "getWidth"));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** The video URL of the media, or null when it is an image. */
    private static String firstVideoUrl(Object info) {
        for (Method m : noArgListMethods(info.getClass())) {
            try {
                List<?> list = asList(invoke(info, m));
                Object element = firstNonNull(list);
                if (element == null) continue;
                if (!typeContains(element.getClass(), T_VIDEO_VERSION)) continue;
                String url = asString(invokeNamed(element, "getUrl"));
                if (url != null) return url;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * The highest-resolution image URL of the media. Any of the media's {@code ImageInfo}-returning
     * accessors may hold {@code image_versions2}, and some return null, so all are tried.
     */
    private static String bestImageUrl(Object info) {
        String bestUrl = null;
        int bestWidth = -1;
        for (Method m : info.getClass().getMethods()) {
            if (m.getParameterTypes().length != 0) continue;
            if (!m.getReturnType().getName().contains(T_IMAGE_INFO)) continue;

            Object imageInfo;
            try {
                imageInfo = invoke(info, m);
            } catch (Throwable ignored) {
                continue;
            }
            if (imageInfo == null) continue;

            for (Method lm : noArgListMethods(imageInfo.getClass())) {
                List<?> candidates;
                try {
                    candidates = asList(invoke(imageInfo, lm));
                } catch (Throwable ignored) {
                    continue;
                }
                if (candidates == null) continue;
                for (Object candidate : candidates) {
                    if (candidate == null) continue;
                    try {
                        String url = asString(invokeNamed(candidate, "getUrl"));
                        if (url == null) continue;
                        int width = asInt(invokeNamed(candidate, "getWidth"));
                        if (bestUrl == null || width > bestWidth) {
                            bestUrl = url;
                            bestWidth = width;
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return bestUrl;
    }

    private static int countMethodsReturning(Class<?> clazz, String marker) {
        int n = 0;
        for (Method m : clazz.getMethods()) {
            if (m.getParameterTypes().length == 0 && m.getReturnType().getName().contains(marker)) n++;
        }
        return n;
    }

    /** The carousel children (each a feed {@code Media}), or null when the post is not a carousel. */
    private static List<Object> getCarousel(Object info) {
        for (Method m : noArgListMethods(info.getClass())) {
            try {
                List<?> list = asList(invoke(info, m));
                Object element = firstNonNull(list);
                if (element == null) continue;
                if (typeContains(element.getClass(), T_MEDIA)) {
                    return new ArrayList<>(list);
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static final String T_LIVE_TREE = "livetree.LiveTreeJNI";

    /**
     * Pando addresses fields by the Java {@link String#hashCode()} of their (readable) name, so the
     * hash for a field is computed from the name instead of hardcoding a magic number.
     */
    private static int pandoFieldHash(String fieldName) {
        return fieldName.hashCode();
    }

    private static String getUsername(Object info) {
        try {
            // A media exposes several User accessors (owner, coauthor, sponsor, ...); most are null,
            // so all are tried until one yields a username.
            for (Method m : info.getClass().getMethods()) {
                if (m.getParameterTypes().length != 0) continue;
                if (!m.getReturnType().getName().contains(T_USER)) continue;

                Object user;
                try {
                    user = invoke(info, m);
                } catch (Throwable ignored) {
                    continue;
                }
                if (user == null) continue;

                String name = usernameFromUser(user);
                if (name != null && !name.isEmpty()) return sanitize(name);
            }
        } catch (Throwable ex) {
            Logger.printException(() -> "getUsername failure", ex);
        }
        return "instagram";
    }

    private static String usernameFromUser(Object user) {
        // 1. Robust: read the "username" field by its build-stable Pando hash off the user's live tree.
        Object tree = findLiveTree(user, 0);
        if (tree != null) {
            int usernameHash = pandoFieldHash("username");
            for (String accessor : new String[]{"getOptionalStringValueByHashCode", "getOptionalStringValueNative"}) {
                try {
                    Object name = invokeIntArg(tree, accessor, usernameHash);
                    if (name instanceof String && isUsernameLike((String) name)) return (String) name;
                } catch (Throwable ignored) {
                }
            }
        }

        // 2. Fallback: scan the user and its fields for a username-like string.
        String direct = firstUsernameLike(user);
        if (direct != null) return direct;
        for (Field f : user.getClass().getFields()) {
            try {
                Object dict = f.get(user);
                if (dict != null) {
                    String name = firstUsernameLike(dict);
                    if (name != null) return name;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /** Finds a Pando {@code LiveTreeJNI} reachable from the object (itself or within its fields). */
    private static Object findLiveTree(Object obj, int depth) {
        if (obj == null || depth > 2) return null;
        if (obj.getClass().getName().contains(T_LIVE_TREE)) return obj;
        for (Field f : obj.getClass().getFields()) {
            try {
                Object value = f.get(obj);
                if (value == null) continue;
                if (value.getClass().getName().contains(T_LIVE_TREE)) return value;
                if (value.getClass().getName().startsWith("com.instagram")) {
                    Object nested = findLiveTree(value, depth + 1);
                    if (nested != null) return nested;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeIntArg(Object target, String name, int arg) throws Exception {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name)) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && (params[0] == int.class || params[0] == Integer.class)) {
                m.setAccessible(true);
                return m.invoke(target, arg);
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static String firstUsernameLike(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            if (m.getParameterTypes().length != 0) continue;
            if (m.getReturnType() != String.class) continue;
            try {
                Object value = invoke(obj, m);
                if (value instanceof String && isUsernameLike((String) value)) {
                    return (String) value;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static boolean isUsernameLike(String s) {
        // Usernames: 1-30 chars of [a-z0-9._], at least one letter (excludes numeric ids), no spaces.
        return s != null
                && s.length() >= 1 && s.length() <= 30
                && s.matches("[A-Za-z0-9._]+")
                && s.matches(".*[A-Za-z].*");
    }

    // endregion

    // region Reflection helpers

    private static Method methodReturning(Class<?> clazz, String returnTypeMarker) {
        for (Method m : clazz.getMethods()) {
            if (m.getParameterTypes().length == 0
                    && m.getReturnType().getName().contains(returnTypeMarker)) {
                return m;
            }
        }
        return null;
    }

    private static List<Method> noArgListMethods(Class<?> clazz) {
        List<Method> result = new ArrayList<>();
        for (Method m : clazz.getMethods()) {
            if (m.getParameterTypes().length == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                result.add(m);
            }
        }
        return result;
    }

    private static Object invoke(Object target, Method method) throws Exception {
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Object invokeNamed(Object target, String name) throws Exception {
        Method method = null;
        for (Method m : target.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == 0) {
                method = m;
                break;
            }
        }
        if (method == null) throw new NoSuchMethodException(name);
        return invoke(target, method);
    }

    /** True if the class, any superclass, or any (transitive) interface name contains the marker. */
    private static boolean typeContains(Class<?> clazz, String marker) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            if (c.getName().contains(marker)) return true;
            for (Class<?> i : allInterfaces(c)) {
                if (i.getName().contains(marker)) return true;
            }
        }
        return false;
    }

    private static Set<Class<?>> allInterfaces(Class<?> clazz) {
        Set<Class<?>> seen = new LinkedHashSet<>();
        Deque<Class<?>> queue = new ArrayDeque<>(Arrays.asList(clazz.getInterfaces()));
        while (!queue.isEmpty()) {
            Class<?> i = queue.poll();
            if (seen.add(i)) queue.addAll(Arrays.asList(i.getInterfaces()));
        }
        return seen;
    }

    private static List<?> asList(Object o) {
        return (o instanceof List) ? (List<?>) o : null;
    }

    private static Object firstNonNull(List<?> list) {
        if (list == null) return null;
        for (Object o : list) if (o != null) return o;
        return null;
    }

    private static String asString(Object o) {
        return (o instanceof String) ? (String) o : null;
    }

    private static int asInt(Object o) {
        return (o instanceof Integer) ? (Integer) o : 0;
    }

    private static String className(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    // endregion

    // region Download / storage

    private static boolean download(Item media, String username, int index) {
        HttpURLConnection connection = null;
        try {
            String extension = media.isVideo ? "mp4" : "jpg";
            String name = username + "_" + System.currentTimeMillis()
                    + (index > 0 ? "_" + index : "") + "." + extension;

            URL url = new URL(media.url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);
            connection.connect();

            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                Logger.printException(() -> "Download HTTP " + code + " for " + media.url);
                return false;
            }

            try (InputStream input = connection.getInputStream()) {
                return saveToGallery(input, name, username, media.isVideo);
            }
        } catch (Throwable ex) {
            Logger.printException(() -> "download failure for " + media.url, ex);
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static final String SUBDIR = "Instagram/downloads";

    private static boolean saveToGallery(InputStream input, String name, String username, boolean isVideo)
            throws Exception {
        Context context = Utils.getContext();
        String relativeBase = (isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES)
                + "/" + SUBDIR + "/" + username;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            Uri collection = isVideo
                    ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, isVideo ? "video/mp4" : "image/jpeg");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeBase);
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri item = resolver.insert(collection, values);
            if (item == null) return false;
            try (OutputStream output = resolver.openOutputStream(item)) {
                if (output == null) return false;
                copy(input, output);
            }
            values.clear();
            values.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(item, values, null, null);
            return true;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    isVideo ? Environment.DIRECTORY_MOVIES : Environment.DIRECTORY_PICTURES),
                    SUBDIR + "/" + username);
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File file = new File(dir, name);
            try (OutputStream output = new FileOutputStream(file)) {
                copy(input, output);
            }
            MediaStore.Images.Media.insertImage(context.getContentResolver(),
                    file.getAbsolutePath(), name, null);
            return true;
        }
    }

    private static void copy(InputStream input, OutputStream output) throws Exception {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }

    // endregion
}
