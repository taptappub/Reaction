# Reaction lib [![Maven Central](https://img.shields.io/maven-central/v/io.github.taptappub/reaction.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.taptappub%22%20AND%20a:%22reaction%22)
A class that encapsulates a successful result with a value of type [T] or a failure result with an [Throwable] exception

## Add library to a project

```groovy
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    implementation "io.github.taptappub:reaction:$version"
}
```

## Samples

```kotlin
class MainRepository {
    fun getData(): Reaction<String> = Reaction.on { "some calculations" }
    fun getAnotherData(firstData: String): Reaction<String> = Reaction.on { "some another calculations based on $firstData" }
}
```

```kotlin
fun getData() {
    viewModelScope.launch(Dispatchers.IO) {
        repository.getData()
            .map { "Convert to another string" }
            .doOnError { Log.d("LOG", "it is an error") }
            .zip(
                success = {
                    State.Success(it)
                },
                error = {
                    State.Error
                }
            )
            .let { liveData.postValue(it) }
    }
}
```

```kotlin
fun getAnotherData() {
    viewModelScope.launch(Dispatchers.IO) {
        val data = repository.getData()
            .check { it.isNotEmpty() }
            .flatMap { Reaction.of { "Flatmapped data" } }
            .takeOrReturn {
                Log.d("LOG", "it is an error again")
                return@launch
            }
        repository.getAnotherData(data)
            .handle(
                success = { liveData.postValue(State.Success(it)) },
                error = { Log.d("LOG", "Error too") }
            )
    }
}
```

## List of methods
 - **on** - Construct a safe Reaction from statement 
```kotlin
Reaction.on { "something" }
```
 - **tryReaction** - Construct a safe Reaction from Reaction 
```kotlin
Reaction.tryReaction { Reaction.on { "something" } }
```
 - **map** - Transform the success result by applying a function to it
```kotlin
repository.getData()
    .map { "Convert to another string" }
```
- **map** - Transform the result with success and error data by applying a function to it
```kotlin
repository.getData()
    .mapReaction { s, e -> "Convert to another string: $s + $e" }
```
- **flatMap** - Transform the success result by applying a function to it to another Reaction
```kotlin
repository.getData()
    .flatMap { Reaction.of { "Flatmapped data" } }
```
- **errorMap** - Transform the error result by applying a function to it
```kotlin
repository.getData()
    .errorMap { IllegalStateException("something went wrong") }
```
- **recover** - Transform the error result by applying a function to it to another Reaction
```kotlin
repository.getData()
    .recover { "New reaction, much better then old" }
```
- **doOnSuccess** - Register an action to take when Reaction is Success
```kotlin
repository.getData()
    .doOnSuccess { Log.d("Success! Let's dance!") }
```
- **doOnError** - Register an action to take when Reaction is Error
```kotlin
repository.getData()
    .doOnError { Log.d("Error! Let's dance but sadly =(") }
```
- **doOnComplete** - Register an action to take when Reaction is nevermind
```kotlin
repository.getData()
    .doOnComplete { Log.d("Let's dance in any case!") }
```
- **handle** - Handle the Reaction result with on success and on error actions
```kotlin
repository.getData(data)
    .handle(
          success = { liveData.postValue(it) },
          error = { Log.d("LOG", "Error. That's a shame") }
    )
```
- **flatHandle** - Handle the Reaction result with one action with success and error
```kotlin
repository.getData(data)
    .flatHandle { success, error ->
      Log.d("LOG", "Let's combine results ${success.toString() + error.toString()}")
    }
```
- **zip** - Handle the Reaction result with on success and on error actions and transform them to the new object
```kotlin
repository.getData()
    .zip(
        success = { State.Success(it) },
        error = { State.Error }
    )
```
- **check** - Check the success result by a function
```kotlin
repository.getData()
    .check { it.isNotEmpty() }
```
- **takeOrReturn** - Unwrap and receive the success result data or do a function with *return*
```kotlin
val data = repository.getData()
    .takeOrReturn {
        Log.d("LOG", "it is an error again")
        return
    }
```
- **takeOrDefault** - Unwrap and receive the success result data or receive the default value in error case
```kotlin
val data = repository.getData()
    .takeOrDefault {
        "default data"
    }
```
- **takeOrNull** - Unwrap and receive the success result data or receive *null* in error case
```kotlin
val data = repository.getData()
    .takeOrNull()
```
- **resumeWithReactionSuccess** - Coroutine Continuation for success callback
```kotlin
override fun success() {
    continuation.resumeWithReactionSuccess { true }
}
```
- **resumeWithReactionError** - Coroutine Continuation for failure callback
```kotlin
override fun failure(error: Throwable?) {
    continuation.resumeWithReactionError { error }
}
```
# License

   Copyright 2021 Aleksey Potapov

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
