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
import scala.concurrent.duration.Duration;


/**
 * com.learning.akka.Worker performs some work when it receives the Start message. It will continuously notify the sender of the Start message of current Progress. The com.learning.akka.Worker
 * supervise the CounterService.
 */
public class Worker extends UntypedActor {

    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    final Timeout askTimeout = new Timeout(Duration.create(5, "seconds"));

    // The sender of the initial Start message will continuously be notified
    // about progress
    ActorRef progressListener;
    final ActorRef counterService = getContext().actorOf(
        Props.create(CounterService.class), "counter");
    final int totalCount = 51;

    // Stop the CounterService child if it throws ServiceUnavailable
    private static SupervisorStrategy strategy = new OneForOneStrategy(-1,
        Duration.Inf(), new Function<Throwable, SupervisorStrategy.Directive>() {
        @Override
        public SupervisorStrategy.Directive apply(Throwable t) {
            if (t instanceof CounterServiceApi.ServiceUnavailable) {
                return stop();
            } else {
                return escalate();
            }
        }
    });

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    public void onReceive(Object msg) {
        log.debug("received message {}", msg);
        if (msg.equals(Start) && progressListener == null) {
            progressListener = getSender();
            getContext().system().scheduler().schedule(
                Duration.Zero(), Duration.create(1, "second"), getSelf(), Do,
                getContext().dispatcher(), null
            );
        } else if (msg.equals(Do)) {
            counterService.tell(new CounterServiceApi.Increment(1), getSelf());
            counterService.tell(new CounterServiceApi.Increment(1), getSelf());
            counterService.tell(new CounterServiceApi.Increment(1), getSelf());

            // Send current progress to the initial sender
            pipe(ask(counterService, GetCurrentCount, askTimeout)
                .mapTo(classTag(CounterServiceApi.CurrentCount.class))
                .map(new Mapper<CounterServiceApi.CurrentCount, WorkerApi.Progress>() {
                    public WorkerApi.Progress apply(CounterServiceApi.CurrentCount c) {
                        return new WorkerApi.Progress(100.0 * c.count / totalCount);
                    }
                }, getContext().dispatcher()), getContext().dispatcher())
                .to(progressListener);
        } else {
            unhandled(msg);
        }
    }
}
