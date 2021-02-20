package taptap.parcel_test

import taptap.pub.Reaction

class TestRepository {
    fun getData(): Reaction<String> = Reaction.of { "some calculating" }
}