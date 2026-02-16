package com.example.taautomotive.shared;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
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

    private static final String TAG = "TAMediaLibraryService";
    private static final String NOTIFICATION_CHANNEL_ID = "ta_automotive_media";
    private static final int NOTIFICATION_ID = 1;
    private ExoPlayer player;
    private MediaFolderTrie folderTrie;
    private MediaLibraryService.MediaLibrarySession librarySession;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Starting TAMediaLibraryService");
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "TAAutomotive Media",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Media playback service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        
        player = new ExoPlayer.Builder(this).build();
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        folderTrie = new MediaFolderTrie();
        Log.d(TAG, "onCreate: MediaFolderTrie initialized");

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
                        Log.d(TAG, "onGetChildren: parentId=" + parentId + ", page=" + page + ", pageSize=" + pageSize);
                        // Load children from the trie for the selected folder node
                        MediaFolderNode node = folderTrie.getNode(parentId);
                        if (node == null) {
                            Log.w(TAG, "onGetChildren: No node found for parentId=" + parentId);
                            return Futures.immediateFuture(
                                    LibraryResult.ofItemList(ImmutableList.of(), params));
                        }
                        List<MediaItem> all = node.loadChildren();
                        Log.d(TAG, "onGetChildren: Found " + all.size() + " items for parentId=" + parentId);
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

                    @Override
                    public ListenableFuture<List<MediaItem>> onAddMediaItems(MediaSession mediaSession, MediaSession.ControllerInfo controller, List<MediaItem> mediaItems) {
                        Log.d(TAG, "onAddMediaItems: Received " + mediaItems.size() + " items");
                        // Resolve each media item to ensure it exists in our library
                        ImmutableList.Builder<MediaItem> resolvedItems = ImmutableList.builder();
                        for (MediaItem item : mediaItems) {
                            Log.d(TAG, "onAddMediaItems: Processing item with mediaId=" + item.mediaId);
                            MediaItem resolved = resolveMediaItem(item.mediaId);
                            if (resolved != null) {
                                Log.d(TAG, "onAddMediaItems: Resolved item " + item.mediaId + ", URI=" + 
                                    (resolved.localConfiguration != null ? resolved.localConfiguration.uri.toString() : "null"));
                                resolvedItems.add(resolved);
                            } else {
                                Log.w(TAG, "onAddMediaItems: Failed to resolve item with mediaId=" + item.mediaId);
                            }
                        }
                        List<MediaItem> result = resolvedItems.build();
                        Log.d(TAG, "onAddMediaItems: Returning " + result.size() + " resolved items");
                        return Futures.immediateFuture(result);
                    }
                };

        librarySession =
                new MediaLibraryService.MediaLibrarySession.Builder(
                                TAMediaLibraryService.this, player, callback)
                        .build();
        
        // Start foreground service with notification
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "onCreate: MediaLibraryService started as foreground service");
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("TAAutomotive Media")
                    .setContentText("Media service is running")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build();
        } else {
            return new Notification.Builder(this)
                    .setContentTitle("TAAutomotive Media")
                    .setContentText("Media service is running")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .build();
        }
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
        Log.d(TAG, "resolveMediaItem: Resolving mediaId=" + mediaId);
        if (mediaId == null || mediaId.isEmpty()) {
            MediaItem rootItem = folderTrie.getRoot().getMediaItem();
            Log.d(TAG, "resolveMediaItem: Returning root item");
            return rootItem;
        }
        MediaFolderNode node = folderTrie.getNode(mediaId);
        if (node != null) {
            Log.d(TAG, "resolveMediaItem: Found folder node for mediaId=" + mediaId);
            return node.getMediaItem();
        }
        MediaItem foundItem = folderTrie.findMediaItemById(mediaId);
        if (foundItem != null) {
            Log.d(TAG, "resolveMediaItem: Found media item for mediaId=" + mediaId + 
                ", URI=" + (foundItem.localConfiguration != null ? foundItem.localConfiguration.uri.toString() : "null"));
        } else {
            Log.w(TAG, "resolveMediaItem: No item found for mediaId=" + mediaId);
        }
        return foundItem;
    }
}
