# This script will be loaded during the installation process of Sui.
#
# It's designed to automatically copy the command-line tool.
# For example, you can copy the tool to a terminal app, so that you can use this tool in any terminal app you like.
#
# Rename this file to /data/adb/sui/post-install.sh to allow this script to be loaded.

# Variables:
# SUI_DEX:  the path to the dex of the command-line tool
# SUI_WRAPPER:      the path to the wrapper script to start the dex

copy_cmd_tool() {
  APP_APPLICATION_ID=$1
  APP_UID=$2
  SUI_DEX_TARGET=$3/$4.dex
  SUI_WRAPPER_TARGET=$3/$4

  cp "$SUI_DEX" "$SUI_DEX_TARGET"
  cp "$SUI_WRAPPER" "$SUI_WRAPPER_TARGET"
  chmod 600 "$SUI_DEX" "$SUI_DEX_TARGET"
  chmod 700 "$SUI_WRAPPER" "$SUI_WRAPPER_TARGET"
  chown "$APP_UID":"$APP_UID" "$SUI_DEX_TARGET" "$SUI_WRAPPER_TARGET"
  sed -i "s/%%%SUI_APPLICATION_ID%%%/$APP_APPLICATION_ID/g" "$SUI_WRAPPER_TARGET"
}

# Example: copy the tool to /data/local/tmp (for adb)
ui_print "- Copy command-line tool to /data/local/tmp"
copy_cmd_tool com.android.shell 2000 /data/local/tmp sui # Since every app can check if a specific file exists in /data/local/tmp, it's recommended to change the last parameter "sui" to something else

# Example: copy the tool to Termux
ui_print "- Copy command-line tool to Termux"
APP_UID=$(stat -c '%u' /data/user/0/com.termux)
copy_cmd_tool com.termux $APP_UID /data/user/0/com.termux/files/home sui