#!/system/bin/sh
MODDIR=${0%/*}
[ ! -f "$MODDIR/riru.sh" ] && exit 1
. $MODDIR/riru.sh

# Rename module.prop.new
if [ -f "$RIRU_MODULE_PATH/module.prop.new" ]; then
  rm "$RIRU_MODULE_PATH/module.prop"
  mv "$RIRU_MODULE_PATH/module.prop.new" "$RIRU_MODULE_PATH/module.prop"
fi

run_server() {
    export CLASSPATH="/data/adb/sui/classes.dex"
    /system/bin/app_process -Djava.class.path=/data/adb/sui/classes.dex /system/bin --nice-name=sui app.rikka.sui.server.Starter
}

(run_server)&