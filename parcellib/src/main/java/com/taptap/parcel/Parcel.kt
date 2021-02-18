package com.taptap.parcel

sealed class Parcel<out T> {
    data class Success<out T>(val data: T) : Parcel<T>()
    data class Error(val exception: Exception) : Parcel<Nothing>()

    companion object {
        inline fun <T> of(f: () -> T): Parcel<T> = try {
            val result = f()
            if (result is Parcel<*>) {
                result as Parcel<T>
            } else {
                Success(result)
            }
        } catch (ex: Exception) {
            Error(ex)
        }
    }
}

inline fun <T> Parcel<T>.takeOrReturn(f: (Exception) -> Unit): T = when (this) {
    is Parcel.Success -> this.data
    is Parcel.Error -> {
        f(this.exception)
        throw IllegalStateException("You must write 'return' in the error lambda")
    }
}

inline fun <T, R> Parcel<T>.flatMap(f: (T) -> Parcel<R>) = try {
    when (this) {
        is Parcel.Success -> f(this.data)
        is Parcel.Error -> this
    }
} catch (e: Exception) {
    Parcel.Error(e)
}

inline fun <R, T> Parcel<T>.map(f: (T) -> R) = try {
    when (this) {
        is Parcel.Success -> Parcel.Success(f(this.data))
        is Parcel.Error -> this
    }
} catch (e: Exception) {
    Parcel.Error(e)
}

inline fun <T> Parcel<T>.errorMap(f: (Exception) -> Exception) = try {
    when (this) {
        is Parcel.Success -> this
        is Parcel.Error -> Parcel.Error(f(this.exception))
    }
} catch (e: Exception) {
    Parcel.Error(e)
}

inline fun <T> Parcel<T>.flatHandle(f: (T?, Exception?) -> Unit) {
    when (this) {
        is Parcel.Success -> f(this.data, null)
        is Parcel.Error -> f(null, this.exception)
    }
}

inline fun <T> Parcel<T>.doOnComplete(f: () -> Unit) {
    f()
}

inline fun <T> Parcel<T>.handle(success: (T) -> Unit, error: (Exception) -> Unit) {
    when (this) {
        is Parcel.Success -> success(this.data)
        is Parcel.Error -> error(this.exception)
    }
}

inline fun <T, R> Parcel<T>.zip(success: (T) -> R, error: (Exception) -> R): R =
    when (this) {
        is Parcel.Success -> success(this.data)
        is Parcel.Error -> error(this.exception)
    }

inline fun <T> Parcel<T>.doOnSuccess(f: (T) -> Unit): Parcel<T> = try {
    when (this) {
        is Parcel.Success -> {
            f(this.data)
            this
        }
        is Parcel.Error -> this
    }
} catch (e: Exception) {
    Parcel.Error(e)
}

inline fun <T> Parcel<T>.doOnError(f: (Exception) -> Unit): Parcel<T> = try {
    when (this) {
        is Parcel.Success -> this
        is Parcel.Error -> {
            f(this.exception)
            this
        }
    }
} catch (e: Exception) {
    Parcel.Error(e)
}

inline fun <T> Parcel<T>.check(f: (T) -> Boolean, noinline lazyMessage: () -> String): Parcel<T> =
    try {
        when (this) {
            is Parcel.Success -> {
                check(f(this.data)) { lazyMessage }
                this
            }
            is Parcel.Error -> this
        }
    } catch (e: Exception) {
        Parcel.Error(e)
    }