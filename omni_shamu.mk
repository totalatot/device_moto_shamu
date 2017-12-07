# Include omni board-specific configurations
# include device/moto/shamu/BoardConfigOmni.mk

# Inherit base AOSP device configuration
$(call inherit-product, device/moto/shamu/aosp_shamu.mk)

# Override product naming for Omni
PRODUCT_NAME := omni_shamu
PRODUCT_BRAND := motorola
PRODUCT_MODEL := Moto X Pro
PRODUCT_DEVICE := shamu
PRODUCT_MANUFACTURER := motorola
PRODUCT_RESTRICT_VENDOR_FILES := false

# Device Fingerprint
PRODUCT_BUILD_PROP_OVERRIDES += \
    BUILD_FINGERPRINT=google/shamu/shamu:7.1.1/N6F27M/4299435:user/release-keys \
    PRIVATE_BUILD_DESC="OPR5.170623.011"

# Inherit OmniROM parts
$(call inherit-product, vendor/omni/config/gsm.mk)
$(call inherit-product, vendor/omni/config/common.mk)

# Inherit proprietary bits
$(call inherit-product-if-exists, vendor/moto/shamu/device-vendor.mk)

# Inherit TWRP configuration
include device/moto/shamu/twrp.mk
