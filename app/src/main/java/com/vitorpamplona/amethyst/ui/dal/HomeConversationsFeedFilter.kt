package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.TextNoteEvent

object HomeConversationsFeedFilter : FeedFilter<Note>() {
    lateinit var account: Account

    override fun feed(): List<Note> {
        val user = account.userProfile()
        val followingKeySet = user.cachedFollowingKeySet()
        val followingTagSet = user.cachedFollowingTagSet()

        return LocalCache.notes.values
            .filter {
                (it.event is TextNoteEvent) &&
                    (it.author?.pubkeyHex in followingKeySet || (it.event?.isTaggedHashes(followingTagSet) ?: false)) &&
                    // && account.isAcceptable(it)  // This filter follows only. No need to check if acceptable
                    it.author?.let { !account.isHidden(it) } ?: true &&
                    !it.isNewThread()
            }
            .sortedBy { it.createdAt() }
            .reversed()
    }
}
