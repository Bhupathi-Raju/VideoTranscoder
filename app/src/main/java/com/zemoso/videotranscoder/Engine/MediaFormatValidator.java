package com.zemoso.videotranscoder.Engine;

/**
 * @author Bhupathi
 *         Created on 12/1/18.
 */
import android.media.MediaFormat;

import com.zemoso.videotranscoder.format.MediaFormatExtraConstants;
import com.zemoso.videotranscoder.utils.AvcCsdUtils;
import com.zemoso.videotranscoder.utils.AvcSpsUtils;

import java.nio.ByteBuffer;

class MediaFormatValidator {
    // Refer: http://en.wikipedia.org/wiki/H.264/MPEG-4_AVC#Profiles
    private static final byte PROFILE_IDC_BASELINE = 66;

    public static void validateVideoOutputFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        // Refer: http://developer.android.com/guide/appendix/media-formats.html#core
        // Refer: http://en.wikipedia.org/wiki/MPEG-4_Part_14#Data_streams
        if (!MediaFormatExtraConstants.MIMETYPE_VIDEO_AVC.equals(mime)) {
            throw new InvalidOutputFormatException("Video codecs other than AVC is not supported, actual mime type: " + mime);
        }
        ByteBuffer spsBuffer = AvcCsdUtils.getSpsBuffer(format);
        byte profileIdc = AvcSpsUtils.getProfileIdc(spsBuffer);
        if (profileIdc != PROFILE_IDC_BASELINE) {
            throw new InvalidOutputFormatException("Non-baseline AVC video profile is not supported by Android OS, actual profile_idc: " + profileIdc);
        }
    }

    public static void validateAudioOutputFormat(MediaFormat format) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (!MediaFormatExtraConstants.MIMETYPE_AUDIO_AAC.equals(mime)) {
            throw new InvalidOutputFormatException("Audio codecs other than AAC is not supported, actual mime type: " + mime);
        }
    }
}