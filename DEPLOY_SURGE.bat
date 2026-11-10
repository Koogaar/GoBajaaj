@echo off
echo ============================================
echo   SHIRI Delivery - Surge.sh Deploy
echo ============================================
echo.
echo Waxaad u baahan tahay email iyo password.
echo Haddaadan account surge lahayn, mid cusub
echo ayaa si toos ah loo samaynayaa!
echo.
echo Domain-ka: shiri-delivery.surge.sh
echo.
cd /d E:\go\GoBajaaj_DriverPass_Fixed_Source\admin-web\dist
npx surge . shiri-delivery.surge.sh
echo.
echo ============================================
echo URL: https://shiri-delivery.surge.sh/delete-account.html
echo ============================================
pause

