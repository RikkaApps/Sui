#!/system/bin/sh
MODDIR=${0%/*}
ROOT_PATH="/data/adb/sui"

[ ! -f "$MODDIR/riru.sh" ] && exit 1
. $MODDIR/riru.sh

move_new_file() {
  if [ -f "$1.new" ]; then
    rm "$1"
    mv "$1.new" "$1"
  fi
}

move_new_dir() {
  if [ -d "$1.new" ]; then
    rm -rf "$1"
    mv "$1.new" "$1"
  fi
}
move_new_file "$RIRU_MODULE_PATH/module.prop"
move_new_file "$ROOT_PATH/sui.dex"
move_new_dir "$ROOT_PATH/res"

chmod 700 /data/adb/sui/starter
exec /data/adb/sui/starter