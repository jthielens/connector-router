package com.cleo.labs.connector.router;

import java.io.IOException;
import java.util.regex.Matcher;

import com.cleo.lexicom.edi.EDI;
import com.cleo.lexicom.edi.EDIElement;
import com.cleo.lexicom.edi.EDISegment;
import com.google.gwt.thirdparty.guava.common.base.Strings;

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

    public EDIMetadata(EDI edi) throws IOException {
        this.edi = edi;
        reset();
    }

    private static String group(Matcher m, String name) {
        try {
            return m.group(name);
        } catch (Exception e) {
            return null;
        }
    }
    public EDIMetadata(Matcher m) {
        sender.id(group(m, "sender"));
        receiver.id(group(m, "receiver"));
        groupSender.id(group(m, "groupSender"));
        groupReceiver.id(group(m, "groupReceiver"));
        function = group(m, "function");
        type = group(m, "type");
        icn = group(m, "icn");
    }

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

    private static String getElement(EDISegment segment, int index) {
        try {
            return segment.getElement(index).getElement().trim();
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    private void processX12(EDISegment edisegment) throws IOException {
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

    private void processEdifact(EDISegment edisegment) throws IOException {
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

    private void processTradacoms(EDISegment edisegment) throws IOException {
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

    public void process(EDISegment edisegment) throws IOException {
        if (edi.getType() == EDI.X12) {
            processX12(edisegment);
        } else if (edi.getType() == EDI.EDIFACT) {
            processEdifact(edisegment);
        } else if (edi.getType() == EDI.TRADACOMS) {
            processTradacoms(edisegment);
        }
    }

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

    public EDIID sender() {
        return sender;
    }
    public EDIID receiver() {
        return receiver;
    }
    public EDIID groupSender() {
        return groupSender;
    }
    public EDIID groupReceiver() {
        return groupReceiver;
    }
    public String function() {
        return function;
    }
    public String type() {
        return type;
    }
    public String icn() {
        return icn;
    }
    public boolean typed() {
        return typed;
    }

    private static boolean matches(String metadata, String match) {
        return Strings.isNullOrEmpty(match)
                || Strings.nullToEmpty(metadata).matches(match);
    }
    private static boolean matches(EDIID id, String match, String qualifierMatch) {
        return (Strings.isNullOrEmpty(match) || Strings.nullToEmpty(id.id()).matches(match))
                && (Strings.isNullOrEmpty(qualifierMatch) || Strings.nullToEmpty(id.qualifier()).matches(qualifierMatch));
    }
    public boolean matches(Route route) {
        return Strings.isNullOrEmpty(route.content())
                && matches(sender(), route.sender(), route.senderQualifier())
                && matches(receiver(), route.receiver(), route.receiverQualifier())
                && matches(groupSender(), route.groupSender(), route.groupSenderQualifier())
                && matches(groupReceiver(), route.groupReceiver(), route.groupReceiverQualifier())
                && matches(function(), route.function())
                && matches(type(), route.type());
    }

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
