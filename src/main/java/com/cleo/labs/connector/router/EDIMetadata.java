package com.cleo.labs.connector.router;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cleo.lexicom.edi.EDI;
import com.cleo.lexicom.edi.EDIElement;
import com.cleo.lexicom.edi.EDISegment;
import com.google.common.base.Strings;

/**
 * Collects EDI Metadata as it is parsed from the EDI envelope.  The following
 * elements are supported:
 * <table>
 *   <tr><th>Element                     </th><th>X21</th><th>EDIFACT</th><th>TRADCOMS</th></tr>
 *   <tr><td>sender [qualifier:]id       </td><td>IS05:ISA06</td><td>UNB02.2:UNB02.1</td><td>STX02:1</td></tr>
 *   <tr><td>receiver [qualifier:]id     </td><td>IS07:ISA08</td><td>UNB03.2:UNB03.1</td><td>STX03:1</td></tr>
 *   <tr><td>groupSender [qualifier:]id  </td><td>GS02      </td><td>UNG02.2:UNG02.1</td><td>       </td></tr>
 *   <tr><td>groupReceiver [qualifier:]id</td><td>GS03      </td><td>UNG03.2:UNG03.1</td><td>       </td></tr>
 *   <tr><td>function                    </td><td>GS01      </td><td>UNG01          </td><td>       </td></tr>
 *   <tr><td>type                        </td><td>ST01      </td><td>UNH09:1        </td><td>MHD02  </td></tr>
 *   <tr><td>icn                         </td><td>ISA13     </td><td>UNB05:1        </td><td>STX05:1</td></tr>
 * </table>
 */
public class EDIMetadata {

    private EDI edi;

    private EDIID sender = new EDIID();
    private EDIID receiver = new EDIID();
    private EDIID groupSender = new EDIID();
    private EDIID groupReceiver = new EDIID();
    private String function;
    private String type;
    private String icn;
    private boolean typed;

    /**
     *  Resets all metadata fields to {@code null}.
     */
    public void reset() {
        sender = new EDIID();
        receiver = new EDIID();
        groupSender = new EDIID();
        groupReceiver = new EDIID();
        function = null;
        type = null;
        icn = null;
        typed = false;
    }

    /**
     * Constructs a new {@code EDIMetadata} object based on
     * an {@code EDI} parsing object.
     * @param edi the {@code EDI} object
     * @throws IOException
     */
    public EDIMetadata(EDI edi) {
        this.edi = edi;
        reset();
    }

    /**
     * Attempts to extract the value of named capture group
     * {@code name} from a {@link Matcher}, returning {@code null}
     * instead of throwing an exception in case the named group
     * is not defined in the underlying {@link Pattern}.
     * @param m the matched {@link Matcher}
     * @param name the capture group name
     * @return the captured text, or {@code null} if the group was not defined
     */
    private static String group(Matcher m, String name) {
        try {
            return m.group(name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Constructs a new {@code EDIMetadata} object based on
     * a non-EDI parse represented in a matched {@link Matcher}.
     * The following named capture groups are supported (note
     * that the names are case sensitive):
     * <ul><li>sender</li>
     *     <li>receiver</li>
     *     <li>groupSender</li>
     *     <li>groupReceiver</li>
     *     <li>function</li>
     *     <li>type</li>
     *     <li>icn</li></ul>
     * @param m the matched {@link Matcher}.
     */
    public EDIMetadata(Matcher m) {
        sender.id(group(m, "sender"));
        receiver.id(group(m, "receiver"));
        groupSender.id(group(m, "groupSender"));
        groupReceiver.id(group(m, "groupReceiver"));
        function = group(m, "function");
        type = group(m, "type");
        icn = group(m, "icn");
    }

    /**
     * Safely gets subelement 0 for element {@code index} from {@code segment},
     * returning {@code ""} if there are not that many elements.  If the
     * element has subelements, returns the first subelement.
     * avoiding an Exception.  Return values are {@link String#trim()}ed.
     * @param segment the {@code EDISegment} from which to extract the element
     * @param index which (0-relative) element to extract
     * @return the element, or {@code ""} if it does not exist
     */
    private static String getSubelementOrNot(EDISegment segment, int index) {
        if (segment.getElementCount() > index) {
            EDIElement e = segment.getElement(index);
            if (e.getSubelementCount() > 0) {
                return e.getSubelement(0).trim();
            } else {
                return e.getElement().trim();
            }
        } else {
            return "";
        }
    }

    /**
     * Safely gets element {@code index} from {@code segment},
     * returning {@code ""} if there are not that many elements,
     * avoiding an Exception.  Return values are {@link String#trim()}ed.
     * @param segment the {@code EDISegment} from which to extract the element
     * @param index which (0-relative) element to extract
     * @return the element, or {@code ""} if it does not exist
     */
    private static String getElement(EDISegment segment, int index) {
        try {
            return segment.getElement(index).getElement().trim();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /**
     * Processes an X12 segment, parsing supported metadata values.
     * Sets {@code typed} to {@code true} when the end of the header
     * sequence is parsed (the {@code ST}).
     * @param edisegment the {@code EDISegment} to parse
     */
    private void processX12(EDISegment edisegment) {
        if (edisegment.getName().equals("ISA")) { // interchange header
            sender.id(getElement(edisegment, 5))
                    .qualifier(getElement(edisegment, 4));
            receiver.id(getElement(edisegment, 7))
                    .qualifier(getElement(edisegment, 6));
            icn = getElement(edisegment, 12);
        } else if (edisegment.getName().equals("GS")) { // functional group header
            function = getElement(edisegment, 0);
            groupSender.id(getElement(edisegment, 1));
            groupReceiver.id(getElement(edisegment, 2));
        } else if (edisegment.getName().equals("ST")) { // transaction set header
            type = getElement(edisegment, 0);
            typed = true;
        }
    }

    /**
     * Processes an EDIFACT segment, parsing supported metadata values.
     * Sets {@code typed} to {@code true} when the end of the header
     * sequence is parsed (the {@code UNH}).
     * @param edisegment the {@code EDISegment} to parse
     */
    private void processEdifact(EDISegment edisegment) {
        if (edisegment.getName().startsWith("UNA")) { // optional data delimiter header
        } else if (edisegment.getName().equals("UNB")) { // interchange header
            sender.fromEdifact(edisegment, 1);
            receiver.fromEdifact(edisegment, 2);
            icn = getSubelementOrNot(edisegment, 4);
        } else if (edisegment.getName().equals("UNG")) { // functional group header
            function = getSubelementOrNot(edisegment, 0);
            groupSender.fromEdifact(edisegment, 1);
            groupReceiver.fromEdifact(edisegment, 2);
        } else if (edisegment.getName().equals("UNH")) { // message header
            type = getSubelementOrNot(edisegment, 1);
            typed = true;
        }
    }

    /**
     * Processes a TRADACOMS segment, parsing supported metadata values.
     * Sets {@code typed} to {@code true} when the end of the header
     * sequence is parsed (the {@code MHD}).
     * @param edisegment the {@code EDISegment} to parse
     */
    private void processTradacoms(EDISegment edisegment) {
        if (edisegment.getName().equals("STX")) { // interchange header
            sender.id(getSubelementOrNot(edisegment, 1));
            receiver.id(getSubelementOrNot(edisegment, 2));
            icn = getSubelementOrNot(edisegment, 4);
        } else if (edisegment.getName().equals("BAT")) { // functional group header
        } else if (edisegment.getName().equals("MHD")) { // message header
            type = getSubelementOrNot(edisegment, 1);
            typed = true;
        }
    }

    /**
     * Processes an EDI segment, dispatching the parsing to the syntax-specific
     * parsing routines above.
     * @param edisegment the {@code EDISegment} to parse
     */
    public void process(EDISegment edisegment) {
        if (edi.getType() == EDI.X12) {
            processX12(edisegment);
        } else if (edi.getType() == EDI.EDIFACT) {
            processEdifact(edisegment);
        } else if (edi.getType() == EDI.TRADACOMS) {
            processTradacoms(edisegment);
        }
    }

    /**
     * Returns {@code true} if an {@code EDISegment} is the closing
     * segment of an interchange, depending on syntax.
     * @param segment the {Ecode EDISegment} to parse
     * @return {@code true} if the segment is a closing segment
     */
    public boolean isLastSegment(EDISegment segment) {
        switch (edi.getType()) {
        case EDI.X12:
            return segment.getName().equals("IEA");
        case EDI.EDIFACT:
            return segment.getName().equals("UNZ");
        case EDI.TRADACOMS:
            return segment.getName().equals("END");
        default:
            return false;
        }
    }

    /**
     * EDIMetadata getter for the sender.
     * @return the sender
     */
    public EDIID sender() {
        return sender;
    }
    /**
     * EDIMetadata getter for the receiver.
     * @return the receiver
     */
    public EDIID receiver() {
        return receiver;
    }
    /**
     * EDIMetadata getter for the groupSender.
     * @return the groupSender
     */
    public EDIID groupSender() {
        return groupSender;
    }
    /**
     * EDIMetadata getter for the groupReceiver.
     * @return the groupReceiver
     */
    public EDIID groupReceiver() {
        return groupReceiver;
    }
    /**
     * EDIMetadata getter for the function.
     * @return the function
     */
    public String function() {
        return function;
    }
    /**
     * EDIMetadata getter for the type.
     * @return the type
     */
    public String type() {
        return type;
    }
    /**
     * EDIMetadata getter for the icn.
     * @return the icn
     */
    public String icn() {
        return icn;
    }
    /**
     * Gets the {@code typed} flag indicating completion
     * of the envelope parsing.
     * @return {@code true} if all headers have been parsed
     */
    public boolean typed() {
        return typed;
    }

    /**
     * Internal helper method equivalent to {@link String#matches(String)}
     * with {@code null}-proofing all around.  {@code null} patterns always
     * match, and {@code null} metadata values are the same as {@code ""}.
     * @param metadata the possibly {@code null} metadata value to match
     * @param match the possibly {@code null} pattern to match against
     * @return {@code true} if there is a match
     */
    private static boolean matches(String metadata, String match) {
        return Strings.isNullOrEmpty(match)
                || Strings.nullToEmpty(metadata).matches(match);
    }
    /**
     * Compares all metadata values against their counterpart patterns
     * in a {@link Route}, returning {@code true} if all match.
     * @param route the {@link Route} to match against
     * @return {@code true} if all metadata values match
     */
    public boolean matches(Route route) {
        return ((edi!=null) == Strings.isNullOrEmpty(route.content()))  // edi MUST NOT have content, non-edi MUST have content
                && sender().matches(route.senderQualifier(), route.sender())
                && receiver().matches(route.receiverQualifier(), route.receiver())
                && groupSender().matches(route.groupSenderQualifier(), route.groupSender())
                && groupReceiver().matches(route.groupReceiverQualifier(), route.groupReceiver())
                && matches(function(), route.function())
                && matches(type(), route.type());
    }

    /**
     * Returns {@code} true if all metadata values are {@code null}
     * or empty.
     * @return {@code true} if the metadata is empty
     */
    public boolean isEmpty() {
        return sender.isEmpty()
                && receiver.isEmpty()
                && groupSender.isEmpty()
                && groupReceiver.isEmpty()
                && Strings.isNullOrEmpty(function)
                && Strings.isNullOrEmpty(type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!sender.isEmpty()) {
            sb.append("sender=").append(sender.toString()).append(' ');
        }
        if (!receiver.isEmpty()) {
            sb.append("receiver=").append(receiver.toString()).append(' ');
        }
        if (!groupSender.isEmpty()) {
            sb.append("groupSender=").append(groupSender.toString()).append(' ');
        }
        if (!groupReceiver.isEmpty()) {
            sb.append("groupReceiver=").append(groupReceiver.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(function)) {
            sb.append("function=").append(function).append(' ');
        }
        if (!Strings.isNullOrEmpty(type)) {
            sb.append("type=").append(type).append(' ');
        }
        if (!Strings.isNullOrEmpty(icn)) {
            sb.append("icn=").append(icn).append(' ');
        }
        if (sb.length()>0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
}
