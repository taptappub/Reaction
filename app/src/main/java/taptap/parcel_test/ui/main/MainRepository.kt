package taptap.parcel_test.ui.main

import taptap.pub.Reaction

class MainRepository {
    fun getData(): Reaction<String> = Reaction.of { "some calculating" }
}