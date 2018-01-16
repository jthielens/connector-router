package com.cleo.labs.connector.router;

import java.util.ArrayList;
import java.util.List;

public class EDISegment {
    private EDI.Type type;
    private byte[] segment;

    private String name;
    private EDIElement[] elements;

    private EDISegment interchange;
    private EDISegment functionalGroup;
    private EDISegment transactionSet;

    protected EDISegment(EDI edi, byte[] segment, int offset, int len, int extra, byte segmentTerminator,
            char elementSeparator, char subelementSeparator, boolean escape, char escapeCharacter) {
        this.type = edi.getType();
        /*------------------------------------------------------------------------------
         *  Save the original segment bytes
         *----------------------------------------------------------------------------*/
        this.segment = new byte[len + extra];
        System.arraycopy(segment, offset, this.segment, 0, len + extra);

        /*------------------------------------------------------------------------------
         *  Now parse out the name and elements
         *----------------------------------------------------------------------------*/
        List<String> list = new ArrayList<>();
        int index = 0;
        for (int i = 0; i <= len; i++) {
            /*------------------------------------------------------------------------------
             *    At the end of an element if
             *      - At the end of the segment or
             *      - At an element separator and
             *          At the beginning of the segment or
             *          No escape character supported or
             *          Escape character doesn't precede element separator
             *----------------------------------------------------------------------------*/
            if (i == len || (this.segment[i] == elementSeparator
                    && (i == 0 || !escape || this.segment[i - 1] != escapeCharacter))) {
                String string = ignoreExtra(escape(new String(this.segment, index, i - index), segmentTerminator,
                        elementSeparator, escape, escapeCharacter));
                /*------------------------------------------------------------------------------
                 *      First item in a segment is always the segment name
                 *----------------------------------------------------------------------------*/
                if (this.name == null) {
                    /*------------------------------------------------------------------------------
                     *        For Tradacoms, first item is name=segment
                     *----------------------------------------------------------------------------*/
                    if (edi.getType() == EDI.Type.TRADACOMS) {
                        int equal = string.indexOf("=");
                        if (equal >= 0) {
                            this.name = string.substring(0, equal);
                            list.add(string.substring(equal + 1));
                        } else {
                            this.name = string;
                        }
                    } else {
                        this.name = string;
                    }
                } else {
                    list.add(string);
                }
                index = i + 1;
            }
        }
        this.elements = new EDIElement[list.size()];
        for (int i = 0; i < elements.length; i++)
            this.elements[i] = new EDIElement((String) list.get(i), subelementSeparator, escape, escapeCharacter);
    }

    private String ignoreExtra(String string) {
        if (string == null)
            return string;
        StringBuffer sb = new StringBuffer(string);
        for (int i = sb.length() - 1; i >= 0; i--) {
            // this check may eventually need to include more than just CR/LF
            // and non-ASCII
            if (sb.charAt(i) == '\r' || sb.charAt(i) == '\n' || sb.charAt(i) >= 128)
                sb.deleteCharAt(i);
        }
        return sb.toString();
    }

    protected void setInterchange(EDISegment interchange) {
        this.interchange = interchange;
    }

    protected void setFunctionalGroup(EDISegment functionalGroup) {
        this.functionalGroup = functionalGroup;
    }

    protected void setTransactionSet(EDISegment transactionSet) {
        this.transactionSet = transactionSet;
    }

    /**
     * Returns the EDI type
     * 
     * @return either EDI.Type.X12, EDI.Type.EDIFACT, or EDI.Type.TRADACOMS
     */
    public EDI.Type getType() {
        return this.type;
    }

    /**
     * Returns the EDI segment's parent interchange segment
     * 
     * @return EDI interchange segment
     */
    public EDISegment getInterchange() {
        return this.interchange;
    }

    /**
     * Returns the EDI segment's parent functional group segment
     * 
     * @return EDI functional group segment
     */
    public EDISegment getFunctionalGroup() {
        return this.functionalGroup;
    }

    /**
     * Returns the EDI segment's parent transaction set segment
     * 
     * @return EDI transaction set segment
     */
    public EDISegment getTransactionSet() {
        return this.transactionSet;
    }

    /*------------------------------------------------------------------------------
     * Pull out escape characters
     *----------------------------------------------------------------------------*/
    static protected String escape(String string, int delimiter1, char delimiter2, boolean escape, char escapeCharacter) {
        if (!escape) {
            return string;
        }
        for (int i = 0; i < string.length(); i++) {
            if (i < string.length() - 1 && string.charAt(i) == escapeCharacter
                    && (string.charAt(i + 1) == delimiter1 || string.charAt(i + 1) == delimiter2)) {
                string = string.substring(0, i) + string.substring(i + 1);
            }
        }
        return string;
    }

    /**
     * Returns the EDI segment's original unparsed string
     * 
     * @return Unparsed EDI segment
     */
    public byte[] getSegment() {
        return this.segment;
    }

    /**
     * Returns the EDI segment name
     * 
     * @return EDI segment name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the parsed EDI element array
     * 
     * @return EDI element array
     */
    public EDIElement[] getElements() {
        return this.elements;
    }

    /**
     * Returns the parsed EDI element count
     * 
     * @return EDI element count
     */
    public int getElementCount() {
        return this.elements.length;
    }

    /**
     * Returns the requested, parsed EDI element
     * 
     * @return EDI element
     */
    public EDIElement getElement(int index) {
        return this.elements[index];
    }

    /*------------------------------------------------------------------------------
     * For optional reference number and additional reference number logging
     *----------------------------------------------------------------------------*/
    private String reference1;

    public void setReference1(String reference1) {
        this.reference1 = reference1;
    }

    public String getReference1() {
        return this.reference1;
    }

    private String reference2;

    public void setReference2(String reference2) {
        this.reference2 = reference2;
    }

    public String getReference2() {
        return this.reference2;
    }

    /*------------------------------------------------------------------------------
     * For optional functional acknowledgment tracking
     *----------------------------------------------------------------------------*/
    private List<FunctionalAcknowledgment> functionalAcknowledgments = new ArrayList<>();

    public FunctionalAcknowledgment newFunctionalAcknowledgment() {
        return new FunctionalAcknowledgment();
    }

    public void addFunctionalAcknowledgment(FunctionalAcknowledgment ack) {
        functionalAcknowledgments.add(ack);
    }

    public FunctionalAcknowledgment[] getFunctionalAcknowledgment() {
        return functionalAcknowledgments.toArray(new FunctionalAcknowledgment[functionalAcknowledgments.size()]);
    }

    public class FunctionalAcknowledgment {
        private String iSender;
        private String iSenderQualifier = "";
        private String iReceiver;
        private String iReceiverQualifier = "";
        private String iControlNum;
        private String refControlNum;
        private String status;

        public FunctionalAcknowledgment() {
        }

        public void setISender(String iSender) {
            this.iSender = iSender;
        }

        public void setISenderQualifier(String iSenderQualifier) {
            this.iSenderQualifier = iSenderQualifier;
        }

        public void setIReceiver(String iReceiver) {
            this.iReceiver = iReceiver;
        }

        public void setIReceiverQualifier(String iReceiverQualifier) {
            this.iReceiverQualifier = iReceiverQualifier;
        }

        public void setIControlNum(String iControlNum) {
            this.iControlNum = iControlNum;
        }

        public void setRefControlNum(String refControlNum) {
            this.refControlNum = refControlNum;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getISender() {
            return this.iSender;
        }

        public String getISenderQualifier() {
            return this.iSenderQualifier;
        }

        public String getIReceiver() {
            return this.iReceiver;
        }

        public String getIReceiverQualifier() {
            return this.iReceiverQualifier;
        }

        public String getIControlNum() {
            return this.iControlNum;
        }

        public String getRefControlNum() {
            return this.refControlNum;
        }

        public String getStatus() {
            return this.status;
        }

        private List<FunctionalAcknowledgment> transactionSetAcknowledgments = new ArrayList<>();

        public void addTransactionSetAcknowledgment(FunctionalAcknowledgment ack) {
            transactionSetAcknowledgments.add(ack);
        }

        public FunctionalAcknowledgment[] getTransactionSetAcknowledgment() {
            return transactionSetAcknowledgments.toArray(new FunctionalAcknowledgment[transactionSetAcknowledgments.size()]);
        }
    }

}
