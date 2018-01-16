package com.zemoso.videotranscoder.utils;

/**
 * @author Bhupathi
 *         Created on 12/1/18.
 */

import java.nio.ByteBuffer;

public class AvcSpsUtils {
    public static byte getProfileIdc(ByteBuffer spsBuffer) {
        // Refer: http://www.cardinalpeak.com/blog/the-h-264-sequence-parameter-set/
        // First byte after NAL.
        return spsBuffer.get(0);
    }
}