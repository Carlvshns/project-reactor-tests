package dev.carlvs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

@Slf4j
public class OperatorsTest {

    @BeforeAll
    public static void setUp() {
        BlockHound.install(builder ->  builder.allowBlockingCallsInside("org.slf4j.impl.SimpleLogger", "write"));
    }

    @Test
    public void subscriberOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .subscribeOn(Schedulers.single())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void publisherOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .publishOn(Schedulers.boundedElastic())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void multipleSubscriberOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .subscribeOn(Schedulers.single())
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void multiplePublisherOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .publishOn(Schedulers.single())
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .publishOn(Schedulers.boundedElastic())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void publishAndSubscribeOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .publishOn(Schedulers.single())
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .subscribeOn(Schedulers.boundedElastic())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void subscribeAndPubliseOnSimple(){
        Flux<Integer> flux = Flux.range(1, 4)
            .subscribeOn(Schedulers.single())
            .map(i -> {
                log.info("Map 1 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            })
            .publishOn(Schedulers.boundedElastic())
            .map(i -> {
                log.info("Map 2 - Number {} on Thread {}", i, Thread.currentThread().getName());
                return i;
            });

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext(1, 2, 3, 4)
                    .verifyComplete();
    }

    @Test
    public void subscriberOnIO() throws InterruptedException{
        Mono<List<String>> list = Mono.fromCallable(() -> Files.readAllLines(Path.of("D:/project-reactor/src/test/java/dev/carlvs/test-file")))
                                    .log()
                                    .subscribeOn(Schedulers.boundedElastic());

        StepVerifier.create(list)
                    .expectSubscription()
                    .thenConsumeWhile(l -> {Assertions.assertFalse(l.isEmpty());
                                        log.info("{}", l.size());
                                        return true;})
                    .verifyComplete();
    }

    @Test
    public void switchIfEmptyOperator(){
        Flux<Object> flux = emptyFlux()
                            .switchIfEmpty(Flux.just("Not Empty Anymore"))
                            .log();

        StepVerifier.create(flux)
                    .expectSubscription()
                    .expectNext("Not Empty Anymore")
                    .expectComplete()
                    .verify();
    }

    private Flux<Object> emptyFlux(){
        return Flux.empty();
    }

    @Test
    public void deferOperator() throws Exception{
        Mono<Long> just = Mono.just(System.currentTimeMillis());
        Mono<Long> defer = Mono.defer(() -> Mono.just(System.currentTimeMillis()));

        just.subscribe(l -> log.info("Time Just {}", l));
        Thread.sleep(300);
        just.subscribe(l -> log.info("Time Just {}", l));
        Thread.sleep(300);
        defer.subscribe(l -> log.info("Time Defer {}", l));
        Thread.sleep(300);
        defer.subscribe(l -> log.info("Time Defer {}", l));

        AtomicLong atomicLong = new AtomicLong();
        defer.subscribe(atomicLong::set);
        Assertions.assertTrue(atomicLong.get() > 0);
    }

    @Test
    public void concatOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> concatFlux = Flux.concat(flux1, flux2).log();

        StepVerifier.create(concatFlux)
                    .expectSubscription()
                    .expectNext("a", "b", "c", "d")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void concatOperatorError(){
        Flux<String> flux1 = Flux.just("a", "b")
                                .map(s -> {
                                    if(s.equals("b")){
                                        throw new IllegalArgumentException();
                                    }
                                    return s;
                                });

        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> concatFlux = Flux.concatDelayError(flux1, flux2).log();

        StepVerifier.create(concatFlux)
                    .expectSubscription()
                    .expectNext("a", "c", "d")
                    .expectError()
                    .verify();
    }

    @Test
    public void concatWithOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> concatWithFlux = flux1.concatWith(flux2).log();

        StepVerifier.create(concatWithFlux)
                    .expectSubscription()
                    .expectNext("a", "b", "c", "d")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void combineLastestOperator(){
        Flux<String> flux1 = Flux.just("a", "b");
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> combineLastest = Flux.combineLatest(flux1, flux2, 
                                            (s1, s2) -> s1.toUpperCase() + s2.toUpperCase())
                                            .log();

        StepVerifier.create(combineLastest)
                    .expectSubscription()
                    .expectNext("BC", "BD")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void mergeOperator() throws InterruptedException{
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.merge(flux1, flux2)
                                    .delayElements(Duration.ofMillis(200))
                                    .log();

        Thread.sleep(1000);

        StepVerifier.create(mergeFlux)
                    .expectSubscription()
                    .expectNext("c", "d", "a", "b")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void mergeWithOperator() throws InterruptedException{
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux =flux1.mergeWith(flux2)
                                    .delayElements(Duration.ofMillis(200))
                                    .log();

        Thread.sleep(1000);

        StepVerifier.create(mergeFlux)
                    .expectSubscription()
                    .expectNext("c", "d", "a", "b")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void mergeSequentialsOperator() throws InterruptedException{
        Flux<String> flux1 = Flux.just("a", "b").delayElements(Duration.ofMillis(200));
        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.mergeSequential(flux1, flux2, flux1)
                                    .delayElements(Duration.ofMillis(200))
                                    .log();

        StepVerifier.create(mergeFlux)
                    .expectSubscription()
                    .expectNext("a", "b", "c", "d", "a", "b")
                    .expectComplete()
                    .verify();
    }

    @Test
    public void mergeDelayErrorOperator() throws InterruptedException{
        Flux<String> flux1 = Flux.just("a", "b")
                                .map(s -> {
                                    if(s.equals("b")){
                                        throw new IllegalArgumentException();
                                    }
                                    return s;
                                    }).doOnError(t -> log.error("We could do something with this"));

        Flux<String> flux2 = Flux.just("c", "d");

        Flux<String> mergeFlux = Flux.mergeDelayError(1, flux1, flux2, flux1)
                                    .log();

        StepVerifier.create(mergeFlux)
                    .expectSubscription()
                    .expectNext("a", "c", "d", "a")
                    .expectError()
                    .verify();
    }

    @Test
    public void flatMapOperator() throws Exception {
        Flux<String> flux = Flux.just("a", "b");

        Flux<String> flatFlux = flux.map(String::toUpperCase)
            .flatMap(this::findByName)
            .log();

        flatFlux.subscribe(s -> log.info(s.toString()));

        StepVerifier.create(flatFlux)
                    .expectSubscription()
                    .expectNext( "nameB1", "nameB2", "nameA1", "nameA2")
                    .verifyComplete();
    }

    @Test
    public void flatMapSequentialOperator() throws Exception {
        Flux<String> flux = Flux.just("a", "b");

        Flux<String> flatFlux = flux.map(String::toUpperCase)
            .flatMapSequential(this::findByName)
            .log();

        flatFlux.subscribe(s -> log.info(s.toString()));

        StepVerifier.create(flatFlux)
                    .expectSubscription()
                    .expectNext("nameA1", "nameA2", "nameB1", "nameB2")
                    .verifyComplete();
    }

    public Flux<String> findByName(String name){
        return name.equals("A") ? Flux.just("nameA1", "nameA2").delayElements(Duration.ofMillis(100))
                                         : Flux.just("nameB1", "nameB2");
    }

    @Test
    public void zipOperator(){
        Flux<String> titleFlux = Flux.just("Grand Blue", "Baki");
        Flux<String> studioflux = Flux.just("Zero-G", "TMS Enternaiment");
        Flux<Integer> episodesFlux = Flux.just(12, 24);

        Flux<Anime> animeFlux = Flux.zip(titleFlux, studioflux, episodesFlux)
                    .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), tuple.getT2(), tuple.getT3())));

        StepVerifier.create(animeFlux)
                    .expectSubscription()
                    .expectNext(new Anime("Grand Blue", "Zero-G", 12), 
                                new Anime("Baki", "TMS Enternaiment", 24))
                    .verifyComplete();
    }

    @Test
    public void zipWithOperator(){
        Flux<String> titleFlux = Flux.just("Grand Blue", "Baki");
        Flux<Integer> episodesFlux = Flux.just(12, 24);

        Flux<Anime> animeFlux = titleFlux.zipWith(episodesFlux)
                    .flatMap(tuple -> Flux.just(new Anime(tuple.getT1(), null, tuple.getT2())));

        StepVerifier.create(animeFlux)
                    .expectSubscription()
                    .expectNext(new Anime("Grand Blue", null, 12), 
                                new Anime("Baki", null, 24))
                    .verifyComplete();
    }

    @AllArgsConstructor
    @Getter
    @ToString
    @EqualsAndHashCode
    class Anime {
        private String title;
        private String studio;
        private int episodes;      
    }
}
