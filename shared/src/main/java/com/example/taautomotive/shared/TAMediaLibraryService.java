package com.example.taautomotive.shared;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
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
import java.util.List;

/**
 * MediaLibraryService that exposes a browsable media library and playback for Android Automotive.
 * The library structure is driven by {@link MediaFolderTrie} and {@link DefaultFolder}; the user
 * can select and play audio from the library via the system media UI.
 */
@UnstableApi
public final class TAMediaLibraryService extends MediaLibraryService {

    private ExoPlayer player;
    private MediaFolderTrie folderTrie;
    private MediaLibraryService.MediaLibrarySession librarySession;

    @Override
    public void onCreate() {
        super.onCreate();
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        folderTrie = new MediaFolderTrie();

        MediaLibraryService.MediaLibrarySession.Callback callback =
                new MediaLibraryService.MediaLibrarySession.Callback() {
                    @Override
                    public ListenableFuture<LibraryResult<MediaItem>> onGetLibraryRoot(
                            MediaLibraryService.MediaLibrarySession session,
                            MediaSession.ControllerInfo browser,
                            @Nullable LibraryParams params) {
                        MediaItem rootItem = folderTrie.getRoot().getMediaItem();
                        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params));
                    }

                    @Override
                    public ListenableFuture<LibraryResult<MediaItem>> onGetItem(
                            MediaLibraryService.MediaLibrarySession session,
                            MediaSession.ControllerInfo browser,
                            String mediaId) {
                        MediaItem item = resolveMediaItem(mediaId);
                        if (item != null) {
                            return Futures.immediateFuture(LibraryResult.ofItem(item, null));
                        }
                        return Futures.immediateFuture(
                                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE));
                    }

                    @Override
                    public ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> onGetChildren(
                            MediaLibraryService.MediaLibrarySession session,
                            MediaSession.ControllerInfo browser,
                            String parentId,
                            int page,
                            int pageSize,
                            @Nullable LibraryParams params) {
                        // Load children from the trie for the selected folder node
                        MediaFolderNode node = folderTrie.getNode(parentId);
                        if (node == null) {
                            return Futures.immediateFuture(
                                    LibraryResult.ofItemList(ImmutableList.of(), params));
                        }
                        List<MediaItem> all = node.loadChildren();
                        int fromIndex = page * pageSize;
                        int toIndex = Math.min(fromIndex + pageSize, all.size());
                        if (fromIndex >= all.size()) {
                            return Futures.immediateFuture(
                                    LibraryResult.ofItemList(ImmutableList.of(), params));
                        }
                        ImmutableList<MediaItem> pageItems =
                                ImmutableList.copyOf(all.subList(fromIndex, toIndex));
                        return Futures.immediateFuture(
                                LibraryResult.ofItemList(pageItems, params));
                    }
                };

        librarySession =
                new MediaLibraryService.MediaLibrarySession.Builder(
                                TAMediaLibraryService.this, player, callback)
                        .build();
    }

    @Override
    @Nullable
    public MediaLibraryService.MediaLibrarySession onGetSession(
            MediaSession.ControllerInfo controllerInfo) {
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

    /**
     * Resolves a mediaId to a MediaItem only if it exists in the trie: either as a folder node
     * (returns that node's MediaItem) or as a playable item returned by some node's loadChildren().
     */
    @Nullable
    private MediaItem resolveMediaItem(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) {
            return folderTrie.getRoot().getMediaItem();
        }
        MediaFolderNode node = folderTrie.getNode(mediaId);
        if (node != null) {
            return node.getMediaItem();
        }
        return folderTrie.findMediaItemById(mediaId);
    }
}
