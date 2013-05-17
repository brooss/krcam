LOCAL_PATH := $(call my-dir) 



include $(CLEAR_VARS) 
LOCAL_MODULE := libvpx-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvpx.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libvorbis-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libogg.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libogg-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libvorbis.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS) 
LOCAL_MODULE := libkrad-prebuilt 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libkrad.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := krcam
LOCAL_SRC_FILES := krcam.cpp krcam_util.c
LOCAL_STATIC_LIBRARIES := cpufeatures libvpx-prebuilt libogg-prebuilt libvorbis-prebuilt libkrad-prebuilt 
LOCAL_CFLAGS 	:= -D__STDC_CONSTANT_MACROS
LOCAL_LDLIBS    := -llog -landroid -lEGL -lGLESv1_CM 
LOCAL_ARM_MODE := arm
include $(BUILD_SHARED_LIBRARY)
$(call import-module,android/cpufeatures)
