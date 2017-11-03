package com.cleo.labs.connector.router;

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
public class Metadata {
    protected EDIID sender = new EDIID();
    protected EDIID receiver = new EDIID();
    protected EDIID groupSender = new EDIID();
    protected EDIID groupReceiver = new EDIID();
    protected String function;
    protected String type;
    protected String icn;

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
    }

    /**
     * Don't ever construct one of these directly.  Either construct
     * a {@link RoutableContent.ContentMetadata} or use {@link #getEDIMetadata(EDI)}.
     */
    protected Metadata() {
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
        return sender().matches(route.senderQualifier(), route.sender())
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
