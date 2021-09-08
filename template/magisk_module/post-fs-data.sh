#!/system/bin/sh
MODDIR=${0%/*}

log -p d -t "Sui" "Magisk module path $MODDIR"

# Setup adb root support
rm "$MODDIR"/bin/adb_root
ln -s "$MODDIR"/bin/sui "$MODDIR"/bin/adb_root
chmod 700 "$MODDIR"/bin/adb_root
"$MODDIR"/bin/adb_root "$MODDIR"

# Run Sui server
chmod 700 "$MODDIR"/bin/sui
exec "$MODDIR"/bin/sui "$MODDIR"
