# from kivy.uix.screenmanager import Screen
import threading
import httpx
from kivy.clock import Clock
from libs.uix.baseclass.base_screen import BaseScreen
from libs.applibs import request_read


class HomeScreen(BaseScreen):
    # changing screens also can be done in python
    # def goto_settings_screen(self):
    #     self.manager.push("settings")

    def send_request(self):
        self.ids.req_output.text = "Processing..."
        threading.Thread(target=self._execute_raw_request, daemon=True).start()

    def _execute_raw_request(self):
        try:
            raw_text = self.ids.req_input.text
            if not raw_text.strip():
                return

            req = request_read.HTTPRequest(raw_text)

            if req.error_code:
                error_msg = "HTTP Parse Error " + \
                            f"{req.error_code}: {req.error_message}"
                Clock.schedule_once(lambda dt: self._update_ui(error_msg))
                return

            method = req.command.upper()
            path = req.path

            # reconstruct absolute URL
            host = req.headers.get("Host")
            if path.startswith("http"):
                final_url = path
            elif host:
                protocol = "http" if ":80" in host else "https"
                final_url = f"{protocol}://{host.strip()}{path}"
            else:
                raise ValueError(
                    "Relative path used but no 'Host' header found."
                )

            body = req.rfile.read()
            # convert req.headers (HTTPMessage) to a standard dict for httpx
            with httpx.Client(
                verify=False, follow_redirects=True, timeout=5.0
            ) as client:
                response = client.request(
                    method=method,
                    url=final_url,
                    headers=dict(req.headers),
                    content=body
                )

                # 'HTTP/1.1' or 'HTTP/2'
                res_text = f"{response.http_version}" + \
                           f" {response.status_code}" + \
                           f" {response.reason_phrase}\n"
                for k, v in response.headers.items():
                    res_text += f"{k}: {v}\n"
                res_text += f"\n{response.text}"

                Clock.schedule_once(lambda dt: self._update_ui(res_text))

        except Exception as e:
            err_out = f"Execution Error: {type(e).__name__} - {str(e)}"
            print(err_out)
            Clock.schedule_once(lambda dt: self._update_ui(err_out))

    def _update_ui(self, text):
        self.ids.req_output.text = text
