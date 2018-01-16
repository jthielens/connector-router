package com.cleo.labs.connector.router;

import java.io.IOException;
import java.io.InputStream;

/**
 * Parses an EDI file or stream (X12, EDIFACT, and TRADACOMS) into individual
 * segments containing elements and subelements. Refer to
 * {@link EDIFilterInputStream#getEDI()}.
 */
public class EDI {
    public enum Type {
        X12, EDIFACT, TRADACOMS, FUZZYX12, UNKNOWN;
        public boolean isX12() {
            return this.equals(X12) || this.equals(FUZZYX12);
        }
        public boolean isEDIFACT() {
            return this.equals(EDIFACT);
        }
        public boolean isTRADACOMS() {
            return this.equals(TRADACOMS);
        }
    };

    private static final int BUFFER_SIZE = 4096;

    InputStream in;
    byte[] bytes = new byte[BUFFER_SIZE]; // data byte buffer
    int len = 0; // current length of data bytes
    int index = 0; // current index into data bytes
    boolean eof = false;

    Type type = Type.UNKNOWN;
    boolean redetect = false;

    char elementSeparator = '*';
    char subelementSeparator = '*';
    byte segmentTerminator = '\r';
    boolean escape = false;
    char escapeCharacter;

    private EDISegment currentInterchange;
    private EDISegment currentFunctionalGroup;
    private EDISegment currentTransactionSet;

    /**
     * Constructs EDI parser for an input stream
     * 
     * @param in
     *            the EDI input stream
     */
    public EDI(InputStream in) throws IOException {
        this.in = in;
        detect();
    }

    private void detect() throws IOException {
        if (!eof)
            read();
        if (this.index < 0)
            return;
        if (ignoreExtra(this.bytes, this.index, 106, true).string.startsWith("ISA")) {
            this.type = checkX12();
        } else if (ignoreExtra(this.bytes, this.index, 9, true).string.startsWith("UNA")
                || ignoreExtra(this.bytes, this.index, 4, false).string.startsWith("UNB+")) {
            this.type = checkEDIFACT();
        } else if (ignoreExtra(this.bytes, this.index, 7, false).string.startsWith("STX=")) {
            this.type = checkTRADACOMS();
        } else {
            this.type = Type.UNKNOWN;
        }
        redetect = false;
    }

    private static final int[] ISA_LENGTHS = new int[] {3, 2, 10, 2, 10, 2, 15, 2, 15, 6, 4, 1, 5, 9, 1, 1};
    private static final int ISA06 = 6; // Sender ID
    private static final int ISA08 = 8; // Receiver ID

    private Type checkX12() {
        Characters isa = ignoreExtra(this.bytes, index, 106, true);
        char elementSeparator = isa.string.charAt(3);
        Type result = Type.X12;
        // ISA 00 01 02 03 04 05 06 07 08 09 10 11 12 13 14 15
        // len  3  2 10  2 10  2 15  2 15  6  4  1  5  9  1  1
        // notes: ISA00 is "ISA" itself
        //        ISA16 is the subelement separator and we tolerate this being the element separator, so we don't check
        //        we are also tolerating short ISA06 and ISA08
        int pointer = 0;
        for (int i = 0; i < ISA_LENGTHS.length; i++) {
            int len = ISA_LENGTHS[i];
            int next = isa.string.indexOf(elementSeparator, pointer);
            if (next < 0) {
                result = Type.UNKNOWN;
                break;
            } else if (next-pointer != len) {
                if (next-pointer < len) { // short value
                    if (i==ISA06 || i==ISA08) {
                        result = Type.FUZZYX12;
                    } else {
                        type = Type.UNKNOWN;
                        break;
                    }
                }
            }
            pointer += (next-pointer)+1;
        }

        if (result.isX12()) {
            if (pointer < 104) {
                // re-parse the ISA to properly handle the segmentTerminator (in case it's CR/LF)
                isa = ignoreExtra(this.bytes, index, pointer+2, true);
            }
            this.elementSeparator = elementSeparator;
            subelementSeparator = isa.string.charAt(pointer); // usually this is 104
            segmentTerminator = isa.bytes[pointer+1];  // usually this is 105
            escape = false;
        }
        return result;
    }

    private Type checkEDIFACT() {
        Type result = Type.EDIFACT;
        // defaults if no UNA
        char elementSeparator = '+';
        char subelementSeparator = ':';
        char escapeCharacter = '?';
        byte segmentTerminator = '\'';

        // make sure starts with UNA then UNB or just UNB
        int off = index;
        Characters chars = ignoreExtra(bytes, index, 9, true);
        if (chars.string.startsWith("UNA")) {
            elementSeparator = chars.string.charAt(4);
            subelementSeparator = chars.string.charAt(3);
            escapeCharacter = chars.string.charAt(6);
            segmentTerminator = chars.bytes[8];
            off = index + 9 + chars.skipped; // UNA is exactly 9 characters
            // check for extra end-of-line terminators
            while (off < bytes.length && isExtra(bytes[off])) {
                off++;
            }
        }

        if (!ignoreExtra(bytes, off, 4, false).string.equals("UNB" + elementSeparator)) {
            result = Type.UNKNOWN;
        } else {
            // make sure find a segment terminator and make
            // sure not too few or too many element separators
            int count = 0;
            for (int i = off; i < bytes.length; i++) {
                if (bytes[i] == elementSeparator) {
                    count++;
                } else if (bytes[i] == segmentTerminator) {
                    if (count >= 5 && count <= 11) {
                        // ok
                    } else {
                        result = Type.UNKNOWN;
                    }
                }
            }
        }

        if (result == Type.EDIFACT) {
            this.elementSeparator = elementSeparator;
            this.subelementSeparator = subelementSeparator;
            this.escapeCharacter = escapeCharacter;
            this.segmentTerminator = segmentTerminator;
            escape = true;
            escapeCharacter = '?';
        }
        return result;
    }

    private Type checkTRADACOMS() {
        Type result = Type.TRADACOMS;
        // defaults
        char elementSeparator = '+';
        byte segmentTerminator = '\'';

        // make sure find a segment terminator and make
        // sure not too few or too many element separators
        int count = 0;
        for (int i = index; i < bytes.length; i++) {
            if (bytes[i] == elementSeparator) {
                count++;
            } else if (bytes[i] == segmentTerminator) {
                if (count >= 4 && count <= 8) {
                    // ok
                } else {
                    result = Type.UNKNOWN;
                }
            }
        }

        if (this.type == Type.TRADACOMS) {
            // default values for TRADACOMS
            this.elementSeparator = '+';
            subelementSeparator = ':';
            this.segmentTerminator = segmentTerminator;
            escape = true;
            escapeCharacter = '?';
        }

        return result;
    }

    /**
     * Returns the detected EDI type
     * 
     * @return either X12, EDIFACT, TRADACOMS, or UNKNOWN
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Returns the EDI element separator
     * 
     * @return the EDI element separator
     */
    public char getElementSeparator() {
        return elementSeparator;
    }

    /**
     * Returns the EDI subelement separator
     * 
     * @return the Edi subelement separator
     */
    public char getSubelementSeparator() {
        return subelementSeparator;
    }

    /**
     * Returns the EDI segment terminator
     * 
     * @return the EDI segment terminator
     */
    public byte getSegmentTerminator() {
        return segmentTerminator;
    }

    /**
     * Returns the EDI escape character
     * 
     * @return the EDI escape character
     */
    public char getEscapeCharacter() {
        return escapeCharacter;
    }

    /**
     * Get the next EDI segment
     * 
     * @return the next EDI segment
     */
    public EDISegment getNextSegment() throws IOException {
        /*------------------------------------------------------------------------------
         *  If we're at end-of-file and have already emptied our buffer
         *----------------------------------------------------------------------------*/
        if (this.eof && this.index == -1)
            return null;

        /*------------------------------------------------------------------------------
         *  If we're at the end of our buffer, then go ahead and read more
         *----------------------------------------------------------------------------*/
        else if (this.index == this.len)
            read();

        if (redetect)
            detect();

        while (true) {
            /*------------------------------------------------------------------------------
             *    If ISA or UNA segment, then pull out the data delimiter values
             *----------------------------------------------------------------------------*
             ** moved this logic to detect() **
            if (this.type == Type.X12) {
                Characters chars = ignoreExtra(this.bytes, this.index, 106, true);
                if (chars.string.startsWith("ISA")) {
                    elementSeparator = chars.string.charAt(3);
                    subelementSeparator = chars.string.charAt(104);
                    segmentTerminator = chars.bytes[105];
                }

            } else if (this.type == Type.EDIFACT) {
                Characters chars = ignoreExtra(this.bytes, this.index, 9, true);
                if (chars.string.startsWith("UNA")) {
                    subelementSeparator = chars.string.charAt(3);
                    elementSeparator = chars.string.charAt(4);
                    escapeCharacter = chars.string.charAt(6);
                    segmentTerminator = chars.bytes[8];
                }
            }
             *----------------------------------------------------------------------------*/

            /*------------------------------------------------------------------------------
             *    Now walk through the bytes looking for a segment terminator
             *----------------------------------------------------------------------------*/
            for (int i = this.index; i < this.len; i++) {
                if (this.bytes[i] == segmentTerminator
                        && (i == this.index || !escape || this.bytes[i - 1] != escapeCharacter)) {
                    int offset = this.index;
                    int seglen = i - this.index;
                    /*------------------------------------------------------------------------------
                     *        Now check if there are extra end-of-line characters at the end of the
                     *        segment.  These need to be pulled into the segment object, but aren't
                     *        actually part of the element data.
                     *----------------------------------------------------------------------------*/
                    int extra = 1; // there's always at least one extra
                                   // character at the end - the segment
                                   // terminator
                    int workIndex = i + 1;
                    // as long as we keep finding more EOL characters, keep
                    // incrementing
                    while (workIndex < this.len && isExtra(bytes[workIndex])) {
                        extra++;
                        workIndex++;
                        i++;
                    }
                    // if we found a non-EOL character, go ahead and capture
                    // this segment
                    if (workIndex < this.len) {
                        this.index = workIndex; // update the byte index for the
                                                // next call
                        return processSegment(new EDISegment(this, bytes, offset, seglen, extra, segmentTerminator,
                                elementSeparator, subelementSeparator, escape, escapeCharacter));
                    } else {
                        break;
                    }
                }
            }

            /*------------------------------------------------------------------------------
             *    If we're at EOF, may need to capture one more segment
             *----------------------------------------------------------------------------*/
            if (this.eof) {
                if (this.index >= 0) {
                    int offset = this.index;
                    int seglen = this.len - this.index;

                    // count segment terminator and EOL characters at end of
                    // buffer as extra
                    int extra = 0;
                    int workIndex = this.len - 1;
                    while (workIndex >= this.index
                            && (bytes[workIndex] == segmentTerminator || isExtra(bytes[workIndex]))) {
                        extra++;
                        seglen--;
                        workIndex--;
                    }

                    this.index = -1;
                    return processSegment(new EDISegment(this, bytes, offset, seglen, extra, segmentTerminator,
                            elementSeparator, subelementSeparator, escape, escapeCharacter));

                } else {
                    return null;
                }
                /*------------------------------------------------------------------------------
                 *    Else if not at EOF, then go ahead and read more
                 *----------------------------------------------------------------------------*/
            } else {
                read();
            }
        }
    }

    private EDISegment processSegment(EDISegment segment) throws IOException {
        if (getType().isX12()) {
            if (segment.getName().equals("ISA")) {
                currentInterchange = segment;
                currentFunctionalGroup = null;
                currentTransactionSet = null;
            } else if (segment.getName().equals("GS")) {
                currentFunctionalGroup = segment;
                currentTransactionSet = null;
            } else if (segment.getName().equals("ST")) {
                currentTransactionSet = segment;
                // if end of interchange, check if another EDI type follows
            } else if (segment.getName().equals("IEA")) {
                redetect = true;
            }
        } else if (getType().isEDIFACT()) {
            if (segment.getName().equals("UNB")) {
                currentInterchange = segment;
                currentFunctionalGroup = null;
                currentTransactionSet = null;
            } else if (segment.getName().equals("UNG")) {
                currentFunctionalGroup = segment;
                currentTransactionSet = null;
            } else if (segment.getName().equals("UNH")) {
                currentTransactionSet = segment;
                // if end of interchange, check if another EDI type follows
            } else if (segment.getName().equals("UNZ")) {
                redetect = true;
            }
        } else if (getType().isTRADACOMS()) {
            if (segment.getName().equals("STX")) {
                currentInterchange = segment;
                currentFunctionalGroup = null;
                currentTransactionSet = null;
            } else if (segment.getName().equals("BAT")) {
                currentFunctionalGroup = segment;
                currentTransactionSet = null;
            } else if (segment.getName().equals("MHD")) {
                currentTransactionSet = segment;
                // if end of interchange, check if another EDI type follows
            } else if (segment.getName().equals("END")) {
                redetect = true;
            }
        }
        segment.setInterchange(currentInterchange);
        segment.setFunctionalGroup(currentFunctionalGroup);
        segment.setTransactionSet(currentTransactionSet);
        return segment;
    }

    private boolean isExtra(byte character) {
        return !Character.isLetterOrDigit((char) character);
    }

    // ignore CR/LF characters within the segment
    // if keepLast=true, then very last character can be a CR/LF (segment
    // terminator)
    private Characters ignoreExtra(byte[] bytes, int off, int len, boolean keepLast) {
        Characters chars = new Characters(len);
        int index = 0;
        for (int i = off; i < bytes.length; i++) {
            // this check may eventually need to include more than just CR/LF...
            if ((bytes[i] == '\r' || bytes[i] == '\n') && (index < len - 1 || !keepLast))
                chars.skipped++;
            else {
                chars.bytes[index] = bytes[i];
                index++;
            }
            if (index == len) {
                chars.string = new String(chars.bytes);
                break;
            }
        }
        return chars;
    }

    private static class Characters {
        int skipped = 0;
        byte[] bytes;
        String string = "";

        private Characters(int len) {
            this.bytes = new byte[len];
        }
    }

    /*------------------------------------------------------------------------------
     * Read from the input stream, and combine the remaining bytes with the newly
     * read bytes in the data byte buffer
     *----------------------------------------------------------------------------*/
    private void read() throws IOException {
        if (!eof) {
            byte[] remainingBytes = new byte[this.len - this.index];
            System.arraycopy(this.bytes, this.index, remainingBytes, 0, this.len - this.index);

            int len = this.bytes.length;
            if (len > BUFFER_SIZE)
                len = BUFFER_SIZE;
            int off = 0;
            this.len = 0;
            while (len > 0) {
                int read = in.read(this.bytes, off, len);
                if (read == -1) {
                    this.in.close();
                    this.eof = true;
                    break;
                } else {
                    this.len += read;
                    off += read;
                    len -= read;
                }
            }
            if (remainingBytes.length > 0) {
                byte[] combinedBytes = new byte[remainingBytes.length + this.len];
                System.arraycopy(remainingBytes, 0, combinedBytes, 0, remainingBytes.length);
                if (this.len > 0) {
                    System.arraycopy(this.bytes, 0, combinedBytes, remainingBytes.length, this.len);
                    this.len += remainingBytes.length;
                } else
                    this.len = remainingBytes.length;
                this.bytes = combinedBytes;
            }
            this.index = 0;
        } else
            this.index = -1;
    }

    /**
     * Closes the EDI input stream
     */
    public void close() throws IOException {
        in.close();
    }
}
