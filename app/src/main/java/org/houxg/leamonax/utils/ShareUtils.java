package org.houxg.leamonax.utils;

import android.content.Intent;
import android.text.TextUtils;

import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.EXTRA_SUBJECT;
import static android.content.Intent.EXTRA_TEXT;


/**
 * Utilities for creating a share intent
 */
public class ShareUtils {

    /**
     * Create intent with subject and body
     *
     * @param subject
     * @param body
     * @return intent
     */
    public static Intent create(final CharSequence subject,
                                final CharSequence body) {
        Intent intent = new Intent(ACTION_SEND);
        intent.setType("text/plain");
        if (!TextUtils.isEmpty(subject))
            intent.putExtra(EXTRA_SUBJECT, subject);
        intent.putExtra(EXTRA_TEXT, body);
        return intent;
    }

    /**
     * Get body from intent
     *
     * @param intent
     * @return body
     */
    public static String getBody(final Intent intent) {
        return intent != null ? intent.getStringExtra(EXTRA_TEXT) : "";
    }

    /**
     * Get subject from intent
     *
     * @param intent
     * @return subject
     */
    public static String getSubject(final Intent intent) {
        String result;
        result = intent != null ? intent.getStringExtra(EXTRA_SUBJECT) : null;
        if (result == null) {
            result = getBody(intent);
            result = result.substring(0, Math.min(result.length(), 20));
        }
        return result;
    }
}