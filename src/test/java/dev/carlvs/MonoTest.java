package dev.carlvs;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.ReactorBlockHoundIntegration;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Slf4j
/**
 * Reactive Streams
 * 1. Asynchronous
 * 2. Non-Blocking
 * 3. Backpressure
 * Publisher <- (subscribe) Subscriber
 * Subscription is created
 * Publisher (onSubsribe with the subscription) -> Subcriber
 * Subscription <- (request N) Subscriber
 * Publisher ->  (onNext) Subscriber
 * until:
 * 1. publisher send all the objects requested.
 * 2. Publish send all the objects it has. (onComplete) subscriber and subscription will be canceled
 * 3. There is an error. (onError) -> subscriber and subscription will be canceled
 */
public class MonoTest {

    @BeforeAll
    public static void setUp() {
        BlockHound.install();
    }

    @Test
    public void blockHoundWorks() {
        try {
            FutureTask<?> task = new FutureTask<>(() -> {
                Thread.sleep(0);
                return "";
            });
            Schedulers.parallel().schedule(task);

            task.get(10, TimeUnit.SECONDS);
            Assertions.fail("should fail");
        } catch (Exception e) {
            Assertions.assertTrue(e.getCause() instanceof BlockingOperationError);
        }
    }
    
    @Test
    public void monoSubscriber(){
        String name = "Carlos";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe();

        log.info("-------------------------------");
        
        StepVerifier.create(mono)
            .expectNext(name)
            .verifyComplete();
    }

    @Test
    public void subscribeConsumer(){
        String name = "Carlos";
        Mono<String> mono = Mono.just(name).log();

        mono.subscribe(s -> log.info("Value {}", s));

        log.info("-------------------------------");
        
        StepVerifier.create(mono)
            .expectNext(name)
            .verifyComplete();
    }

    @Test
    public void subscribeConsumerError(){
        String name = "Carlos";
        Mono<String> mono = Mono
                            .just(name)
                            .map(s -> {throw new RuntimeException("Testing Mono with error");});

        

        mono.subscribe(s -> log.info("Value {}", s), s -> log.error("Somenthing bad happened"));
        mono.subscribe(s -> log.info("Value {}", s), Throwable::printStackTrace);

        log.info("-------------------------------");
        
        StepVerifier.create(mono)
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    public void subscribeConsumerComplete(){
        String name = "Carlos";
        Mono<String> mono = Mono
                            .just(name)
                            .log()
                            .map(s -> s.toUpperCase());

        mono.subscribe(s -> log.info("Value {}", s), 
                                    Throwable::printStackTrace, 
                                    () -> log.info("FINISHED"));

        log.info("-------------------------------");
        
        StepVerifier.create(mono)
            .expectNext(name.toUpperCase())
            .verifyComplete();
    }

    @Test
    public void subscribeConsumerSubscription(){
        String name = "Carlos";
        Mono<String> mono = Mono
                            .just(name)
                            .log()
                            .map(s -> s.toUpperCase());

        mono.subscribe(s -> log.info("Value {}", s), 
                                    Throwable::printStackTrace, 
                                    () -> log.info("FINISHED"),
                                    subscription -> subscription.request(1));

        log.info("-------------------------------");

        StepVerifier.create(mono)
            .expectNext(name.toUpperCase())
            .verifyComplete();
    }

    @Test
    public void monoDoOnMethod(){
        String name = "Carlos";
        Mono<String> mono = Mono
                            .just(name)
                            .log()
                            .map(String::toUpperCase)
                            .doOnSubscribe(subscription -> log.info("Subscribed"))
                            .doOnRequest(longNumber -> log.info("Request Received, starting somenthing..."))
                            //.doOnNext(s -> log.info("Value is here. executing doOnNext {}", s))
                            //.flaMap(s -> Mono.empty()) Kill mono value and line doOnNext dont printed why value is empty
                            .doOnNext(s -> log.info("Value is here. executing doOnNext {}", s))
                            .doOnSuccess(s -> log.info("doOnSucess executed"));

        mono.subscribe(s -> log.info("Value {}", s), 
                                    Throwable::printStackTrace, 
                                    () -> log.info("FINISHED"));

        log.info("-------------------------------");
        
        StepVerifier.create(mono)
            .expectNext(name.toUpperCase())
            .verifyComplete();
    }

    @Test
    public void monoDoOnError(){
        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal Argument Exception"))
            .doOnError(e -> log.error("Error message {}", e.getMessage())).log();
        
        StepVerifier.create(error)
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    public void monoDoOnErrorResume(){
        String name = "Carlos Return in OoErrorResume";

        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal Argument Exception"))
            .onErrorResume(s -> {log.info("Executhing this onErrorResume"); //in onErrorResume this is executed when as error
                            return Mono.just(name);})
            .doOnError(e -> MonoTest.log.error("Error message  {}", e.getMessage())) //This not executed why onResumeError are 'get' the ErrorException and here dont have error  
            .log();
        
        StepVerifier.create(error) //This is executed why onResumeError are 'get' the ErrorException and here dont have error
            .expectNext(name)
            .verifyComplete();
    }

    @Test
    public void monoDoOnErrorReturn(){
        String name = "Carlos Return in OoErrorResume";

        Mono<Object> error = Mono.error(new IllegalArgumentException("Illegal Argument Exception"))
            .onErrorReturn("EMPTY")
            .onErrorResume(s -> {log.info("Executhing this onErrorResume"); //This not executed why onErrorReturn are 'get' the ErrorException and here in this point dont have error
                            return Mono.just(name);})
            .doOnError(e -> MonoTest.log.error("Error message  {}", e.getMessage())) //This not executed why onErrorReturn are 'get' the ErrorException and here in this point dont have error
            .log();
        
        StepVerifier.create(error) //This is executed why onErrorReturn are 'get' the ErrorException and here in this point dont have error
            .expectNext("EMPTY")
            .verifyComplete();
    }
}
