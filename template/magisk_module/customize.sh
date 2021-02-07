SKIPUNZIP=1

ROOT_PATH="/data/adb/sui"
mkdir $ROOT_PATH
set_perm "$ROOT_PATH" 0 0 0600 $RIRU_SECONTEXT

# check_architecture
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

# extract riru.sh
extract "$ZIPFILE" 'riru.sh' "$MODPATH"
. $MODPATH/riru.sh

check_riru_version

# extract libs
ui_print "- Extracting module files"

extract "$ZIPFILE" 'module.prop' "$MODPATH"
extract "$ZIPFILE" 'post-fs-data.sh' "$MODPATH"
extract "$ZIPFILE" 'uninstall.sh' "$MODPATH"

mkdir "$MODPATH/system"

if [ "$ARCH" = "x86" ] || [ "$ARCH" = "x64" ]; then
  ui_print "- Extracting x86 libraries"
  extract "$ZIPFILE" "lib/x86/libriru_$RIRU_MODULE_ID.so" "$MODPATH"
  mv "$MODPATH/lib/x86" "$MODPATH/system/lib"

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting x64 libraries"
    extract "$ZIPFILE" "system_x86/lib64/libriru_$RIRU_MODULE_ID.so" "$MODPATH"
    mv "$MODPATH/lib/x86_64" "$MODPATH/system/lib64"
    extract "$ZIPFILE" "system_x86/lib64/libstarter.so" "$ROOT_PATH" true
  else
    extract "$ZIPFILE" "system_x86/lib/libstarter.so" "$ROOT_PATH" true
  fi
fi

if [ "$ARCH" = "arm" ] || [ "$ARCH" = "arm64" ]; then
  ui_print "- Extracting arm libraries"
  extract "$ZIPFILE" "lib/armeabi-v7a/libriru_$RIRU_MODULE_ID.so" "$MODPATH"
  mv "$MODPATH/lib/armeabi-v7a" "$MODPATH/system/lib"

  if [ "$IS64BIT" = true ]; then
    ui_print "- Extracting arm64 libraries"
    extract "$ZIPFILE" "lib/arm64-v8a/libriru_$RIRU_MODULE_ID.so" "$MODPATH"
    mv "$MODPATH/lib/arm64-v8a" "$MODPATH/system/lib64"
    extract "$ZIPFILE" "lib/arm64-v8a/libstarter.so" "$ROOT_PATH" true
  else
    extract "$ZIPFILE" "lib/armeabi-v7a/libstarter.so" "$ROOT_PATH" true
  fi
fi

rm -rf "$MODPATH/lib"

set_perm_recursive "$MODPATH" 0 0 0755 0644

rm "$ROOT_PATH/starter"
mv "$ROOT_PATH/libstarter.so" "$ROOT_PATH/starter"
set_perm "$ROOT_PATH/starter" 0 0 0700 $RIRU_SECONTEXT

# extract Riru files
ui_print "- Extracting extra files"
[ -d "$RIRU_MODULE_PATH" ] || mkdir -p "$RIRU_MODULE_PATH" || abort "! Can't create $RIRU_MODULE_PATH"

rm -f "$RIRU_MODULE_PATH/module.prop.new"
extract "$ZIPFILE" 'riru/module.prop.new' "$RIRU_MODULE_PATH" true
set_perm "$RIRU_MODULE_PATH/module.prop.new" 0 0 0600 $RIRU_SECONTEXT

# extract server files
ui_print "- Extracting Sui files"
rm -rf "$ROOT_PATH/tmp" && mkdir "$ROOT_PATH/tmp"

extract "$ZIPFILE" 'sui.dex' "$ROOT_PATH/tmp"
rm "$ROOT_PATH/sui.dex.new"
mv "$ROOT_PATH/tmp/sui.dex" "$ROOT_PATH/sui.dex.new"

extract "$ZIPFILE" 'res/layout/confirmation_dialog.xml' "$ROOT_PATH/tmp"
extract "$ZIPFILE" 'res/layout/management_dialog.xml' "$ROOT_PATH/tmp"
extract "$ZIPFILE" 'res/layout/management_app_item.xml' "$ROOT_PATH/tmp"
extract "$ZIPFILE" 'res/drawable/ic_su_24.xml' "$ROOT_PATH/tmp"
extract "$ZIPFILE" 'res/drawable/ic_close_24.xml' "$ROOT_PATH/tmp"
rm -rf "$ROOT_PATH/res.new"
mv "$ROOT_PATH/tmp/res" "$ROOT_PATH/res.new"

set_perm "$ROOT_PATH/sui.dex" 0 0 0600 $RIRU_SECONTEXT
set_perm_recursive "$ROOT_PATH/res.new" 0 0 0700 0600 $RIRU_SECONTEXT

rm -rf "$ROOT_PATH/tmp"