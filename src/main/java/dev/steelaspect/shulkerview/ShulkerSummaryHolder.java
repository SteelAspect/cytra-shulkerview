package dev.steelaspect.shulkerview;

import org.jspecify.annotations.Nullable;

/** Added to ShulkerBoxBlockEntity. Server: last summary sent to clients. Client: summary the server sent. */
public interface ShulkerSummaryHolder {
    @Nullable ShulkerSummary cytra$getSummary();

    void cytra$setSummary(@Nullable ShulkerSummary summary);
}
