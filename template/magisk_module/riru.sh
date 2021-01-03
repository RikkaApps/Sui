#!/sbin/sh
RIRU_PATH="/data/adb/riru"
RIRU_MODULE_ID="%%%RIRU_MODULE_ID%%%"
RIRU_MODULE_PATH="$RIRU_PATH/modules/$RIRU_MODULE_ID"
RIRU_SECONTEXT="u:object_r:magisk_file:s0"

# used by /data/adb/riru/util_functions.sh
RIRU_MODULE_API_VERSION=%%%RIRU_MODULE_API_VERSION%%%
RIRU_MODULE_MIN_API_VERSION=%%%RIRU_MODULE_MIN_API_VERSION%%%
RIRU_MODULE_MIN_RIRU_VERSION_NAME="%%%RIRU_MODULE_MIN_RIRU_VERSION_NAME%%%"

# this function will be used when /data/adb/riru/util_functions.sh not exits
check_riru_version() {
  if [ ! -f "$RIRU_PATH/api_version" ] && [ ! -f "$RIRU_PATH/api_version.new" ]; then
    ui_print "*********************************************************"
    ui_print "! Riru $RIRU_MIN_VERSION_NAME or above is required"
    ui_print "! Please install Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
    abort "*********************************************************"
  fi
  local_api_version=$(cat "$RIRU_PATH/api_version.new") || local_api_version=$(cat "$RIRU_PATH/api_version") || local_api_version=0
  [ "$local_api_version" -eq "$local_api_version" ] || local_api_version=0
  ui_print "- Riru API version: $local_api_version"
  if [ "$local_api_version" -lt $RIRU_MODULE_MIN_API_VERSION ]; then
    ui_print "*********************************************************"
    ui_print "! Riru $RIRU_MIN_VERSION_NAME or above is required"
    ui_print "! Please upgrade Riru from Magisk Manager or https://github.com/RikkaApps/Riru/releases"
    abort "*********************************************************"
  fi
}

if [ -f /data/adb/riru/util_functions.sh ]; then
  ui_print "- Load /data/adb/riru/util_functions.sh"
  . /data/adb/riru/util_functions.sh
else
  ui_print "- Can't find /data/adb/riru/util_functions.sh"
fi