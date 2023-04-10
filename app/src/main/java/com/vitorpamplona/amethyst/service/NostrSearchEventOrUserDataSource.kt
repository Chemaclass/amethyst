package com.vitorpamplona.amethyst.service

import com.vitorpamplona.amethyst.model.decodePublicKey
import com.vitorpamplona.amethyst.service.model.*
import com.vitorpamplona.amethyst.service.relays.FeedType
import com.vitorpamplona.amethyst.service.relays.JsonFilter
import com.vitorpamplona.amethyst.service.relays.TypedFilter
import nostr.postr.bechToBytes
import nostr.postr.toHex

object NostrSearchEventOrUserDataSource : NostrDataSource("SingleEventFeed") {
    private var searchString: String? = null

    private fun createAnythingWithIDFilter(): List<TypedFilter>? {
        val mySearchString = searchString
        if (mySearchString.isNullOrBlank()) {
            return null
        }

        val hexToWatch = try {
            if (mySearchString.startsWith("npub") || mySearchString.startsWith("nsec")) {
                decodePublicKey(mySearchString).toHex()
            } else if (mySearchString.startsWith("note")) {
                mySearchString.bechToBytes().toHex()
            } else {
                mySearchString
            }
        } catch (e: Exception) {
            // Usually when people add an incomplete npub or note.
            null
        }

        // downloads all the reactions to a given event.
        return listOfNotNull(
            hexToWatch?.let {
                TypedFilter(
                    types = FeedType.values().toSet(),
                    filter = JsonFilter(
                        ids = listOfNotNull(hexToWatch)
                    )
                )
            },
            hexToWatch?.let {
                TypedFilter(
                    types = FeedType.values().toSet(),
                    filter = JsonFilter(
                        kinds = listOf(MetadataEvent.kind),
                        authors = listOfNotNull(hexToWatch)
                    )
                )
            },
            TypedFilter(
                types = FeedType.values().toSet(),
                filter = JsonFilter(
                    kinds = listOf(MetadataEvent.kind),
                    search = mySearchString,
                    limit = 20
                )
            ),
            TypedFilter(
                types = FeedType.values().toSet(),
                filter = JsonFilter(
                    kinds = listOf(TextNoteEvent.kind, LongTextNoteEvent.kind, PollNoteEvent.kind, ChannelMetadataEvent.kind, ChannelCreateEvent.kind, ChannelMessageEvent.kind),
                    search = mySearchString,
                    limit = 20
                )
            )
        )
    }

    val searchChannel = requestNewChannel()

    override fun updateChannelFilters() {
        searchChannel.typedFilters = createAnythingWithIDFilter()
    }

    fun search(searchString: String) {
        this.searchString = searchString
        invalidateFilters()
    }

    fun clear() {
        searchString = null
    }
}
