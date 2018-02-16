package com.cleo.labs.connector.router;

import com.google.common.base.Strings;

public class Route {
    private boolean enabled;
    private String filename;
    private String content;
    private String sender;
    private String receiver;
    private String groupSender;
    private String groupReceiver;
    private String senderQualifier;
    private String receiverQualifier;
    private String groupSenderQualifier;
    private String groupReceiverQualifier;
    private String function;
    private String type;
    private String destination;

    public Route() {
        this.enabled = false;
        this.filename = null;
        this.content = null;
        this.sender = null;
        this.receiver = null;
        this.groupSender = null;
        this.groupReceiver = null;
        this.senderQualifier = null;
        this.receiverQualifier = null;
        this.groupSenderQualifier = null;
        this.groupReceiverQualifier = null;
        this.function = null;
        this.type = null;
        this.destination = null;
    }

    public boolean enabled() {
        return enabled;
    }
    public Route enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    public String filename() {
        return filename;
    }
    public Route filename(String filename) {
        this.filename = filename;
        return this;
    }
    public String content() {
        return content;
    }
    public Route content(String content) {
        this.content = content;
        return this;
    }
    public String sender() {
        return sender;
    }
    public Route sender(String sender) {
        this.sender = sender;
        return this;
    }
    public String receiver() {
        return receiver;
    }
    public Route receiver(String receiver) {
        this.receiver = receiver;
        return this;
    }
    public String groupSender() {
        return groupSender;
    }
    public Route groupSender(String groupSender) {
        this.groupSender = groupSender;
        return this;
    }
    public String groupReceiver() {
        return groupReceiver;
    }
    public Route groupReceiver(String groupReceiver) {
        this.groupReceiver = groupReceiver;
        return this;
    }
    public String senderQualifier() {
        return senderQualifier;
    }
    public Route senderQualifier(String senderQualifier) {
        this.senderQualifier = senderQualifier;
        return this;
    }
    public String receiverQualifier() {
        return receiverQualifier;
    }
    public Route receiverQualifier(String receiver) {
        this.receiver = receiver;
        return this;
    }
    public String groupSenderQualifier() {
        return groupSenderQualifier;
    }
    public Route groupSenderQualifier(String groupSenderQualifier) {
        this.groupSenderQualifier = groupSenderQualifier;
        return this;
    }
    public String groupReceiverQualifier() {
        return groupReceiverQualifier;
    }
    public Route groupReceiverQualifier(String groupReceiverQualifier) {
        this.groupReceiverQualifier = groupReceiverQualifier;
        return this;
    }
    public String function() {
        return function;
    }
    public Route function(String function) {
        this.function = function;
        return this;
    }
    public String type() {
        return type;
    }
    public Route type(String type) {
        this.type = type;
        return this;
    }
    public String destination() {
        return destination;
    }
    public Route destination(String destination) {
        this.destination = destination;
        return this;
    }

    /**
     * Returns {@code true} if this route matches anything, meaning
     * that it has no content or metadata-related matching patterns.
     * <p/>
     * Note that the {@code enabled} flag and any {@code filename}
     * matching patterns are not considered in this evaluation.
     * @return {@code true} is this route matches anything
     */
    public boolean matchesAnything() {
        return Strings.isNullOrEmpty(content) &&
                Strings.isNullOrEmpty(sender) &&
                Strings.isNullOrEmpty(receiver) &&
                Strings.isNullOrEmpty(groupSender) &&
                Strings.isNullOrEmpty(groupReceiver) &&
                Strings.isNullOrEmpty(senderQualifier) &&
                Strings.isNullOrEmpty(receiverQualifier) &&
                Strings.isNullOrEmpty(groupSenderQualifier) &&
                Strings.isNullOrEmpty(groupReceiverQualifier) &&
                Strings.isNullOrEmpty(function) &&
                Strings.isNullOrEmpty(type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!Strings.isNullOrEmpty(filename)) {
            sb.append("filename=").append(filename.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(content)) {
            sb.append("content=").append(content.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(sender)) {
            sb.append("sender=").append(sender.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(receiver)) {
            sb.append("receiver=").append(receiver.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(groupSender)) {
            sb.append("groupSender=").append(groupSender.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(groupReceiver)) {
            sb.append("groupReceiver=").append(groupReceiver.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(senderQualifier)) {
            sb.append("senderQualifier=").append(senderQualifier.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(receiverQualifier)) {
            sb.append("receiverQualifier=").append(receiverQualifier.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(groupSenderQualifier)) {
            sb.append("groupSenderQualifier=").append(groupSenderQualifier.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(groupReceiverQualifier)) {
            sb.append("groupReceiverQualifier=").append(groupReceiverQualifier.toString()).append(' ');
        }
        if (!Strings.isNullOrEmpty(function)) {
            sb.append("function=").append(function).append(' ');
        }
        if (!Strings.isNullOrEmpty(type)) {
            sb.append("type=").append(type).append(' ');
        }
        if (!Strings.isNullOrEmpty(destination)) {
            sb.append("destination=").append(destination).append(' ');
        }
        if (sb.length()>0) {
            sb.setLength(sb.length()-1);
        }
        return sb.toString();
    }
}
