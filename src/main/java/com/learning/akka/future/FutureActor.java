package com.learning.akka.future;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class FutureActor extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    public static String messageReady = "Message_ready";
    private static String messageReadyPipe = "Message_ready_pipe";
    public static String resultMessage = "this is result message";
    private static String resultMessagePipe = "this is result message pipe";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .matchEquals(messageReady, this::handleMessageReady)
            .matchEquals(messageReadyPipe, this::handleMessageReadyPipe)
            .build();
    }

    private void handleMessageReadyPipe(String messageReadyPipe) {

    }

    private void handleMessageReady(String message) {
        log.info("ready message {}", message);
        getSender().tell(resultMessage, getSelf());
    }
}
