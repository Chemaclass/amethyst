package com.vitorpamplona.amethyst.service.model

import com.vitorpamplona.amethyst.model.HexKey
import java.math.BigDecimal

interface EventInterface {
    fun id(): HexKey

    fun pubKey(): HexKey

    fun createdAt(): Long

    fun kind(): Int

    fun tags(): List<List<String>>

    fun content(): String

    fun sig(): HexKey

    fun toJson(): String

    fun checkSignature()

    fun hasValidSignature(): Boolean

    fun isTaggedUser(loggedInUser: String): Boolean

    fun isTaggedHash(hashtag: String): Boolean
    fun isTaggedHashes(hashtag: Set<String>): Boolean
    fun firstIsTaggedHashes(hashtag: Set<String>): String?
    fun hashtags(): List<String>

    fun getReward(): BigDecimal?

    fun clone(): EventInterface
}
