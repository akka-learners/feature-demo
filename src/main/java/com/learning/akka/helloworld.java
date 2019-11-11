package com.learning.akka;

import static akka.dispatch.Futures.*;
import static akka.pattern.Patterns.retry;
import static com.learning.akka.future.FutureActor.messageReady;
import static com.learning.akka.worker.actor.WorkerApi.Start;
import static scala.concurrent.Future.traverse;

import akka.actor.*;
import akka.dispatch.*;
import akka.japi.Function;
import akka.japi.Function2;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.learning.akka.future.FutureActor;
import com.learning.akka.worker.actor.Listener;
import com.learning.akka.worker.actor.Worker;
import com.sun.org.apache.regexp.internal.REUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import scala.concurrent.*;
import scala.concurrent.Future;
import scala.reflect.ClassTag;
import scala.reflect.ManifestFactory;
import scala.util.Try;


public class helloworld {

    /**
     * Runs the sample
     */
    public static void main(String[] args) throws Exception {
        Config config = ConfigFactory.parseString("akka.loglevel = INFO \n" +
            "akka.actor.debug.lifecycle = off");

        //ActorSystem system = ActorSystem.create("FaultToleranceSample", config);
        ActorSystem system = ActorSystem.create("FutureActorSystem");
        //syncFutureDemo(system);
//        getMessageFromPipeDemo(system);
        // futureSimpleDemo(system);
        // successfulResultPrint(system);
//        futureMapDemo(system);

        //IterableDemo(system);
//        foldDemo(system);
//        reduceDemo(system);
        //reduceDemo(system);
//        execWithOrder(system);
//        auxiliaryMethods(system);
//        zipDemo(system);
//        exceptionDemo(system);

        recoverDemo(system);
    }


    static void syncFutureDemo(ActorSystem system) throws Exception {
        Timeout timeout = Timeout.create(Duration.ofSeconds(5));
        ActorRef futureActor = system.actorOf(Props.create(FutureActor.class), "futureActor");

        Future<Object> future = Patterns.ask(futureActor, messageReady, timeout);
        String result = (String) Await.result(future, timeout.duration());
        System.out.println(result);
    }

    static void getMessageFromPipeDemo(ActorSystem system) {
        Future<String> f =
            future(
                new Callable<String>() {
                    public String call() {
                        return "Hello" + "World";
                    }
                },
                system.dispatcher());

        f.onComplete(r -> {
            System.out.println("get message from pipe " + r.get());
            return true;
        }, system.dispatcher());
        System.out.println("future is complete " + f.isCompleted());
        while (true) {
            if (f.isCompleted()) {
                System.out.println("future is complete from while " + f.isCompleted());
                break;
            }
        }

    }

    static void futureSimpleDemo(ActorSystem system) {
        Future<String> future = Futures.successful("Yay!");
        printHelper(system, future);
//        Future<Object> failed = Futures.failed(new RuntimeException("I am failed"));
//        failed.onComplete(r -> {
//            System.out.println(r.isSuccess());
//            System.out.println(r.get());
//            return false;
//        }, system.getDispatcher());
    }

    static void promiseAndFutureConvert(ActorSystem system) {
        Promise<Object> promise = Futures.promise();
        Future<Object> objectFuture = promise.future();
        objectFuture.onComplete(r -> {
            System.out.println(r.get());
            return true;
        }, system.getDispatcher());
        promise.success("I am promise");
    }

    static void futureMapDemo(ActorSystem system) {
        Future<String> future = Futures.successful("Yay!");
        Future<String> map = future.map(item -> item.replace("Y", "map"), system.getDispatcher());
        printHelper(system, map);
    }

    static void IterableDemo(ActorSystem system) {
        Future<List<String>> listFuture = Future.successful(Arrays.asList("1", "2", "3"));
        Future<Integer> future1 = Future.successful(1);
        Future<Integer> future2 = Future.successful(1);
        Future<Integer> future3 = Future.successful(1);
        List<Future<Integer>> futures = Arrays.asList(future1, future1, future3);
        Future<Iterable<Integer>> sequence = sequence(futures, system.getDispatcher());
        Future<Integer> integerMapFuture = sequence.map(items -> {
            final Result result = new Result(0);
            items.forEach(item -> {
                result.sum += item;
            });
            return result.sum;
        }, system.getDispatcher());
        printHelper(system, integerMapFuture);
    }


    static void TraverseDemo(ActorSystem system) {
        Iterable<String> listStrings = Arrays.asList("a", "b", "c");
    }

    static void foldDemo(ActorSystem system) {

        Future<Integer> future1 = Future.successful(1);
        Future<Integer> future2 = Future.successful(1);
        Future<Integer> future3 = Future.successful(1);
        List<Future<Integer>> futures = Arrays.asList(future1, future1, future3);

        Future<Integer> fold = fold(
            10,
            futures,
            (arg1, arg2) -> arg1 + arg2,
            system.getDispatcher());
        printHelper(system, fold);
    }

    static void reduceDemo(ActorSystem system) {

        Future<Integer> future1 = Future.successful(1);
        Future<Integer> future2 = Future.successful(1);
        Future<Integer> future3 = Future.successful(1);
        List<Future<Integer>> futures = Arrays.asList(future1, future2, future3);

        Future<Integer> reduce = reduce(
            futures,
            (result, item) -> result + item,
            system.getDispatcher());
        printHelper(system, reduce);
    }

    static void execWithOrder(ActorSystem system) {
        final ExecutionContext ec = system.dispatcher();
        Future<String> future1 =
            Futures.successful("value")
                .andThen(
                    new OnComplete<String>() {
                        public void onComplete(Throwable failure, String result) {
                            System.out.println("first order " + result);
                        }
                    },
                    ec)
                .andThen(
                    new OnComplete<String>() {
                        public void onComplete(Throwable failure, String result) {
                            System.out.println("second order " + result);

                        }
                    },
                    ec);

        printHelper(system, future1);
    }

    static void auxiliaryMethods(ActorSystem system) {
        Future<String> future1 = Futures.failed(new IllegalStateException("OHNOES1"));
        Future<String> future2 = Futures.failed(new IllegalStateException("OHNOES2"));
//        Future<String> future3 = Futures.successful("bar");
        Future<String> future3 = Futures.failed(new RuntimeException("error"));
// Will have "bar" in this case
        Future<String> future4 = future1.fallbackTo(future2).fallbackTo(future3);
        printHelper(system, future4);
    }


    static void zipDemo(ActorSystem system) {
        Future<String> future1 = Futures.successful("foo");
        Future<String> future2 = Futures.successful("bar");
        Future<String> future3 =
            future1
                .zip(future2)
                .map(tuple -> tuple._1 + tuple._2,
                    system.dispatcher());
        printHelper(system, future3);
    }

    static void exceptionDemo(ActorSystem system) {
        Future<Integer> future =
            future(
                () -> 1 / 0,
                system.getDispatcher())
                .recover(
                    new Recover<Integer>() {
                        public Integer recover(Throwable problem) throws Throwable {
                            if (problem instanceof ArithmeticException) {
                                return 1000;
                            } else {
                                throw problem;
                            }
                        }
                    },
                    system.getDispatcher());
        printHelper(system, future);
    }


    static void recoverDemo(ActorSystem system) {
        Future<Integer> future = future(
            () -> 1 / 0,
            system.getDispatcher()
        ).recoverWith(
            new Recover<Future<Integer>>() {
                public Future<Integer> recover(Throwable problem) throws Throwable {
                    if (problem instanceof ArithmeticException) {
                        return future(
                            () -> 0,
                            system.getDispatcher());
                    } else {
                        throw problem;
                    }
                }
            },
            system.getDispatcher());
        printHelper(system, future);
    }


    static void retryDemo(ActorSystem system) {
        Callable<CompletionStage<String>> attempt = () -> CompletableFuture.completedFuture("test");
        CompletionStage<String> retriedFuture =
            retry(attempt, 3, java.time.Duration.ofMillis(200), system.scheduler(), system.getDispatcher());
    }

    static class Result {

        public Integer sum;

        public Result(int i) {
            sum = i;
        }
    }

    private static <T> void printHelper(ActorSystem system, Future<T> future) {
        future.onComplete(r -> {
            System.out.println(r.isSuccess());
            System.out.println(r.get());
            return false;
        }, system.getDispatcher());
    }
}