# Reaction lib [![](https://jitpack.io/v/taptappub/Reaction.svg)](https://jitpack.io/#taptappub/Reaction)

The wrapper on business logic response.

## Add library to a project

```groovy
allprojects {
  repositories {
    jcenter()
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
    implementation "com.github.taptappub:Reaction:$version"
}
```

## Samples

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
 - **of** - Construct a safe Reaction from statement 
```kotlin
Reaction.of { "something" }
```
 - **map** - Transform the success result by applying a function to it
```kotlin
repository.getData()
    .map { "Convert to another string" }
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
