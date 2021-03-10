# Ogury Choice Manager plugin for Godot engine.

Use [NativeLib Addon](https://github.com/DrMoriarty/nativelib) or [NativeLib-CLI](https://github.com/DrMoriarty/nativelib-cli) for installation.

# Usage

To let user make his choise you only need this:

```
func _ready():
    ogury.connect('gdpr_answer', self, '_on_gdpr_answer')

func _on_gdpr_answer(applies: bool, approval: bool, consent: String):
   # initialize ad network or put this data to it if network already initialized
```

Some of networks can use consent automatically, so their api don't have methods for putting this values.
