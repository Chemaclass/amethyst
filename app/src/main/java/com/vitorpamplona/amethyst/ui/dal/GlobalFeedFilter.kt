package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.*

object GlobalFeedFilter : FeedFilter<Note>() {
    lateinit var account: Account

    override fun feed(): List<Note> {
        val followChannels = account.followingChannels()
        val followUsers = account.followingKeySet()
        val now = System.currentTimeMillis() / 1000

        val notes = LocalCache.notes.values
            .asSequence()
            .filter {
                it.event is BaseTextNoteEvent && it.replyTo.isNullOrEmpty()
            }
            .filter {
                val channel = it.channel()
                // does not show events already in the public chat list
                (channel == null || channel !in followChannels) &&
                    // does not show people the user already follows
                    (it.author?.pubkeyHex !in followUsers)
            }
            .filter { account.isAcceptable(it) }
            .filter {
                // Do not show notes with the creation time exceeding the current time, as they will always stay at the top of the global feed, which is cheating.
                it.createdAt()!! <= now
            }
            .toList()

        val longFormNotes = LocalCache.addressables.values
            .asSequence()
            .filter {
                it.event is LongTextNoteEvent && it.replyTo.isNullOrEmpty()
            }
            .filter {
                val channel = it.channel()
                // does not show events already in the public chat list
                (channel == null || channel !in followChannels) &&
                    // does not show people the user already follows
                    (it.author?.pubkeyHex !in followUsers)
            }
            .filter { account.isAcceptable(it) }
            .filter {
                // Do not show notes with the creation time exceeding the current time, as they will always stay at the top of the global feed, which is cheating.
                it.createdAt()!! <= now
            }
            .toList()

        return (notes + longFormNotes)
            .sortedBy { it.createdAt() }
            .reversed()
    }
}
