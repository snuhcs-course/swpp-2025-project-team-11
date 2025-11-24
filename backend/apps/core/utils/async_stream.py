from collections.abc import AsyncGenerator, Callable, Generator
from functools import wraps

from asgiref.sync import sync_to_async

_SENTINEL = object()


def as_async_stream(func: Callable[..., Generator[str, None, None]]) -> Callable[..., AsyncGenerator[str, None]]:
    @wraps(func)
    async def wrapper(*args, **kwargs) -> AsyncGenerator[str, None]:
        gen = func(*args, **kwargs)

        def _next() -> object:
            try:
                return next(gen)
            except StopIteration:
                return _SENTINEL

        next_async = sync_to_async(_next, thread_sensitive=True)

        try:
            while True:
                chunk = await next_async()
                if chunk is _SENTINEL:
                    break
                yield chunk
        finally:
            try:
                gen.close()
            except Exception:
                pass

    return wrapper
