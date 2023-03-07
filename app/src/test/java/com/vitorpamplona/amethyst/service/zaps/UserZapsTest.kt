package com.vitorpamplona.amethyst.service.zaps

import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.LnZapEventInterface
import com.vitorpamplona.amethyst.service.model.zaps.UserZaps
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import java.math.BigDecimal

class UserZapsTest {
  @Test
  fun user_without_zaps() {
    val actual = UserZaps.groupZapsByUser(zaps = null)

    Assert.assertEquals(emptyList<Note>(), actual)
  }

  @Test
  fun avoid_duplicates_with_same_zap_request() {
    val zapRequest = mockk<Note>()

    val zaps = mapOf(
      zapRequest to mockZapNote(zapAmount = 100),
      zapRequest to mockZapNote(zapAmount = 200),
    )

    val actual = UserZaps.groupZapsByUser(zaps)

    Assert.assertEquals(1, actual.count())
    Assert.assertEquals(
      BigDecimal(200),
      (actual.first().second.event as LnZapEventInterface).amount()
    )
  }

  @Test
  fun aggregate_zap_amount_group_by_user() {
    val zaps = mapOf(
      mockUserNote("user-1") to mockZapNote(zapAmount = 100),
      mockUserNote("user-1") to mockZapNote(zapAmount = 200),
      mockUserNote("user-2") to mockZapNote(zapAmount = 400),
    )

    val actual = UserZaps.groupZapsByUser(zaps)

    Assert.assertEquals(2, actual.count())

    Assert.assertEquals(
      BigDecimal(300),
      (actual[0].second.event as LnZapEventInterface).amount()
    )

    Assert.assertEquals(
      BigDecimal(400),
      (actual[1].second.event as LnZapEventInterface).amount()

    )
  }

  private fun mockUserNote(authorPubKey: String): Note {
    val userNote = mockk<Note>()
    every { userNote.author?.pubkeyHex } returns authorPubKey

    return userNote
  }

  private fun mockZapNote(zapAmount: Int): Note {
    val lnZapEvent = mockk<LnZapEventInterface>()
    every { lnZapEvent.amount() } returns zapAmount.toBigDecimal()

    val zapNote = mockk<Note>()
    every { zapNote.event } returns lnZapEvent

    return zapNote
  }
}
