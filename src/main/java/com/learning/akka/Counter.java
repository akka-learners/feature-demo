package com.learning.akka;

import static com.learning.akka.CounterServiceApi.GetCurrentCount;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * The in memory count variable that will send current value to the Storage,
 * if there is any storage available at the moment.
 */
public class Counter extends UntypedActor {
    final LoggingAdapter log = Logging.getLogger(getContext().system(), this);
    final String key;
    long count;
    ActorRef storage;

    public Counter(String key, long initialValue) {
        this.key = key;
        this.count = initialValue;
    }

    @Override
    public void onReceive(Object msg) {
        log.debug("received message {}", msg);
        if (msg instanceof CounterApi.UseStorage) {
            storage = ((CounterApi.UseStorage) msg).storage;
            storeCount();
        } else if (msg instanceof CounterServiceApi.Increment) {
            count += ((CounterServiceApi.Increment) msg).n;
            storeCount();
        } else if (msg.equals(GetCurrentCount)) {
            getSender().tell(new CounterServiceApi.CurrentCount(key, count), getSelf());
        } else {
            unhandled(msg);
        }
    }

    void storeCount() {
        // Delegate dangerous work, to protect our valuable state.
        // We can continue without storage.
        if (storage != null) {
            storage.tell(new StorageApi.Store(new StorageApi.Entry(key, count)), getSelf());
        }
    }
}
