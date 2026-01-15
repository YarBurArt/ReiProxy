# from kivy.uix.screenmanager import Screen
import sys
from libs.uix.baseclass.base_screen import BaseScreen
from kivymd.uix.label import MDLabel
from kivy.metrics import dp


if hasattr(sys, 'getandroidapilevel'):  # Android
    from jnius import autoclass
    from android.runnable import run_on_ui_thread
    WebView = autoclass('android.webkit.WebView')
    WebViewClient = autoclass('android.webkit.WebViewClient')
    LayoutParams = autoclass('android.view.ViewGroup$LayoutParams')
    LinearLayout = autoclass('android.widget.LinearLayout')
    Activity = autoclass('org.kivy.android.PythonActivity').mActivity
else:
    def run_on_ui_thread(func):
        return func


class ToolsScreen(BaseScreen):
    webview = None

    def on_enter(self, *args):
        super().on_enter(*args)
        if hasattr(sys, 'getandroidapilevel'):
            self.create_webview()
        else:
            self.show_dev_message()
            self.open_browser()

    @run_on_ui_thread
    def create_webview(self):
        if not self.webview:
            self.webview = WebView(Activity)
            self.webview.getSettings().setJavaScriptEnabled(True)
            self.webview.setWebViewClient(WebViewClient())

            bottom_margin = int(dp(55))  # fix for bottom dock
            layout = LinearLayout(Activity)
            layout.setOrientation(1)  # Vertical

            params = LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            Activity.addContentView(self.webview, params)

            self.webview.loadUrl('https://gchq.github.io/CyberChef/')

    def on_leave(self, *args):
        super().on_leave(*args)

        if self.webview and hasattr(sys, 'getandroidapilevel'):
            self.hide_webview()

    @run_on_ui_thread
    def hide_webview(self):
        if self.webview:
            self.webview.setVisibility(8)  # 8 = GONE

    @run_on_ui_thread
    def show_webview(self):
        if self.webview:
            self.webview.setVisibility(0)  # 0 = VISIBLE

    def show_dev_message(self):
        self.ids.webview_container.clear_widgets()
        self.ids.webview_container.add_widget(
            MDLabel(
                text="Dev testing on Win/Linux",
                halign="center",
                theme_text_color="Custom",
                text_color=(0.8, 0.8, 0.8, 1)
            )
        )

    def open_browser(self):
        import webbrowser
        webbrowser.open("https://gchq.github.io/CyberChef/")
