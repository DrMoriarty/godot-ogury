extends Node2D

signal gdpr_answer(applies, approval, consent)

var _ads = null
onready var Production: bool = not OS.is_debug_build()
var isTop = true

func _ready():
    pause_mode = Node.PAUSE_MODE_PROCESS
    if(Engine.has_singleton("Ogury")):
        _ads = Engine.get_singleton("Ogury")
    if ProjectSettings.has_setting('Ogury/AssetKey'):
        var assetKey = ProjectSettings.get_setting('Ogury/AssetKey')
        init(assetKey)

func init(asset_key: String) -> void:
    if _ads != null:
        _ads.initWithCallback(asset_key, Production, get_instance_id(), '_on_get_consent')

func editConsent(callback_object: Object, callback_method: String) -> void:
    if _ads != null:
        _ads.editConsent(callback_object.get_instance_id(), callback_method)

func gdprApplies() -> bool:
    if _ads != null:
        return _ads.gdprApplies()
    else:
        return false

func _on_get_consent(applies: bool, approval: bool, consent: String) -> void:
    if OS.is_debug_build():
        print('GDPR applies: %s Policy approval: %s, Consent string: %s'%[var2str(applies), var2str(approval), consent])
    emit_signal('gdpr_answer', applies, approval, consent)
