package com.learning.akka;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.stop;
import static akka.japi.Util.classTag;
import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;
import static com.learning.akka.CounterServiceApi.GetCurrentCount;
import static com.learning.akka.WorkerApi.Do;
import static com.learning.akka.WorkerApi.Start;

import akka.actor.*;
import akka.dispatch.Mapper;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;


/**
 * com.learning.akka.Worker performs some work when it receives the Start message. It will continuously notify the sender of the Start message of current
 * Progress. The com.learning.akka.Worker supervise the CounterService.
 */
public class Worker extends AbstractActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    final Timeout askTimeout = new Timeout(Duration.create(5, "seconds"));

    // The sender of the initial Start message will continuously be notified
    // about progress
    ActorRef progressListener;
    ActorRef counterService;
    final int totalCount = 51;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        counterService = getContext().actorOf(Props.create(CounterServiceNew.class), "counter");
    }

    // Stop the CounterService child if it throws ServiceUnavailable
    private static SupervisorStrategy strategy = new OneForOneStrategy(-1,
        Duration.Inf(), t -> {
            if (t instanceof CounterServiceApi.ServiceUnavailable) {
                return stop();
            } else {
                return escalate();
            }
        });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .matchEquals(Start, this::handleStart)
            .matchEquals(Do, this::handleDo)
            .build();
    }

    private void handleStart(Object msg) {
        log.debug("received message {}", msg);
        if (progressListener == null) {
            progressListener = getSender();
            getContext().getSystem().scheduler()
                .schedule(Duration.Zero(),
                    Duration.create(1, TimeUnit.SECONDS),
                    getSelf(),
                    Do,
                    getContext().getDispatcher(),
                    null);
        } else {
            unhandled(msg);
        }
    }

    private void handleDo(Object msg) {
        log.debug("received message {}", msg);
        counterService.tell(new CounterServiceApi.Increment(1), getSelf());
        counterService.tell(new CounterServiceApi.Increment(1), getSelf());
        counterService.tell(new CounterServiceApi.Increment(1), getSelf());
        // Send current progress to the initial sender
        pipe(
            ask(counterService, GetCurrentCount, askTimeout)
                .mapTo(classTag(CounterServiceApi.CurrentCount.class))
                .map(new Mapper<CounterServiceApi.CurrentCount, WorkerApi.Progress>() {
                         public WorkerApi.Progress apply(CounterServiceApi.CurrentCount c) {
                             return new WorkerApi.Progress(100.0 * c.count / totalCount);
                         }
                     },
                    getContext().dispatcher()
                ),
            getContext().dispatcher()
        ).to(progressListener);
    }
}
