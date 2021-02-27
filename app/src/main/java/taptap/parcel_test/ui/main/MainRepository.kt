package taptap.parcel_test.ui.main

import taptap.pub.Reaction

class MainRepository {
    fun getData(): Reaction<String> = Reaction.on { "some calculating" }
    fun getAnotherData(firstData: String): Reaction<String> =
        Reaction.on { "some another calculating by $firstData" }
}