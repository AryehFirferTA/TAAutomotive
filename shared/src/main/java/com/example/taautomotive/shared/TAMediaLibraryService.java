package com.example.taautomotive.shared;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.LibraryResult;
import androidx.media3.session.MediaLibraryService;
import androidx.media3.session.MediaSession;
import androidx.media3.session.SessionError;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;

/**
 * MediaLibraryService that exposes a browsable media library and playback for Android Automotive.
 * The user can select and play audio from the library via the system media UI.
 */
@UnstableApi
public final class TAMediaLibraryService extends MediaLibraryService {

    private static final String ROOT_ID = "root";
    private static final String MUSIC_ID = "music";

    private ExoPlayer player;
    private MediaLibraryService.MediaLibrarySession librarySession;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);

        MediaLibraryService.MediaLibrarySession.Callback callback = new MediaLibraryService.MediaLibrarySession.Callback() {
            @Override
            public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                    MediaLibraryService.MediaLibrarySession session,
                    MediaSession.ControllerInfo browser,
                    @Nullable LibraryParams params) {
                MediaItem rootItem =
                        new MediaItem.Builder()
                                .setMediaId(ROOT_ID)
                                .setMediaMetadata(
                                        new MediaMetadata.Builder()
                                                .setIsBrowsable(true)
                                                .setIsPlayable(false)
                                                .setTitle("TAAutomotive")
                                                .build())
                                .build();
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
            }

            @Override
            public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                    MediaLibraryService.MediaLibrarySession session,
                    MediaSession.ControllerInfo browser,
                    String mediaId) {
                if (ROOT_ID.equals(mediaId)) {
                    MediaItem item =
                            new MediaItem.Builder()
                                    .setMediaId(ROOT_ID)
                                    .setMediaMetadata(
                                            new MediaMetadata.Builder()
                                                    .setIsBrowsable(true)
                                                    .setIsPlayable(false)
                                                    .setTitle("TAAutomotive")
                                                    .build())
                                    .build();
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null));
                }
                if (MUSIC_ID.equals(mediaId)) {
                    MediaItem item =
                            new MediaItem.Builder()
                                    .setMediaId(MUSIC_ID)
                                    .setMediaMetadata(
                                            new MediaMetadata.Builder()
                                                    .setIsBrowsable(true)
                                                    .setIsPlayable(false)
                                                    .setTitle("Music")
                                                    .build())
                                    .build();
                    return Futures.immediateFuture(LibraryResult.ofItem(item, null));
                }
                // Playable track
                MediaItem track = buildTrackItem(mediaId);
                if (track != null) {
                    return Futures.immediateFuture(LibraryResult.ofItem(track, null));
                }
                return Futures.immediateFuture(LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
            }

            @Override
            public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                    MediaLibraryService.MediaLibrarySession session,
                    MediaSession.ControllerInfo browser,
                    String parentId,
                    int page,
                    int pageSize,
                    @Nullable LibraryParams params) {
                if (ROOT_ID.equals(parentId)) {
                    MediaItem musicFolder =
                            new MediaItem.Builder()
                                    .setMediaId(MUSIC_ID)
                                    .setMediaMetadata(
                                            new MediaMetadata.Builder()
                                                    .setIsBrowsable(true)
                                                    .setIsPlayable(false)
                                                    .setTitle("Music")
                                                    .build())
                                    .build();
                    return Futures.immediateFuture(
                            LibraryResult.ofItemList(ImmutableList.of(musicFolder), params));
                }
                if (MUSIC_ID.equals(parentId)) {
                    List<MediaItem> tracks = getPlayableTracks();
                    int fromIndex = page * pageSize;
                    int toIndex = Math.min(fromIndex + pageSize, tracks.size());
                    if (fromIndex >= tracks.size()) {
                        return Futures.immediateFuture(
                                LibraryResult.ofItemList(ImmutableList.of(), params));
                    }
                    ImmutableList<MediaItem> pageItems =
                            ImmutableList.copyOf(tracks.subList(fromIndex, toIndex));
                    return Futures.immediateFuture(LibraryResult.ofItemList(pageItems, params));
                }
                return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params));
            }
        };

        librarySession =
                new MediaLibraryService.MediaLibrarySession.Builder(TAMediaLibraryService.this, player, callback)
                        .build();
    }

    @Override
    @Nullable
    public MediaLibraryService.MediaLibrarySession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return librarySession;
    }

    @Override
    public void onDestroy() {
        if (librarySession != null) {
            librarySession.release();
            librarySession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private static List<MediaItem> getPlayableTracks() {
        List<MediaItem> items = new ArrayList<>();
        items.add(
                buildTrackItem(
                        "track_1",
                        "https://storage.googleapis.com/exoplayer-test-media-1/mp3/android_spot_15sec.mp3",
                        "Android Spot (15 sec)"));
        items.add(
                buildTrackItem(
                        "track_2",
                        "https://storage.googleapis.com/exoplayer-test-media-1/mp3/ice_cream_15sec.mp3",
                        "Ice Cream (15 sec)"));
        return items;
    }

    private static MediaItem buildTrackItem(String mediaId, String uri, String title) {
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

    @Nullable
    private static MediaItem buildTrackItem(String mediaId) {
        if ("track_1".equals(mediaId)) {
            return buildTrackItem(
                    mediaId,
                    "https://storage.googleapis.com/exoplayer-test-media-1/mp3/android_spot_15sec.mp3",
                    "Android Spot (15 sec)");
        }
        if ("track_2".equals(mediaId)) {
            return buildTrackItem(
                    mediaId,
                    "https://storage.googleapis.com/exoplayer-test-media-1/mp3/ice_cream_15sec.mp3",
                    "Ice Cream (15 sec)");
        }
        return null;
    }
}
