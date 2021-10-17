package taptap.reaction.ui.main

import taptap.pub.Reaction
import java.lang.IllegalStateException

class MainRepository {
    fun getData(): Reaction<String> = Reaction.on { "some calculating" }

    fun getAnotherData(firstData: String): Reaction<String> =
        Reaction.on { "some another calculating by $firstData" }

    fun getErrorData(): Reaction<String> =
        Reaction.on { "Hi there" }
}