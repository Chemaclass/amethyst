package com.vitorpamplona.amethyst.ui.dal

import com.vitorpamplona.amethyst.model.Account
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.model.AppRecommendationEvent
import com.vitorpamplona.amethyst.service.model.BookmarkListEvent
import com.vitorpamplona.amethyst.service.model.PeopleListEvent

class UserProfileNewThreadFeedFilter(val user: User, val account: Account) : FeedFilter<Note>() {
    override fun feedKey(): String {
        return account.userProfile().pubkeyHex + "-" + user.pubkeyHex
    }

    override fun feed(): List<Note> {
        val longFormNotes = LocalCache.addressables.values
            .filter { it.author == user && (it.event !is PeopleListEvent && it.event !is BookmarkListEvent && it.event !is AppRecommendationEvent) }

        return user.notes
            .plus(longFormNotes)
            .filter { account.isAcceptable(it) == true && it.isNewThread() }
            .sortedWith(compareBy({ it.createdAt() }, { it.idHex }))
            .reversed() ?: emptyList()
    }
}
