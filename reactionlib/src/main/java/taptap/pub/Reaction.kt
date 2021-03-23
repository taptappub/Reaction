package taptap.pub

/**
 * A class that encapsulates a successful result with a value of type [T] or a failure result with an [Throwable] exception
 */
sealed class Reaction<out T> {

    open operator fun component1(): T? = null
    open operator fun component2(): Throwable? = null

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

    class Error(val exception: Throwable) : Reaction<Nothing>() {

        override fun component2(): Throwable = exception

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

        inline fun <T> tryReaction(f: () -> Reaction<T>): Reaction<T> = try {
            f()
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
 * Transform the result with success and error data by applying a function to it
 * ```kotlin
 * repository.getData()
 *     .mapReaction { s, e -> "Convert to another string: $s + $e" }
 * ```
 */
inline fun <R, T> Reaction<T>.mapReaction(f: (T?, Throwable?) -> R): R {
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

sealed class Result<out V : Any?, out E : Exception> {

    open operator fun component1(): V? = null
    open operator fun component2(): E? = null

    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X = when (this) {
        is Success -> success(this.value)
        is Failure -> failure(this.error)
    }

    abstract fun get(): V

    class Success<out V : Any?>(val value: V) : Result<V, Nothing>() {
        override fun component1(): V? = value

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = value.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*> && value == other.value
        }
    }

    class Failure<out E : Exception>(val error: E) : Result<Nothing, E>() {
        override fun component2(): E? = error

        override fun get() = throw error

        fun getException(): E = error

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*> && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun <E : Exception> error(ex: E) = Failure(ex)

        fun <V : Any?> success(v: V) = Success(v)

        inline fun <V : Any?> of(value: V?, fail: (() -> Exception) = { Exception() }): Result<V, Exception> =
                value?.let { success(it) } ?: error(fail())

        inline fun <V : Any?, reified E: Exception> of(noinline f: () -> V): Result<V, E> = try {
            success(f())
        } catch (ex: Exception) {
            when (ex) {
                is E -> error(ex)
                else -> throw ex
            }
        }
    }

}