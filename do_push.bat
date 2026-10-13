@echo off
set PATH=%PATH%;C:\Program Files\Git\cmd
git config user.name "Koogaar"
git config user.email "koogaar@gmail.com"
git add .
git commit -m "chore: updated version to 3.3 (25), Driver Pass 24h expiration, retry payment button, animated splash, and generated APK/AAB"
git push origin main
