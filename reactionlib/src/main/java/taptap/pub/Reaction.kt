package taptap.pub

import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation

/**
 * A class that encapsulates a successful result with a value of type [T] or a failure result with an [Exception] exception
 */
sealed class Reaction<out T> {

    open operator fun component1(): T? = null
    open operator fun component2(): Exception? = null

    abstract fun get(): T

    class Success<out T : Any?>(val data: T) : Reaction<T>() {

        override fun component1(): T = data

        override fun get(): T = data

        override fun toString() = "Success: $data"

        override fun hashCode(): Int = data.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*> && data == other.data
        }
    }

    class Error(val exception: Exception) : Reaction<Nothing>() {

        override fun component2(): Exception = exception

        override fun get(): Nothing {
            throw exception
        }

        override fun toString() = "Error: $exception"

        override fun hashCode(): Int = exception.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Error && exception == other.exception
        }
    }

    companion object {
        /**
         * Construct a Reaction from condition
         * ```kotlin
         * Reaction.onCondition { it == "what you want" }
         * ```
         */
        inline fun <T> onCondition(f: () -> Boolean): Reaction<Unit> {
            return try {
                if (f()) {
                    Success(Unit)
                } else {
                    Error(IllegalStateException("Illegal state"))
                }
            } catch (ex: Exception) {
                if (ex is CancellationException) {
                    throw ex
                }
                Error(ex)
            }
        }

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
            if (ex is CancellationException) {
                throw ex
            }
            Error(ex)
        }

        /**
         * Construct a safe Reaction from Reaction
         * ```kotlin
         * Reaction.tryReaction { Reaction.on { "something" } }
        `* ```
         */
        inline fun <T> tryReaction(f: () -> Reaction<T>): Reaction<T> = try {
            f()
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            Error(ex)
        }
    }
}

fun <T> T.toSuccessReaction() = Reaction.Success(this)

fun <E : Exception> E.toErrorReaction() = Reaction.Error(this)

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
inline fun <T> Reaction<T>.takeOrReturn(f: (Exception) -> Nothing): T = when (this) {
    is Reaction.Success -> this.data
    is Reaction.Error -> {
        f(this.exception)
        //throw IllegalStateException("You must write 'return' in the error lambda")
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
fun <T> Reaction<T>.takeOrNull(): T? = when (this) {
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
} catch (ex: Exception) {
    if (ex is CancellationException) {
        throw ex
    }
    Reaction.Error(ex)
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
} catch (ex: Exception) {
    if (ex is CancellationException) {
        throw ex
    }
    Reaction.Error(ex)
}

/**
 * Transform the result with success and error data by applying a function to it
 * ```kotlin
 * repository.getData()
 *     .mapReaction { s, e -> "Convert to another string: $s + $e" }
 * ```
 */
inline fun <R, T> Reaction<T>.mapReaction(f: (T?, Exception?) -> R): R {
    return when (this) {
        is Reaction.Success -> f(this.data, null)
        is Reaction.Error -> f(null, this.exception)
    }
}

/**
 * Transform the error result by applying a function to it
 * ```kotlin
 * repository.getData()
 *     .errorMap { IllegalStateException("something went wrong") }
 * ```
 */
inline fun <T> Reaction<T>.errorMap(f: (Exception) -> Exception) = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> Reaction.Error(f(this.exception))
    }
} catch (ex: Exception) {
    if (ex is CancellationException) {
        throw ex
    }
    Reaction.Error(ex)
}

/**
 * Transform the error result by applying a function to it to another Reaction
 * ```kotlin
 * repository.getData()
 *     .recover { "New reaction, much better then old" }
 * ```
 */
inline fun <T> Reaction<T>.recover(transform: (exception: Exception) -> T): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> Reaction.on { transform(this.exception) }
    }
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
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
inline fun <T> Reaction<T>.flatHandle(f: (T?, Exception?) -> Unit) {
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
inline fun <T> Reaction<T>.handle(success: (T) -> Unit, error: (Exception) -> Unit) {
    when (this) {
        is Reaction.Success -> success(this.data)
        is Reaction.Error -> error(this.exception)
    }
}

/**
 * Handle the Reaction result with on success and on error actions and transform them to the new object
 * ```kotlin
 * repository.getData()
 *     .fold(
 *         success = { State.Success(it) },
 *         error = { State.Error }
 *     )
 * ```
 */
inline fun <T, R> Reaction<T>.fold(success: (T) -> R, error: (Exception) -> R): R =
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
    if (e is CancellationException) {
        throw e
    }
    Reaction.Error(e)
}

/**
 * Register an action to take when Reaction is Error
 * ```kotlin
 * repository.getData()
 *     .doOnError { Log.d("Error! Let's dance but sadly =(") }
 * ```
 */
inline fun <T> Reaction<T>.doOnError(f: (Exception) -> Unit): Reaction<T> = try {
    when (this) {
        is Reaction.Success -> this
        is Reaction.Error -> {
            f(this.exception)
            this
        }
    }
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
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
    if (e is CancellationException) {
        throw e
    }
    Reaction.Error(e)
}

/**
 * Check is result success
 * ```kotlin
 * repository.getData()
 *     .isSuccess() == true
 * ```
 */
fun <T> Reaction<T>.isSuccess(): Boolean = this is Reaction.Success

/**
 * Check is result error
 * ```kotlin
 * repository.getData()
 *     .isSuccess() == true
 * ```
 */
fun <T> Reaction<T>.isError(): Boolean = this is Reaction.Error

//-------------------------Coroutine Continuation-------------------------

/**
 * Coroutine Continuation for success callback
 * ```kotlin
 * override fun success() {
 *     continuation.resumeWithReactionSuccess { true }
 * }
 * ```
 */
inline fun <T> Continuation<Reaction<T>>.resumeWithReactionSuccess(
    f: () -> T
): Reaction<T> = try {
    Reaction.on { f() }
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
    Reaction.Error(e)
}

/**
 * Coroutine Continuation for failure callback
 * ```kotlin
 * override fun failure(error: Throwable?) {
 *     continuation.resumeWithReactionError { error }
 * }
 * ```
 */
inline fun <T> Continuation<Reaction<T>>.resumeWithReactionError(
    f: () -> Throwable
): Reaction<T> = try {
    Reaction.Error(f() as Exception)
} catch (e: Exception) {
    if (e is CancellationException) {
        throw e
    }
    Reaction.Error(e)
}
