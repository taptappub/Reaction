package taptap.pub

/**
 * A class that encapsulates a successful result with a value of type [T] or a failure result with an [Throwable] exception
 */
sealed class Reaction<out T> {
    data class Success<out T>(val data: T) : Reaction<T>()
    data class Error(val exception: Throwable) : Reaction<Nothing>()

    companion object {
        /**
         * Construct a safe Reaction from statement
         * ```kotlin
         * Reaction.on { "something" }
         * ```
         */
        inline fun <T> on(f: () -> T): Reaction<T> = try {
            val result = f()
            if (result is Reaction<*>) {
                result as Reaction<T>
            } else {
                Success(result)
            }
        } catch (ex: Exception) {
            Error(ex)
        }
    }
}

/**
 * Unwrap and receive the success result data or do a function with *return*
 * ```kotlin
 * val data = repository.getData()
 *     .takeOrReturn {
 *         Log.d("LOG", "it is an error again")
 *         return
 *     }
```
 */
inline fun <T> Reaction<T>.takeOrReturn(f: (Throwable) -> Unit): T = when (this) {
    is Reaction.Success -> this.data
    is Reaction.Error -> {
        f(this.exception)
        throw IllegalStateException("You must write 'return' in the error lambda")
    }
}

/**
 * Unwrap and receive the success result data or receive the default value in error case
 * ```kotlin
 * val data = repository.getData()
 *     .takeOrDefault {
 *         "default data"
 *     }
 * ```
 */
inline fun <T> Reaction<T>.takeOrDefault(default: () -> T): T = when (this) {
    is Reaction.Success -> this.data
    is Reaction.Error -> default()
}

/**
 * Unwrap and receive the success result data or receive '''null''' in error case
 * ```kotlin
 * val data = repository.getData()
 *     .takeOrNull()
 */
inline fun <T> Reaction<T>.takeOrNull(): T? = when (this) {
    is Reaction.Success -> this.data
    is Reaction.Error -> null
}

/**
 * Transform the success result by applying a function to it to another Reaction
 * ```kotlin
 * repository.getData()
 *     .flatMap { Reaction.of { "Flatmapped data" } }
 * ```
 */
inline fun <T, R> Reaction<T>.flatMap(f: (T) -> Reaction<R>) = try {
    when (this) {
        is Reaction.Success -> f(this.data)
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Transform the success result by applying a function to it
 * ```kotlin
 * repository.getData()
 *     .map { "Convert to another string" }
 * ```
 */
inline fun <R, T> Reaction<T>.map(f: (T) -> R) = try {
    when (this) {
        is Reaction.Success -> Reaction.Success(f(this.data))
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Transform the error result by applying a function to it
 * ```kotlin
 * repository.getData()
 *     .errorMap { IllegalStateException("something went wrong") }
 * ```
 */
inline fun <T> Reaction<T>.errorMap(f: (Throwable) -> Throwable) = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> Reaction.Error(f(this.exception))
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Transform the error result by applying a function to it to another Reaction
 * ```kotlin
 * repository.getData()
 *     .recover { "New reaction, much better then old" }
 * ```
 */
inline fun <T> Reaction<T>.recover(transform: (exception: Throwable) -> T): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> Reaction.on { transform(this.exception) }
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Handle the Reaction result with one action with success and error
 * ```kotlin
 * repository.getData(data)
 *     .flatHandle { success, error ->
 *         Log.d("LOG", "Let's combine results ${success.toString() + error.toString()}")
 *     }
 * ```
 */
inline fun <T> Reaction<T>.flatHandle(f: (T?, Throwable?) -> Unit) {
    when (this) {
        is Reaction.Success -> f(this.data, null)
        is Reaction.Error -> f(null, this.exception)
    }
}

/**
 * Register an action to take when Reaction is nevermind
 * ```kotlin
 * repository.getData()
 *     .doOnComplete { Log.d("Let's dance in any case!") }
 * ```
 */
inline fun <T> Reaction<T>.doOnComplete(f: () -> Unit) {
    f()
}

/**
 * Handle the Reaction result with on success and on error actions
 * ```kotlin
 * repository.getData(data)
 *     .handle(
 *         success = { liveData.postValue(it) },
 *         error = { Log.d("LOG", "Error. That's a shame") }
 *     )
```
 */
inline fun <T> Reaction<T>.handle(success: (T) -> Unit, error: (Throwable) -> Unit) {
    when (this) {
        is Reaction.Success -> success(this.data)
        is Reaction.Error -> error(this.exception)
    }
}

/**
 * Handle the Reaction result with on success and on error actions and transform them to the new object
 * ```kotlin
 * repository.getData()
 *     .zip(
 *         success = { State.Success(it) },
 *         error = { State.Error }
 *     )
 * ```
 */
inline fun <T, R> Reaction<T>.zip(success: (T) -> R, error: (Throwable) -> R): R =
    when (this) {
        is Reaction.Success -> success(this.data)
        is Reaction.Error -> error(this.exception)
    }

/**
 * Register an action to take when Reaction is Success
 * ```kotlin
 * repository.getData()
 *     .doOnSuccess { Log.d("Success! Let's dance!") }
 * ```
 */
inline fun <T> Reaction<T>.doOnSuccess(f: (T) -> Unit): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> {
            f(this.data)
            this
        }
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Register an action to take when Reaction is Error
 * ```kotlin
 * repository.getData()
 *     .doOnError { Log.d("Error! Let's dance but sadly =(") }
 * ```
 */
inline fun <T> Reaction<T>.doOnError(f: (Throwable) -> Unit): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> {
            f(this.exception)
            this
        }
    }
} catch (e: Exception) {
    Reaction.Error(e)
}

/**
 * Check the success result by a function
 * ```kotlin
 * repository.getData()
 *     .check { it.isNotEmpty() }
 * ```
 */
inline fun <T> Reaction<T>.check(
    message: String = "",
    f: (T) -> Boolean
): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> {
            check(f(this.data)) { message }
            this
        }
        is Reaction.Error -> this
    }
} catch (e: Exception) {
    Reaction.Error(e)
}
