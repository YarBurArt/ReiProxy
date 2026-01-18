import asyncio
import base64

from kivy.clock import Clock
from kivy.properties import StringProperty, BooleanProperty, ColorProperty
from kivy.uix.behaviors import ButtonBehavior
from kivy.uix.relativelayout import RelativeLayout

from libs.applibs.proxy import ProxyServer
from mitmproxy.options import Options
from libs.uix.baseclass.base_screen import BaseScreen


class HistoryListItem(ButtonBehavior, RelativeLayout):
    """single pair of request and response in the HTTP history list """
    req_id = StringProperty("...")
    req_method = StringProperty("...")
    req_host = StringProperty("...")
    req_path = StringProperty("...")
    res_status = StringProperty("...")
    duration = StringProperty("")
    mime = StringProperty("")

    method_color = ColorProperty([1, 1, 1, 1])
    status_color = ColorProperty([1, 1, 1, 1])

    is_selected = BooleanProperty(False)

    def __init__(self, screen, transaction_id, **kwargs):
        super().__init__(**kwargs)
        self.screen = screen
        self.transaction_id = transaction_id

    def dispatch_selection(self):
        """Tell the parent screen that this row was clicked."""
        self.screen.select_transaction(self.transaction_id)


class HistoryScreen(BaseScreen):
    proxy_server = None
    proxy_task = None

    transactions = {}

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._next_req_id = 1

    def on_enter(self):
        """called when the screen is displayed."""
        if not self.proxy_server:
            self.start_proxy()
            self.update_event = Clock.schedule_interval(
                self.poll_proxy_logs, 0.5)

    def on_leave(self):
        """called when leaving the screen."""
        if hasattr(self, 'update_event'):
            self.update_event.cancel()

    def start_proxy(self):
        """initialize and run the ProxyServer in the asyncio loop."""
        opts = Options(
            listen_host='127.0.0.1',
            listen_port=8080, http2=True
        )

        self.proxy_server = ProxyServer(
            options=opts,
            loop=asyncio.get_running_loop(),
            with_termlog=False,
            with_dumper=False
        )

        try:
            loop = asyncio.get_running_loop()
            self.proxy_task = loop.create_task(
                self.proxy_server.run(on_shutdown=lambda exc: None)
            )
            print("Proxy started on 127.0.0.1:8080")
        except RuntimeError:
            print("Error: No asyncio loop found. "
                  "Ensure Kivy is running with asyncio.")

    def poll_proxy_logs(self, dt):
        """
        pull data from proxy._raw_logger,
        match req/res by ID, and update UI.
        """
        if not self.proxy_server:
            return

        # pop all available items from RawLogger
        items = self.proxy_server._raw_logger.pop_all()

        for item in items:
            flow_id = item.get("id")
            if not flow_id:
                continue

            if flow_id not in self.transactions:
                self.transactions[flow_id] = {
                    'req': None,
                    'res': None,
                    'widget': None,
                    'req_seq': None,
                }

            trans = self.transactions[flow_id]

            if item['type'] == 'request':
                trans['req'] = item
                if trans['req_seq'] is None:
                    trans['req_seq'] = self._next_req_id
                    self._next_req_id += 1
                self.add_list_item(flow_id, item, req_seq=trans['req_seq'])

            elif item['type'] == 'response':
                trans['res'] = item
                if trans['widget']:
                    self.update_list_item(trans['widget'], item, trans['req'])

    def add_list_item(self, flow_id, req_data, req_seq=0):
        """creates the visual row for a new request."""
        item = HistoryListItem(screen=self, transaction_id=flow_id)

        item.req_id = str(req_seq)
        item.req_method = req_data.get('method', 'GET')
        item.req_host = req_data.get(
            'headers', {}
        ).get(b'Host', b'').decode('utf-8', 'ignore') or "127.0.0.1"
        item.req_path = req_data.get('url', '/')

        m = item.req_method
        if m == "GET":
            item.method_color = [0.2, 0.6, 1, 1]
        elif m == "POST":
            item.method_color = [1, 0.6, 0.2, 1]
        elif m == "DELETE":
            item.method_color = [1, 0.2, 0.2, 1]
        else:
            item.method_color = [1, 1, 1, 1]

        self.transactions[flow_id]['widget'] = item
        self.ids.project_list.add_widget(item)

    def update_list_item(self, widget, res_data, req_data):
        """update visual row once the response arrives"""
        code = res_data.get('status_code', 0)
        widget.res_status = str(code)

        if 200 <= code < 300:
            widget.status_color = [0.3, 0.8, 0.3, 1]
        elif 400 <= code < 500:
            widget.status_color = [1, 0.6, 0.2, 1]
        elif code >= 500:
            widget.status_color = [1, 0.3, 0.3, 1]

        # MIME Type
        headers = res_data.get('headers', {})
        ctype = headers.get(b'Content-Type', b'').decode('utf-8', 'ignore')
        widget.mime = ctype.split(';')[0] if ctype else "-"

        if req_data and 'ts' in req_data and 'ts' in res_data:
            duration_ms = (res_data['ts'] - req_data['ts']) * 1000
            widget.duration = f"{int(duration_ms)}ms"

    def select_transaction(self, flow_id):
        """handle row click, deselect others and show RAW req/res"""
        for child in self.ids.project_list.children:
            child.is_selected = (child.transaction_id == flow_id)

        trans = self.transactions.get(flow_id)
        if not trans:
            return

        raw_req = self.generate_raw_string(trans['req'])
        raw_res = self.generate_raw_string(trans['res'])

        self.ids.req_h.text = raw_req
        self.ids.res_h.text = raw_res

    def generate_raw_string(self, item):
        """ generates a string representation
        of the RAW request or response """
        if not item:
            return ""
        output = []

        if item["type"] == "request":
            start = item.get(
                "start_line"
            ) or f"{item.get('method')} {item.get('url')}"
        else:
            start = item.get(
                "start_line"
            ) or f"HTTP/1.1 {item.get('status_code')}"
        output.append(start)

        # headers
        for k, v in (item.get("headers") or {}).items():
            # decode safely
            k_str = k.decode(
                'utf-8', 'ignore'
            ) if isinstance(k, bytes) else str(k)
            v_str = v.decode(
                'utf-8', 'ignore'
            ) if isinstance(v, bytes) else str(v)
            output.append(f"{k_str}: {v_str}")

        output.append("")  # line between headers and body

        # body response
        if item.get("body_text") is not None:
            output.append(item["body_text"])
        elif item.get("body_b64"):
            raw = base64.b64decode(item["body_b64"])
            try:
                output.append(raw.decode("utf-8"))
            except Exception:
                output.append(raw.decode("latin1", errors="replace"))

        return "\n".join(output)
