package com.example.taautomotive.shared;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Defines the default folders in the media library. Each constant has a route (path segment)
 * and a loadChildren function that returns the MediaItems shown when the folder is opened.
 */
public enum DefaultFolder {

    ROOT(
            "",
            () -> {
                // Children of root are the other default folders (their folder MediaItems)
                List<MediaItem> items = new ArrayList<>();
                for (DefaultFolder folder : DefaultFolder.values()) {
                    if (folder.getRoute().isEmpty() || "root".equals(folder.getRoute())) continue;
                    items.add(folder.getFolderItem());
                }
                return items;
            }),

    MUSIC(
            "music",
            () -> {
                List<MediaItem> items = new ArrayList<>();
                items.add(
                        buildTrack(
                                "track_1",
                                "https://storage.googleapis.com/exoplayer-test-media-1/mp3/android_spot_15sec.mp3",
                                "Android Spot (15 sec)"));
                items.add(
                        buildTrack(
                                "track_2",
                                "https://storage.googleapis.com/exoplayer-test-media-1/mp3/ice_cream_15sec.mp3",
                                "Ice Cream (15 sec)"));
                return items;
            }),

    PLAYLISTS(
            "playlists",
            () -> Collections.emptyList());

    private final String route;
    private final Supplier<List<MediaItem>> loadChildren;

    DefaultFolder(String route, Supplier<List<MediaItem>> loadChildren) {
        this.route = route;
        this.loadChildren = loadChildren;
    }

    public String getRoute() {
        return route;
    }

    public Supplier<List<MediaItem>> getLoadChildren() {
        return loadChildren;
    }

    public List<MediaItem> loadChildren() {
        return loadChildren.get();
    }

    /** Returns the MediaItem that represents this folder in the library (browsable, not playable). */
    public MediaItem getFolderItem() {
        String mediaId = route.isEmpty() ? "root" : route;
        String title = getDisplayTitle();
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                        new MediaMetadata.Builder()
                                .setIsBrowsable(true)
                                .setIsPlayable(false)
                                .setTitle(title)
                                .build())
                .build();
    }

    private String getDisplayTitle() {
        if (route == null || route.isEmpty()) {
            return "TAAutomotive";
        }
        return route.substring(0, 1).toUpperCase() + route.substring(1);
    }

    private static MediaItem buildTrack(String mediaId, String uri, String title) {
        return new MediaItem.Builder()
                .setMediaId(mediaId)
                .setUri(uri)
                .setMediaMetadata(
                        new MediaMetadata.Builder()
                                .setIsBrowsable(false)
                                .setIsPlayable(true)
                                .setTitle(title)
                                .build())
                .build();
    }
}
