#!/system/bin/sh
MODDIR=${0%/*}
MAGISK_PATH="$(magisk --path)/.magisk/modules/riru-sui"
log -p d -t "Sui" "Magisk module path $MAGISK_PATH"

# Run magiskpolicy manually if Magisk does not load sepolicy.rule
if [ ! -e "$(magisk --path)/.magisk/mirror/sepolicy.rules/riru-sui/sepolicy.rule" ]; then
  log -p e -t "Sui" "Magisk does not load sepolicy.rule..."
  log -p e -t "Sui" "Exec magiskpolicy --live --apply $MAGISK_PATH/sepolicy.rule..."
  magiskpolicy --live --apply "$MAGISK_PATH"/sepolicy.rule
  log -p i -t "Sui" "Apply finished"
else
  log -p i -t "Sui" "Magisk should have loaded sepolicy.rule correctly"
fi

# Setup adb root support
rm "$MODDIR/bin/adb_root"
ln -s "$MODDIR/bin/sui" "$MODDIR/bin/adb_root"
chmod 700 "$MODDIR/bin/adb_root"
"$MODDIR/bin/adb_root" "$MAGISK_PATH"

# Run Sui server
chmod 700 "$MODDIR"/bin/sui
exec "$MODDIR"/bin/sui "$MODDIR"
