package com.cleo.labs.connector.router;

public class Route {
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
    private String type;
    private String destination;

    public Route() {
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
        this.type = null;
        this.destination = null;
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
}
