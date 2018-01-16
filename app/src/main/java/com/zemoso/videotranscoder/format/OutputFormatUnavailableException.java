package com.zemoso.videotranscoder.format;

/**
 * @author Bhupathi
 *         Created on 12/1/18.
 */

public class OutputFormatUnavailableException extends RuntimeException {
    public OutputFormatUnavailableException(String detailMessage) {
        super(detailMessage);
    }
}