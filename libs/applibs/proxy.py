from typing import Callable, Dict, List
import base64
import time
import asyncio

from collections import deque
from threading import Lock

from mitmproxy.options import Options
from mitmproxy.tools.dump import DumpMaster

from mitmproxy import http


def safe_preview(data: bytes, max_len=2048):
    if data is None:
        return b""
    if len(data) <= max_len:
        return data
    return data[:max_len] + b"...[truncated to 2048 bytes]"


class RawLogger:
    """
    addon for mitm proxy to log raw requests and responses in dict
    """
    def __init__(self, max_items=500):
        self._lock = Lock()
        self._items = deque(maxlen=max_items)

    def _push(self, item: dict):
        with self._lock:
            self._items.append(item)

    def pop_all(self):
        with self._lock:
            items = list(self._items)
            self._items.clear()
        return items

    def pop_one(self):
        with self._lock:
            return self._items.popleft() if self._items else None

    def request(self, flow: http.HTTPFlow) -> None:
        req = flow.request
        try:
            body_text = req.get_text(strict=False)
            body_bytes = req.get_content(strict=False) or b""
        except Exception:
            body_text = None
            body_bytes = req.raw_content or b""

        item = {
            "type": "request",
            "id": getattr(flow, "id", None),
            "ts": time.time(),
            "start_line": f"{req.method} {req.path} HTTP/{req.http_version}",
            "method": req.method,
            "url": req.url,
            "headers": dict(req.headers.items()),
            "body_text": body_text,
            "body_b64": base64.b64encode(
                body_bytes
            ).decode("ascii") if body_bytes else None,
        }
        self._push(item)

    def response(self, flow: http.HTTPFlow) -> None:
        resp = flow.response
        if resp is None:
            return
        try:
            body_text = resp.get_text(strict=False)
            body_bytes = resp.get_content(strict=False) or b""
        except Exception:
            body_text = None
            body_bytes = resp.raw_content or b""

        item = {
            "type": "response",
            "id": getattr(flow, "id", None),
            "ts": time.time(),
            "start_line": f"HTTP/{resp.http_version} "
                          f"{resp.status_code} {resp.reason}",
            "status_code": resp.status_code,
            "headers": dict(resp.headers.items()),
            "body_text": body_text,
            "body_b64": base64.b64encode(
                body_bytes
            ).decode("ascii") if body_bytes else None,
        }
        self._push(item)


class ProxyServer(DumpMaster):
    """
    check here https://github.com/mitmproxy/mitmproxy/issues/3306
    """
    def __init__(
        self,
        options: Options = Options(
            listen_host='127.0.0.1',
            listen_port=8080,
            http2=True,
        ),
        loop=None, with_termlog=False, with_dumper=True,
    ):
        super().__init__(
            options=options,
            loop=loop,
            with_termlog=with_termlog,
            with_dumper=with_dumper,
        )
        self._raw_logger = RawLogger()
        self.addons.add(self._raw_logger)

    async def run(
        self,
        on_shutdown: Callable[[Exception], None] | None = None
    ):
        exc: Exception | None = None
        try:
            await DumpMaster.run(self)
        except Exception as e:
            exc = e
            if not callable(on_shutdown):
                raise
        finally:
            if callable(on_shutdown):
                on_shutdown(exc)
            self.shutdown()


def print_raw(logger_data: List[Dict]) -> None:
    """print raw request / response from logger"""
    for it in logger_data:
        if it["type"] == "request":
            start = it.get("start_line") or \
                f"{it.get('method')} {it.get('url')}"
        else:
            start = it.get("start_line") or f"HTTP/1.1 {it.get('status_code')}"
        print(start, flush=True)

        for k, v in (it.get("headers") or {}).items():
            print(f"{k}: {v}", flush=True)
        print("", flush=True)

        if it.get("body_text") is not None:
            print(it["body_text"], flush=True)
        elif it.get("body_b64"):
            raw = base64.b64decode(it["body_b64"])
            try:
                print(raw.decode("utf-8"), flush=True)
            except Exception:
                print(raw.decode(
                    "latin1", errors="replace"
                ), flush=True)
        print("#"*80, flush=True)


async def test_server():
    opts = Options(listen_host='127.0.0.1', listen_port=8080, http2=True)
    proxy = ProxyServer(
        options=opts, loop=None, with_termlog=False, with_dumper=False
    )

    # proxy.run in background task
    loop = asyncio.get_running_loop()
    run_task = loop.create_task(proxy.run(on_shutdown=lambda exc: None))

    try:
        # in usage should add update_ui() here
        while not run_task.done():
            # print and clear collected items right at run
            print_raw(proxy._raw_logger.pop_all())
            await asyncio.sleep(1)
    except asyncio.CancelledError:
        pass
    finally:
        if not run_task.done():
            run_task.cancel()
        await asyncio.sleep(0)

if __name__ == '__main__':
    try:
        # check here https://github.com/kivy/kivy/pull/5241
        asyncio.run(test_server())
    except KeyboardInterrupt:
        pass
