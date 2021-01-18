/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package rikka.sui.resource;

import android.text.Html;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.util.BitSet;

import rikka.sui.util.Preconditions;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class TextUtilsCompat {

    private static final String TAG = "TextUtilsCompat";

    // Zero-width character used to fill ellipsized strings when codepoint length must be preserved.
    /* package */ static final char ELLIPSIS_FILLER = '\uFEFF'; // ZERO WIDTH NO-BREAK SPACE

    // TODO: Based on CLDR data, these need to be localized for Dzongkha (dz) and perhaps
    // Hong Kong Traditional Chinese (zh-Hant-HK), but that may need to depend on the actual word
    // being ellipsized and not the locale.
    private static final String ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)
    private static final String ELLIPSIS_TWO_DOTS = "\u2025"; // TWO DOT LEADER (‥)

    private static final int LINE_FEED_CODE_POINT = 10;
    private static final int NBSP_CODE_POINT = 160;

    /**
     * Flags for {@link #makeSafeForPresentation(String, int, float, int)}
     */
    @Retention(SOURCE)
    @IntDef(flag = true,
            value = {SAFE_STRING_FLAG_TRIM, SAFE_STRING_FLAG_SINGLE_LINE,
                    SAFE_STRING_FLAG_FIRST_LINE})
    public @interface SafeStringFlags {
    }

    /**
     * Remove {@link Character#isWhitespace(int) whitespace} and non-breaking spaces from the edges
     * of the label.
     *
     * @see #makeSafeForPresentation(String, int, float, int)
     */
    public static final int SAFE_STRING_FLAG_TRIM = 0x1;

    /**
     * Force entire string into single line of text (no newlines). Cannot be set at the same time as
     * {@link #SAFE_STRING_FLAG_FIRST_LINE}.
     *
     * @see #makeSafeForPresentation(String, int, float, int)
     */
    public static final int SAFE_STRING_FLAG_SINGLE_LINE = 0x2;

    /**
     * Return only first line of text (truncate at first newline). Cannot be set at the same time as
     * {@link #SAFE_STRING_FLAG_SINGLE_LINE}.
     *
     * @see #makeSafeForPresentation(String, int, float, int)
     */
    public static final int SAFE_STRING_FLAG_FIRST_LINE = 0x4;

    private static boolean isNewline(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.PARAGRAPH_SEPARATOR || type == Character.LINE_SEPARATOR
                || codePoint == LINE_FEED_CODE_POINT;
    }

    private static boolean isWhiteSpace(int codePoint) {
        return Character.isWhitespace(codePoint) || codePoint == NBSP_CODE_POINT;
    }

    /**
     * Remove html, remove bad characters, and truncate string.
     *
     * <p>This method is meant to remove common mistakes and nefarious formatting from strings that
     * were loaded from untrusted sources (such as other packages).
     *
     * <p>This method first {@link Html#fromHtml treats the string like HTML} and then ...
     * <ul>
     * <li>Removes new lines or truncates at first new line
     * <li>Trims the white-space off the end
     * <li>Truncates the string
     * </ul>
     * ... if specified.
     *
     * @param unclean                 The input string
     * @param maxCharactersToConsider The maximum number of characters of {@code unclean} to
     *                                consider from the input string. {@code 0} disables this
     *                                feature.
     * @param ellipsizeDip            Assuming maximum length of the string (in dip), assuming font size 42.
     *                                This is roughly 50 characters for {@code ellipsizeDip == 1000}.<br />
     *                                Usually ellipsizing should be left to the view showing the string. If a
     *                                string is used as an input to another string, it might be useful to
     *                                control the length of the input string though. {@code 0} disables this
     *                                feature.
     * @param flags                   Flags controlling cleaning behavior (Can be {@link #SAFE_STRING_FLAG_TRIM},
     *                                {@link #SAFE_STRING_FLAG_SINGLE_LINE},
     *                                and {@link #SAFE_STRING_FLAG_FIRST_LINE})
     * @return The cleaned string
     */
    public static @NonNull
    CharSequence makeSafeForPresentation(@NonNull String unclean,
                                         @IntRange(from = 0) int maxCharactersToConsider,
                                         @FloatRange(from = 0) float ellipsizeDip, @SafeStringFlags int flags) {
        boolean onlyKeepFirstLine = ((flags & SAFE_STRING_FLAG_FIRST_LINE) != 0);
        boolean forceSingleLine = ((flags & SAFE_STRING_FLAG_SINGLE_LINE) != 0);
        boolean trim = ((flags & SAFE_STRING_FLAG_TRIM) != 0);

        Preconditions.checkNotNull(unclean);
        Preconditions.checkArgumentNonnegative(maxCharactersToConsider);
        Preconditions.checkArgumentNonNegative(ellipsizeDip, "ellipsizeDip");
        Preconditions.checkFlagsArgument(flags, SAFE_STRING_FLAG_TRIM
                | SAFE_STRING_FLAG_SINGLE_LINE | SAFE_STRING_FLAG_FIRST_LINE);
        Preconditions.checkArgument(!(onlyKeepFirstLine && forceSingleLine),
                "Cannot set SAFE_STRING_FLAG_SINGLE_LINE and SAFE_STRING_FLAG_FIRST_LINE at the"
                        + "same time");

        String shortString;
        if (maxCharactersToConsider > 0) {
            shortString = unclean.substring(0, Math.min(unclean.length(), maxCharactersToConsider));
        } else {
            shortString = unclean;
        }

        // Treat string as HTML. This
        // - converts HTML symbols: e.g. &szlig; -> ß
        // - applies some HTML tags: e.g. <br> -> \n
        // - removes invalid characters such as \b
        // - removes html styling, such as <b>
        // - applies html formatting: e.g. a<p>b</p>c -> a\n\nb\n\nc
        // - replaces some html tags by "object replacement" markers: <img> -> \ufffc
        // - Removes leading white space
        // - Removes all trailing white space beside a single space
        // - Collapses double white space
        StringWithRemovedChars gettingCleaned = new StringWithRemovedChars(
                Html.fromHtml(shortString).toString());

        int firstNonWhiteSpace = -1;
        int firstTrailingWhiteSpace = -1;

        // Remove new lines (if requested) and control characters.
        int uncleanLength = gettingCleaned.length();
        for (int offset = 0; offset < uncleanLength; ) {
            int codePoint = gettingCleaned.codePointAt(offset);
            int type = Character.getType(codePoint);
            int codePointLen = Character.charCount(codePoint);
            boolean isNewline = isNewline(codePoint);

            if (onlyKeepFirstLine && isNewline) {
                gettingCleaned.removeAllCharAfter(offset);
                break;
            } else if (forceSingleLine && isNewline) {
                gettingCleaned.removeRange(offset, offset + codePointLen);
            } else if (type == Character.CONTROL && !isNewline) {
                gettingCleaned.removeRange(offset, offset + codePointLen);
            } else if (trim && !isWhiteSpace(codePoint)) {
                // This is only executed if the code point is not removed
                if (firstNonWhiteSpace == -1) {
                    firstNonWhiteSpace = offset;
                }
                firstTrailingWhiteSpace = offset + codePointLen;
            }

            offset += codePointLen;
        }

        if (trim) {
            // Remove leading and trailing white space
            if (firstNonWhiteSpace == -1) {
                // No non whitespace found, remove all
                gettingCleaned.removeAllCharAfter(0);
            } else {
                if (firstNonWhiteSpace > 0) {
                    gettingCleaned.removeAllCharBefore(firstNonWhiteSpace);
                }
                if (firstTrailingWhiteSpace < uncleanLength) {
                    gettingCleaned.removeAllCharAfter(firstTrailingWhiteSpace);
                }
            }
        }

        if (ellipsizeDip == 0) {
            return gettingCleaned.toString();
        } else {
            // Truncate
            final TextPaint paint = new TextPaint();
            paint.setTextSize(42);

            return TextUtils.ellipsize(gettingCleaned.toString(), paint, ellipsizeDip,
                    TextUtils.TruncateAt.END);
        }
    }


    /**
     * A special string manipulation class. Just records removals and executes the when onString()
     * is called.
     */
    private static class StringWithRemovedChars {
        /**
         * The original string
         */
        private final String mOriginal;

        /**
         * One bit per char in string. If bit is set, character needs to be removed. If whole
         * bit field is not initialized nothing needs to be removed.
         */
        private BitSet mRemovedChars;

        StringWithRemovedChars(@NonNull String original) {
            mOriginal = original;
        }

        /**
         * Mark all chars in a range {@code [firstRemoved - firstNonRemoved[} (not including
         * firstNonRemoved) as removed.
         */
        void removeRange(int firstRemoved, int firstNonRemoved) {
            if (mRemovedChars == null) {
                mRemovedChars = new BitSet(mOriginal.length());
            }

            mRemovedChars.set(firstRemoved, firstNonRemoved);
        }

        /**
         * Remove all characters before {@code firstNonRemoved}.
         */
        void removeAllCharBefore(int firstNonRemoved) {
            if (mRemovedChars == null) {
                mRemovedChars = new BitSet(mOriginal.length());
            }

            mRemovedChars.set(0, firstNonRemoved);
        }

        /**
         * Remove all characters after and including {@code firstRemoved}.
         */
        void removeAllCharAfter(int firstRemoved) {
            if (mRemovedChars == null) {
                mRemovedChars = new BitSet(mOriginal.length());
            }

            mRemovedChars.set(firstRemoved, mOriginal.length());
        }

        @Override
        public String toString() {
            // Common case, no chars removed
            if (mRemovedChars == null) {
                return mOriginal;
            }

            StringBuilder sb = new StringBuilder(mOriginal.length());
            for (int i = 0; i < mOriginal.length(); i++) {
                if (!mRemovedChars.get(i)) {
                    sb.append(mOriginal.charAt(i));
                }
            }

            return sb.toString();
        }

        /**
         * Return length or the original string
         */
        int length() {
            return mOriginal.length();
        }

        /**
         * Return codePoint of original string at a certain {@code offset}
         */
        int codePointAt(int offset) {
            return mOriginal.codePointAt(offset);
        }
    }
}
