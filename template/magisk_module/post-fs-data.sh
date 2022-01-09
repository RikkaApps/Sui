#!/system/bin/sh
MODDIR=${0%/*}
MODULE_ID=$(basename "$MODDIR")
FLAVOR=@FLAVOR@

if [ "$ZYGISK_ENABLED" = false ] && [ "$FLAVOR" = "zygisk" ]; then
  log -p w -t "Sui" "Zygisk is disabled, skip zygisk-flavor script"
  exit 1
fi

if [ "$ZYGISK_ENABLED" = true ] && [ "$FLAVOR" = "riru" ]; then
  log -p w -t "Sui" "Zygisk is enabled, skip riru-flavor script"
  exit 1
fi

MAGISK_VER_CODE=$(magisk -V)
if [ "$MAGISK_VER_CODE" -ge 21000 ]; then
  MAGISK_PATH="$(magisk --path)/.magisk/modules/$MODULE_ID"
else
  MAGISK_PATH=/sbin/.magisk/modules/$MODULE_ID
fi

log -p i -t "Sui" "Magisk version $MAGISK_VER_CODE"
log -p i -t "Sui" "Magisk module path $MAGISK_PATH"

enable_once="/data/adb/sui/enable_adb_root_once"
enable_forever="/data/adb/sui/enable_adb_root"
adb_root_exit=0

if [ -f $enable_once ]; then
  log -p i -t "Sui" "adb root support is enabled for this time of boot"
  rm $enable_once
  enable_adb_root=true
fi

if [ -f $enable_forever ]; then
  log -p i -t "Sui" "adb root support is enabled forever"
  enable_adb_root=true
fi

if [ "$enable_adb_root" = true ]; then
  log -p i -t "Sui" "Setup adb root support"

  # Run magiskpolicy manually if Magisk does not load sepolicy.rule
  if [ ! -e "$(magisk --path)/.magisk/mirror/sepolicy.rules/$MODULE_ID/sepolicy.rule" ]; then
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
  adb_root_exit=$?
  log -p i -t "Sui" "Exited with $adb_root_exit"
else
  log -p i -t "Sui" "adb root support is disabled"
fi

# Setup uninstaller
rm "$MODDIR/bin/uninstall"
ln -s "$MODDIR/bin/sui" "$MODDIR/bin/uninstall"

# Run Sui server
chmod 700 "$MODDIR"/bin/sui
exec "$MODDIR"/bin/sui "$MODDIR" "$adb_root_exit"
