#!/system/bin/sh
MODDIR=${0%/*}

log -p d -t "Sui" "Magisk module path $MODDIR"

chmod 700 "$MODDIR"/sui
exec "$MODDIR"/sui "$MODDIR"/sui.dex "$MODDIR"

