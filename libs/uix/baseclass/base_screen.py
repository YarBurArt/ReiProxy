from kivy.uix.boxlayout import BoxLayout
from kivy.uix.anchorlayout import AnchorLayout
from kivy.uix.screenmanager import Screen
from kivy.lang import Builder
from libs.uix.widgets.dock import BottomDock

Builder.load_file("libs/uix/widgets/dock.kv")  # vertical menu


# should not be registered in screens.json
class BaseScreen(Screen):
    """
    base screen to be inherited and extended
    """
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        anchor = AnchorLayout(anchor_x='center', anchor_y='center')
        root = BoxLayout(
            orientation="vertical",
        )

        self.content = BoxLayout(orientation="vertical")
        root.add_widget(self.content)

        dock = BottomDock(app=self.get_app())
        root.add_widget(dock)
        anchor.add_widget(root)
        self.add_widget(anchor)

    def get_app(self):
        from kivy.app import App
        return App.get_running_app()
