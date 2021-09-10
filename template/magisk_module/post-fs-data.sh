#!/system/bin/sh
MODDIR=${0%/*}
MAGISK_PATH="$(magisk --path)/.magisk/modules/riru-sui"
log -p d -t "Sui" "Magisk module path $MAGISK_PATH"

# Setup adb root support
rm "$MODDIR/bin/adb_root"
ln -s "$MODDIR/bin/sui" "$MODDIR/bin/adb_root"
chmod 700 "$MODDIR/bin/adb_root"
"$MODDIR/bin/adb_root" "$MAGISK_PATH"

# Run Sui server
chmod 700 "$MODDIR"/bin/sui
exec "$MODDIR"/bin/sui "$MODDIR"
