#!/sbin/sh
MODDIR=${0%/*}
[ ! -f "$MODDIR/riru.sh" ] && exit 1
. $MODDIR/riru.sh

rm -rf "$RIRU_MODULE_PATH"