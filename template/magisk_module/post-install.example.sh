# This script will be loaded during the installation process of Sui.
#
# It's designed to automatically copy rish.
# rish is an Android program for interacting with a shell that runs on a high-privileged daemon process (Sui server).
#
# Rename this file to /data/adb/sui/post-install.sh to allow this script to be loaded.

# This line is required.
SCRIPT_VERSION=2

# Variables:
# $RISH_DEX:     the path to the dex of rish
# $RISH_LIB:     the path to the native library of rish
# $RISH_SCRIPT:  the path to the wrapper script to start rish

copy_rish() {
  APP_APPLICATION_ID=$1
  APP_UID=$2
  DEX_TARGET=$3/$4.dex
  LIB_TARGET=$3/librish.so
  SCRIPT_TARGET=$3/$4

  cp "$RISH_DEX" "$DEX_TARGET"
  cp "$RISH_LIB" "$LIB_TARGET"
  cp "$RISH_SCRIPT" "$SCRIPT_TARGET"
  chmod 400 "$DEX_TARGET" "$LIB_TARGET"
  chmod 700 "$SCRIPT_TARGET"
  chown "$APP_UID":"$APP_UID" "$DEX_TARGET" "$LIB_TARGET" "$SCRIPT_TARGET"
  sed -i "s/%%%RISH_APPLICATION_ID%%%/$APP_APPLICATION_ID/g" "$SCRIPT_TARGET"
}

# Example: copy rish to /data/local/tmp (for adb)
#
#ui_print "- Copy rish to /data/local/tmp"
#copy_rish com.android.shell 2000 /data/local/tmp rish # Since every app can check if a specific file exists in /data/local/tmp, it's recommended to change the last parameter "rish" to something else

# Example: copy rish to Termux
#
#ui_print "- Copy rish to Termux"
#APP_UID=$(stat -c '%u' /data/user/0/com.termux)
#copy_rish com.termux $APP_UID /data/user/0/com.termux/files/home rish
