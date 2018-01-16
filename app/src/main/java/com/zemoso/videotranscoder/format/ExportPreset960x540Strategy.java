package com.zemoso.videotranscoder.format;

/**
 * @author Bhupathi
 *         Created on 12/1/18.
 */

import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by yuya.tanaka on 2014/11/20.
 */
class ExportPreset960x540Strategy implements MediaFormatStrategy {
    private static final String TAG = "ExportPreset960x540Str";

    @Override
    public MediaFormat createVideoOutputFormat(MediaFormat inputFormat) {
        // TODO: detect non-baseline profile and throw exception
        int width = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int height = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        MediaFormat outputFormat = MediaFormatPresets.getExportPreset960x540(width, height);
        int outWidth = outputFormat.getInteger(MediaFormat.KEY_WIDTH);
        int outHeight = outputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        Log.d(TAG, String.format("inputFormat: %dx%d => outputFormat: %dx%d", width, height, outWidth, outHeight));
        return outputFormat;
    }

    @Override
    public MediaFormat createAudioOutputFormat(MediaFormat inputFormat) {
        // TODO
        return null;
    }
}