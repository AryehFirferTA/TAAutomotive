package com.example.taautomotive.shared;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A node in the media folder trie. Acts as a folder in the MediaItem tree: each node has a
 * path segment, the MediaItem it represents, a function that returns the list of MediaItems
 * when this folder is opened (loadChildren), and child nodes for sub-folders.
 */
public final class MediaFolderNode {

    private final String segment;
    private final MediaItem mediaItem;
    private final Supplier<List<MediaItem>> loadChildren;
    private final Map<String, MediaFolderNode> children;

    public MediaFolderNode(String segment, MediaItem mediaItem, Supplier<List<MediaItem>> loadChildren) {
        this.segment = segment;
        this.mediaItem = mediaItem;
        this.loadChildren = loadChildren != null ? loadChildren : () -> Collections.emptyList();
        this.children = new LinkedHashMap<>();
    }

    public String getSegment() {
        return segment;
    }

    /** Returns the MediaItem this node represents (e.g. the folder item shown in the library). */
    public MediaItem getMediaItem() {
        return mediaItem;
    }

    /**
     * Returns the list of MediaItems for this folder (both sub-folders and playable items).
     * Delegates to the loadChildren supplier.
     */
    public List<MediaItem> loadChildren() {
        return loadChildren.get();
    }

    public Supplier<List<MediaItem>> getLoadChildren() {
        return loadChildren;
    }

    /** Returns the child node for the given segment, or null if none. */
    @Nullable
    public MediaFolderNode getChild(String segment) {
        return children.get(segment);
    }

    /** Returns an unmodifiable view of all child nodes. */
    public Map<String, MediaFolderNode> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    /**
     * Adds or replaces a child node for the given segment. Returns the child node.
     */
    public MediaFolderNode putChild(String segment, MediaFolderNode node) {
        children.put(segment, node);
        return node;
    }

    /**
     * Gets the child for the given segment, or creates and adds one with the given
     * loadChildren supplier if absent.
     */
    public MediaFolderNode getOrCreateChild(String segment, MediaItem childMediaItem, Supplier<List<MediaItem>> loadChildren) {
        MediaFolderNode child = children.get(segment);
        if (child == null) {
            child = new MediaFolderNode(segment, childMediaItem, loadChildren);
            children.put(segment, child);
        }
        return child;
    }
}
