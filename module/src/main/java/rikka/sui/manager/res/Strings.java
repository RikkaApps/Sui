package rikka.sui.manager.res;

import java.util.HashMap;
import java.util.Locale;

import static rikka.sui.manager.res.Res.string.COUNT;
import static rikka.sui.manager.res.Res.string.brackets_format;
import static rikka.sui.manager.res.Res.string.close;
import static rikka.sui.manager.res.Res.string.grant_dialog_button_allow_always;
import static rikka.sui.manager.res.Res.string.grant_dialog_button_allow_one_time;
import static rikka.sui.manager.res.Res.string.grant_dialog_button_deny;
import static rikka.sui.manager.res.Res.string.grant_dialog_button_deny_and_dont_ask_again;
import static rikka.sui.manager.res.Res.string.management_title;
import static rikka.sui.manager.res.Res.string.permission_allowed;
import static rikka.sui.manager.res.Res.string.permission_ask;
import static rikka.sui.manager.res.Res.string.permission_denied;
import static rikka.sui.manager.res.Res.string.permission_description;
import static rikka.sui.manager.res.Res.string.permission_hidden;
import static rikka.sui.manager.res.Res.string.permission_warning_template;

public class Strings {

    private static final HashMap<String, String[]> STRINGS = new HashMap<>();

    static {
        String[] array;

        // Permission related texts can be found in packages/apps/PermissionController/res/values-[locale]/strings.xml

        array = new String[COUNT];
        array[permission_warning_template] = "要允许<b>%1$s</b>%2$s吗？";
        array[permission_description] = "拥有设备的完全访问权限";
        array[grant_dialog_button_allow_always] = "始终允许";
        array[grant_dialog_button_allow_one_time] = "仅限这一次";
        array[grant_dialog_button_deny] = "拒绝";
        array[grant_dialog_button_deny_and_dont_ask_again] = "拒绝，不要再询问";
        array[brackets_format] = "%1$s（%2$s）";
        array[management_title] = "超级用户管理";
        array[close] = "关闭";
        array[permission_allowed] = "允许";
        array[permission_denied] = "拒绝";
        array[permission_hidden] = "隐藏";
        array[permission_ask] = "询问";
        Strings.STRINGS.put("zh-CN", array);

        array = new String[COUNT];
        array[permission_warning_template] = "要允許「%1$s」%2$s嗎？";
        array[permission_description] = "擁有裝置的 Root 權限";
        array[grant_dialog_button_allow_always] = "一律允許";
        array[grant_dialog_button_allow_one_time] = "僅允許這一次";
        array[grant_dialog_button_deny] = "拒絕";
        array[grant_dialog_button_deny_and_dont_ask_again] = "拒絕且不要再詢問";
        array[brackets_format] = "%1$s（%2$s）";
        array[management_title] = "超級使用者管理";
        array[close] = "關閉";
        array[permission_allowed] = "允許";
        array[permission_denied] = "拒絕";
        array[permission_hidden] = "隱藏";
        array[permission_ask] = "詢問";
        Strings.STRINGS.put("zh", array);

        array = new String[COUNT];
        array[permission_warning_template] = "Allow <b>%1$s</b> to %2$s?";
        array[permission_description] = "have the full access of the device";
        array[grant_dialog_button_allow_always] = "Allow all the time";
        array[grant_dialog_button_allow_one_time] = "Only this time";
        array[grant_dialog_button_deny] = "Deny";
        array[grant_dialog_button_deny_and_dont_ask_again] = "Deny, don't ask again";
        array[brackets_format] = "%1$s (%2$s)";
        array[management_title] = "Superuser management";
        array[close] = "Close";
        array[permission_allowed] = "Allowed";
        array[permission_denied] = "Denied";
        array[permission_hidden] = "Hidden";
        array[permission_ask] = "Ask";
        Strings.STRINGS.put("en", array);
    }

    public static String get(int res) {
        return getDefaultString(res, Locale.getDefault());
    }

    private static String[] getStringsMap(Locale locale) {
        String language = locale.getLanguage();
        String region = locale.getCountry();

        // fully match
        locale = new Locale(language, region);
        for (String l : STRINGS.keySet()) {
            if (locale.toString().equals(l.replace('-', '_'))) {
                return STRINGS.get(l);
            }
        }

        // match language only keys
        locale = new Locale(language);
        for (String l : STRINGS.keySet()) {
            if (locale.toString().equals(l)) {
                return STRINGS.get(l);
            }
        }

        // match a language_region with only language
        for (String l : STRINGS.keySet()) {
            if (l.startsWith(locale.toString())) {
                return STRINGS.get(l);
            }
        }

        if (STRINGS.containsKey("en")) {
            return STRINGS.get("en");
        }
        throw new NullPointerException();
    }

    private static String getDefaultString(int res, Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();

            if (locale == null)
                locale = Locale.ENGLISH;
        }

        return getStringsMap(locale)[res];
    }

}