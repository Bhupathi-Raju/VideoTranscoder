package com.zemoso.videotranscoder.Engine;

/**
 * @author Bhupathi
 *         Created on 12/1/18.
 */

public class InvalidOutputFormatException extends RuntimeException {
    public InvalidOutputFormatException(String detailMessage) {
        super(detailMessage);
    }
}