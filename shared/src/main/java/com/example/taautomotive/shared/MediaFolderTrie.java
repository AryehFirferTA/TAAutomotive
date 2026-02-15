package com.example.taautomotive.shared;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.Arrays;

/**
 * Trie that acts as a folder system for MediaItems. The root and its children are populated
 * from {@link DefaultFolder}. Lookup by path (e.g. "root", "root/music") returns the
 * corresponding node whose loadChildren supplies the MediaItems for that folder.
 */
public final class MediaFolderTrie {

    public static final String PATH_SEPARATOR = "/";

    private final MediaFolderNode root;

    public MediaFolderTrie() {
        this.root = new MediaFolderNode(
                DefaultFolder.ROOT.getRoute(),
                DefaultFolder.ROOT.getFolderItem(),
                DefaultFolder.ROOT.getLoadChildren());
        populateFromDefaultFolders();
    }

    /** Populates the trie with one node per {@link DefaultFolder} as direct children of root. */
    private void populateFromDefaultFolders() {
        for (DefaultFolder folder : DefaultFolder.values()) {
            if (folder.getRoute().isEmpty()) continue;
            root.putChild(
                    folder.getRoute(),
                    new MediaFolderNode(
                            folder.getRoute(),
                            folder.getFolderItem(),
                            folder.getLoadChildren()));
        }
    }

    public MediaFolderNode getRoot() {
        return root;
    }

    /**
     * Returns the node at the given path, or null if not found. Path uses "/" as separator
     * (e.g. "root", "root/music", "music"). The path "root" maps to the trie root node so
     * that when a folder MediaItem is selected, its children are loaded from the trie.
     */
    @Nullable
    public MediaFolderNode getNode(String path) {
        if (path == null || path.isEmpty()) {
            return root;
        }
        String normalized = path.startsWith(PATH_SEPARATOR) ? path.substring(1) : path;
        if (normalized.isEmpty()) {
            return root;
        }
        if ("root".equals(normalized)) {
            return root;
        }
        String[] segments = normalized.split(PATH_SEPARATOR);
        MediaFolderNode current = root;
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            current = current.getChild(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    /**
     * Returns the MediaItems for the folder at the given path, or an empty list if the path
     * does not exist.
     */
    public List<MediaItem> loadChildrenForPath(String path) {
        MediaFolderNode node = getNode(path);
        return node != null ? node.loadChildren() : Collections.emptyList();
    }

    /**
     * Returns the MediaItem with the given mediaId if it appears in any node's loadChildren()
     * (i.e. it is reachable from the trie), or null otherwise.
     */
    @Nullable
    public MediaItem findMediaItemById(String mediaId) {
        if (mediaId == null || mediaId.isEmpty()) {
            return null;
        }
        return findMediaItemByIdRecursive(root, mediaId);
    }

    @Nullable
    private static MediaItem findMediaItemByIdRecursive(MediaFolderNode node, String mediaId) {
        for (MediaItem item : node.loadChildren()) {
            if (mediaId.equals(item.mediaId)) {
                return item;
            }
        }
        for (MediaFolderNode child : node.getChildren().values()) {
            MediaItem found = findMediaItemByIdRecursive(child, mediaId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
