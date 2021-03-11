#!/system/bin/sh
MODDIR=${0%/*}

chmod 700 "$MODDIR"/starter
exec "$MODDIR"/starter "$MODDIR"/sui.dex