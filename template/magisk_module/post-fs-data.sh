#!/system/bin/sh
MODDIR=${0%/*}

# Remove unused files
rm -rf /data/adb/sui/res
rm -rf /data/adb/sui/res.new
rm -f /data/adb/sui/com.android.settings
rm -f /data/adb/sui/com.android.systemui
rm -f /data/adb/sui/post-install.example.sh
rm -f /data/adb/sui/starter
rm -f /data/adb/sui/sui.dex
rm -f /data/adb/sui/sui.dex.new
rm -f /data/adb/sui/sui_wrapper

chmod 700 "$MODDIR"/starter
exec "$MODDIR"/starter "$MODDIR"/sui.dex