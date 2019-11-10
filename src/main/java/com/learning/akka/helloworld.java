package com.learning.akka;

import static com.learning.akka.worker.actor.WorkerApi.Start;

import akka.actor.*;
import com.learning.akka.worker.actor.Listener;
import com.learning.akka.worker.actor.Worker;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;


public class helloworld {

    /**
     * Runs the sample
     */
    public static void main(String[] args) {
        Config config = ConfigFactory.parseString("akka.loglevel = DEBUG \n" +
            "akka.actor.debug.lifecycle = on");

        //ActorSystem system = ActorSystem.create("FaultToleranceSample", config);
        ActorSystem system = ActorSystem.create("FaultToleranceSample");
        ActorRef worker = system.actorOf(Props.create(Worker.class), "worker");
        ActorRef listener = system.actorOf(Props.create(Listener.class), "listener");
        // start the work and listen on progress
        // note that the listener is used as sender of the tell,
        // i.e. it will receive replies from the worker
        worker.tell(Start, listener);
    }
}