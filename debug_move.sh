#!/bin/bash
set -x
echo "Starting debug script" > /Users/jang/Desktop/Study/GuestHouse/debug_log.txt
pwd >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt
ls -la /Users/jang/.gemini/antigravity/brain/db6ea040-ff7c-4c0f-95a7-9c8dd20dbdc9/uploaded_image_0_1768196249110.png >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt 2>&1
mkdir -p /Users/jang/Desktop/Study/GuestHouse/frontend/src/assets/icons >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt 2>&1
cp /Users/jang/.gemini/antigravity/brain/db6ea040-ff7c-4c0f-95a7-9c8dd20dbdc9/uploaded_image_0_1768196249110.png /Users/jang/Desktop/Study/GuestHouse/frontend/src/assets/icons/heart-active.png >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt 2>&1
cp /Users/jang/.gemini/antigravity/brain/db6ea040-ff7c-4c0f-95a7-9c8dd20dbdc9/uploaded_image_1_1768196249110.png /Users/jang/Desktop/Study/GuestHouse/frontend/src/assets/icons/heart-inactive.png >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt 2>&1
ls -la /Users/jang/Desktop/Study/GuestHouse/frontend/src/assets/icons/ >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt 2>&1
echo "Finished debug script" >> /Users/jang/Desktop/Study/GuestHouse/debug_log.txt
