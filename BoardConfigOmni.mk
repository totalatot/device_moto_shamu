# Include additional properties in separate system.prop
TARGET_SYSTEM_PROP := device/moto/shamu/omni_system.prop

# Enable sound trigger for hotword detection
BOARD_SUPPORTS_SOUND_TRIGGER := true

# TWRP
TW_THEME := portrait_hdpi
TW_INCLUDE_L_CRYPTO := true
TARGET_HW_DISK_ENCRYPTION := false
RECOVERY_GRAPHICS_USE_LINELENGTH := true
TARGET_RECOVERY_PIXEL_FORMAT := "RGB_565"
TW_SCREEN_BLANK_ON_BOOT := true
BOARD_SUPPRESS_SECURE_ERASE := true

# Additions
TARGET_USERIMAGES_USE_F2FS := true
TW_DEFAULT_BRIGHTNESS := 30
TW_NO_SCREEN_TIMEOUT := true
#RECOVERY_SDCARD_ON_DATA := true
BOARD_HAS_NO_REAL_SDCARD := true
BOARD_HAS_NO_SELECT_BUTTON := true
#BOARD_HAS_LARGE_FILESYSTEM := true

# Asian region languages
TW_EXTRA_LANGUAGES := true
TW_DEFAULT_LANGUAGE := zh_CN

#BOARD_CUSTOM_BOOTIMG_MK := device/moto/shamu/mkbootimg.mk
#BOARD_CANT_BUILD_RECOVERY_FROM_BOOT_PATCH := true
