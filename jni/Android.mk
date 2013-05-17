LOCAL_PATH := $(call my-dir) 

include $(CLEAR_VARS) 
LOCAL_MODULE := libvpx-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvpx.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/vpx/
LOCAL_STATIC_LIBRARIES := cpufeatures 
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libogg-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libogg.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ogg/
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libvorbis-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvorbis.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/vorbis/
LOCAL_STATIC_LIBRARIES := libogg-prebuilt
include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS) 
LOCAL_MODULE := libvorbisfile-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvorbisfile.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/vorbis/
LOCAL_STATIC_LIBRARIES := libogg-prebuilt
include $(PREBUILT_STATIC_LIBRARY)
include $(CLEAR_VARS) 
LOCAL_MODULE := libvorbisenc-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvorbisenc.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/vorbis/
LOCAL_STATIC_LIBRARIES := libogg-prebuilt libvorbis-prebuilt
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libkrad-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libkrad.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/krad_nanolib
LOCAL_STATIC_LIBRARIES := libogg-prebuilt libvorbis-prebuilt libvorbisenc-prebuilt libvpxenc-prebuilt
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := krcam
LOCAL_SRC_FILES := krcam.cpp krcam_util.c
LOCAL_STATIC_LIBRARIES := cpufeatures libogg-prebuilt libvpx-prebuilt libvorbisenc-prebuilt libvorbis-prebuilt libkrad-prebuilt 
LOCAL_CFLAGS 	:= -D__STDC_CONSTANT_MACROS
LOCAL_LDLIBS    := -llog -landroid -lEGL -lGLESv1_CM
LOCAL_ARM_MODE := arm
include $(BUILD_SHARED_LIBRARY)
$(call import-module,android/cpufeatures)
