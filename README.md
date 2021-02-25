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
            .map { "covert to another string" }
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
