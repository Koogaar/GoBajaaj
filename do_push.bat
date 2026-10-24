@echo off
set PATH=%PATH%;C:\Program Files\Git\cmd
git config user.name "Koogaar"
git config user.email "koogaar@gmail.com"

echo === Building Admin Web and syncing to public/ ===
call npm --prefix admin-web run build

git add .
git commit -m "%~1"
if "%~1"=="" git commit -m "update: deploy latest changes"
git push origin main

