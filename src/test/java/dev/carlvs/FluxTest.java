package dev.carlvs;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import lombok.extern.slf4j.Slf4j;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@Slf4j
public class FluxTest {

    @BeforeAll
    public static void setUp() {
        BlockHound.install();
    }
    
    @Test
    public void fluxSubscriber(){
        Flux<String> fluxString = Flux.just("Carlos", "Eduardo", "hns")
                                        .log();

        StepVerifier.create(fluxString)
                    .expectNext("Carlos", "Eduardo", "hns")
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbers(){
        Flux<Integer> fluxNumber = Flux.range(1, 3).log();

        fluxNumber.subscribe(i -> log.info("Number {}", i));

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3)
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberFromList(){
        Flux<Integer> fluxNumber = Flux.fromIterable(List.of(1, 2 , 3)).log();

        fluxNumber.subscribe(i -> log.info("Number {}", i));

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3)
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersError(){
        Flux<Integer> fluxNumber = Flux.range(1, 5)
                                        .log()
                                        .map(i -> {
                                            if(i == 4){
                                                throw new IndexOutOfBoundsException("Index error");
                                            }
                                            return i;
                                        });

        fluxNumber.subscribe(i -> log.info("Number {}", i), 
                                    Throwable::printStackTrace,
                                    () -> log.info("DONE"),
                                    subscription -> subscription.request(3));

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3)
                    .expectError(IndexOutOfBoundsException.class)
                    .verify();
    }

    @Test
    public void fluxSubscriberNumbersUglyBackpressure(){
        Flux<Integer> fluxNumber = Flux.range(1, 10)
                                        .log();

        fluxNumber.subscribe(new Subscriber<Integer>(){

            private int count = 0;
            private Subscription subscription;
            private final int requestCount = 2;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(requestCount);
            }

            @Override
            public void onComplete() {
                
            }

            @Override
            public void onError(Throwable throwable) {
                
            }

            @Override
            public void onNext(Integer onNext) {
                count++;
                if(count >= requestCount){
                    count = 0;
                    subscription.request(requestCount);
                }
            }    
        });

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3, 4, 5, 6, 7,8 , 9, 10 )
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersNotSoUglyBackpressure(){
        Flux<Integer> fluxNumber = Flux.range(1, 10)
                                        .log();

        fluxNumber.subscribe(new BaseSubscriber<Integer>(){

            private int count = 0;
            private final int requestCount = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestCount);
            }
              
            @Override
            protected void hookOnNext(Integer value) {
                count++;
                if(count >= requestCount){
                    count = 0;
                    request(requestCount);
                }
            }
        });

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3, 4, 5, 6, 7,8 , 9, 10 )
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberPrettyBackpressure(){
        Flux<Integer> fluxNumber = Flux.range(1, 10)                                     
                                        .log()
                                        .limitRate(3);

        fluxNumber.subscribe(i -> log.info("Number {}", i));

        log.info("----------------------------------------");

        StepVerifier.create(fluxNumber)
                    .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    .verifyComplete();
    }

    @Test
    public void fluxSubscriberIntervalOne() throws InterruptedException{
        Flux<Long> interval = Flux.interval(Duration.ofMillis(100))
                                    .take(4)
                                    .log();

        interval.subscribe(i -> log.info("Number {}", i));

        Thread.sleep(300);
    }

    @Test
    public void fluxSubscriberIntervalTwo() throws InterruptedException{

        StepVerifier.withVirtualTime(this::createInterval)
                    .expectSubscription()
                    .expectNoEvent(Duration.ofDays(1)) //Para ter certeza de que nada foi publicado antes de tempo
                    .thenAwait(Duration.ofDays(1))
                    .expectNext(0L)
                    .thenAwait(Duration.ofDays(1))
                    .expectNext(1L)
                    .thenCancel()
                    .verify();
    }

    private Flux<Long> createInterval(){
        return Flux.interval(Duration.ofDays(1))
                    .take(4)
                    .log();
    }

    @Test
    public void connectionsFlux() throws InterruptedException{
        ConnectableFlux<Integer> connectableFlux = Flux.range(1, 10)
            .log()
            //.delayElements(Duration.ofMillis(100))
            .publish();

        /*connectableFlux.connect();

        log.info("thread sleeping fir 200ms");

        Thread.sleep(200);

        connectableFlux.subscribe(i -> log.info("Sub1 number {}", i));

        log.info("thread sleeping fir 200ms");

        Thread.sleep(200);

        connectableFlux.subscribe(i -> log.info("Sub2 number {}", i));*/

        StepVerifier.create(connectableFlux)
                    .then(connectableFlux::connect)
                    .thenConsumeWhile(i -> i <= 5)
                    .expectNext(6, 7, 8, 9, 10)
                    .expectComplete()
                    .verify();
    }

    @Test
    public void connectionsFluxAutoConnect() throws InterruptedException{
        Flux<Integer> fluxAutoConnect = Flux.range(1, 5)
            .log()
            .delayElements(Duration.ofMillis(100))
            .publish()
            .autoConnect(2);


        StepVerifier.create(fluxAutoConnect)
                    .then(fluxAutoConnect::subscribe)
                    .expectNext(1, 2, 3, 4, 5)
                    .expectComplete()
                    .verify();
    }
}
