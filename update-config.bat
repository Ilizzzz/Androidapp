@echo off
echo 正在更新Android配置文件...

cd html
call npm run update-config

echo.
echo 配置更新完成！
pause 