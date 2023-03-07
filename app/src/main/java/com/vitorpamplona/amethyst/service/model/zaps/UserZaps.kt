package com.vitorpamplona.amethyst.service.model.zaps

import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.service.model.LnZapEventInterface

object UserZaps {
  fun groupZapsByUser(zaps: Map<Note, Note?>?): List<Pair<Note, Note>> {
    if (zaps == null) return emptyList()

//    "Alex": [
//      Work(workPlace="workPlace", years=1),
//      Work(workPlace="workPlace", years=4),
//      Work(workPlace="workPlace", years=5),
//      Work(workPlace="workPlace", years=1)],
//    "John": [
//      Work(workPlace="workPlace", years=2),
//      Work(workPlace="workPlace", years=2),
//      Work(workPlace="workPlace", years=1),
//      Work(workPlace="workPlace", years=6)]
//
//    val people: Map<String, List<Work>> = ...
//    val peopleToSumOfYears: Map<String, Int> =
//      people.mapValues { (name, works) -> works.sumOf { it.years } }

    val m: Map<String?, List<Pair<Note, Note?>>> = zaps
      .filter { it.value != null }
      .toList()
      .sortedBy { (it.second?.event as? LnZapEventInterface)?.amount() }
      .groupBy { it.first.author?.pubkeyHex }

    val o = m.mapValues { (pubkey, pairs) ->
      pairs.fold(0) { acc, pair ->
        acc = acc + (pair.second?.event as? LnZapEventInterface)?.amount()
      }
    }

    var p = o.keys

//    val peopleToSumOfZaps = m.forEach { (pubkey, pairs) ->
////      (it.second?.event as? LnZapEventInterface)?.amount())
//      pairs.map
//    }

    return (zaps
      .filter { it.value != null }
      .toList()
      .sortedBy { (it.second?.event as? LnZapEventInterface)?.amount() }
//      .groupBy { it.first.author?.pubkeyHex }
      .reversed()) as List<Pair<Note, Note>>

//    return (zaps
//      .filter { it.value != null }
//      .toList()
//      .sortedBy { (it.second?.event as? LnZapEventInterface)?.amount() }
//      .reversed()) as List<Pair<Note, Note>>
  }
}
