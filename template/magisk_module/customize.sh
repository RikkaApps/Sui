SKIPUNZIP=1

ROOT_PATH="/data/adb/sui"
mkdir $ROOT_PATH
set_perm "$ROOT_PATH" 0 0 0600

# Check architecture
if [ "$ARCH" != "arm" ] && [ "$ARCH" != "arm64" ] && [ "$ARCH" != "x86" ] && [ "$ARCH" != "x64" ]; then
  abort "! Unsupported platform: $ARCH"
else
  ui_print "- Device platform: $ARCH"
fi

# extract verify.sh
ui_print "- Extracting verify.sh"
unzip -o "$ZIPFILE" 'verify.sh' -d "$TMPDIR" >&2
if [ ! -f "$TMPDIR/verify.sh" ]; then
  ui_print    "*********************************************************"
  ui_print    "! Unable to extract verify.sh!"
  ui_print    "! This zip may be corrupted, please try downloading again"
  abort "*********************************************************"
fi
. $TMPDIR/verify.sh

# Extract riru.sh
extract "$ZIPFILE" 'riru.sh' "$MODPATH"
. $MODPATH/riru.sh

check_riru_version
enforce_install_from_magisk_app

# Extract libs
ui_print "- Extracting module files"

extract "$ZIPFILE" 'module.prop' "$MODPATH"
extract "$ZIPFILE" 'post-fs-data.sh' "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh' "$MODPATH"

mkdir "$MODPATH/riru"
mkdir "$MODPATH/riru/lib"
mkdir "$MODPATH/riru/lib64"

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- Extracting x86 libraries"
  extract "$ZIPFILE" "lib/x86/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib" true

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting x64 libraries"
    extract "$ZIPFILE" "lib/x86_64/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib64" true
    extract "$ZIPFILE" "lib/x86_64/libstarter.so" "$MODPATH" true
    extract "$ZIPFILE" "lib/x86_64/librish.so" "$MODPATH" true
  else
    extract "$ZIPFILE" "lib/x86/libstarter.so" "$MODPATH" true
    extract "$ZIPFILE" "lib/x86/librish.so" "$MODPATH" true
fi
fi

if [ "$ARCH" = "arm" ] || [ "$ARCH" = "arm64" ]; then
  ui_print "- Extracting arm libraries"
  extract "$ZIPFILE" "lib/armeabi-v7a/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib" true

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting arm64 libraries"
    extract "$ZIPFILE" "lib/arm64-v8a/lib$RIRU_MODULE_LIB_NAME.so" "$MODPATH/riru/lib64" true
    extract "$ZIPFILE" "lib/arm64-v8a/libstarter.so" "$MODPATH" true
    extract "$ZIPFILE" "lib/arm64-v8a/librish.so" "$MODPATH" true
  else
    extract "$ZIPFILE" "lib/armeabi-v7a/libstarter.so" "$MODPATH" true
    extract "$ZIPFILE" "lib/armeabi-v7a/librish.so" "$MODPATH" true
  fi
fi

set_perm_recursive "$MODPATH" 0 0 0755 0644

mv "$MODPATH/libstarter.so" "$MODPATH/starter"
set_perm "$MODPATH/starter" 0 0 0700

# Extract server files
ui_print "- Extracting Sui files"

extract "$ZIPFILE" 'sui.dex' "$MODPATH"
extract "$ZIPFILE" 'sui.apk' "$MODPATH"

extract "$ZIPFILE" 'res/layout/confirmation_dialog.xml' "$MODPATH"
extract "$ZIPFILE" 'res/layout/management_dialog.xml' "$MODPATH"
extract "$ZIPFILE" 'res/layout/management_app_item.xml' "$MODPATH"
extract "$ZIPFILE" 'res/drawable/ic_su_24.xml' "$MODPATH"
extract "$ZIPFILE" 'res/drawable/ic_close_24.xml' "$MODPATH"

set_perm "$MODPATH/sui.dex" 0 0 0600
set_perm "$MODPATH/sui.apk" 0 0 0655
set_perm_recursive "$MODPATH/res" 0 0 0700 0600

ui_print "- Fetching information for SystemUI and Settings"
/system/bin/app_process -Djava.class.path="$MODPATH"/sui.dex /system/bin --nice-name=sui_installer rikka.sui.installer.Installer "$MODPATH"

ui_print "- Extracting files for rish"
extract "$ZIPFILE" 'rish' "$MODPATH"
extract "$ZIPFILE" 'post-install.example.sh' "$MODPATH"
set_perm "$MODPATH/rish" 0 2000 0770
set_perm "$MODPATH/post-install.example.sh" 0 0 0600

if [ -f $ROOT_PATH/post-install.sh ]; then
  cat "$ROOT_PATH/post-install.sh" | grep -q "SCRIPT_VERSION=2"
  if [ "$?" -eq 0 ]; then
    RISH_DEX=$MODPATH/sui.dex
    RISH_LIB=$MODPATH/librish.so
    RISH_SCRIPT=$MODPATH/rish
    ui_print "- Run /data/adb/sui/post-install.sh"
    source $ROOT_PATH/post-install.sh
  else
    ui_print "! To use new interactive shell tool (rish), post-install.sh needs update"
    ui_print "! Please check post-install.example.sh for more"
  fi
else
  ui_print "- Cannot find /data/adb/sui/post-install.sh"
fi
