#!/bin/bash
adb logcat -c
adb logcat | grep -E "ReplyContentCard|ReplyComposeVM|ReplyOptionsSection|ReplyComposeContent"
