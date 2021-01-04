#!/system/bin/sh
MODDIR=${0%/*}
[ ! -f "$MODDIR/riru.sh" ] && exit 1
. $MODDIR/riru.sh

# Rename module.prop.new
if [ -f "$RIRU_MODULE_PATH/module.prop.new" ]; then
  rm "$RIRU_MODULE_PATH/module.prop"
  mv "$RIRU_MODULE_PATH/module.prop.new" "$RIRU_MODULE_PATH/module.prop"
fi

chmod 700 /data/adb/sui/starter
exec /data/adb/sui/starter