from kivy.uix.boxlayout import BoxLayout
from kivy.properties import ObjectProperty


class BottomDock(BoxLayout):
    """app.root.push(name)."""
    app = ObjectProperty()

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.app = kwargs.get("app")
        self.orientation = "horizontal"
