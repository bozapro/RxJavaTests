package com.example.bajob.rxjavatests

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.IoScheduler
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExampleUnitTest2 {

    private fun log(msg: Any) {
        println("${Thread.currentThread().name}: $msg")
    }

    private fun cleanup(disposable: Disposable?) {
        if (disposable != null && !disposable.isDisposed) {
            disposable.dispose()
            log("Disposed")
        }
    }

    private fun loggingTransformer(): ObservableTransformer<Any, Any> {
        return ObservableTransformer { item ->
            item.doOnNext { it1 -> log("onNext $it1") }
                    .doOnError { log("onError $it") }
                    .doOnComplete { log("onComplete") }
        }
    }

    private fun someLongOperationThatReturnInteger(): Int {
        try {
            log("long runnig operation started")
            Thread.sleep(3000)
            log("long runnig operation finished")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return 123
    }

    @Test
    fun justTest() {
        val disposableObserver: DisposableObserver<String> = Observable.just("some string")
                .subscribeWith(object : DisposableObserver<String>() {
                    override fun onNext(s: String) {
                        log("onNext $s")
                    }

                    override fun onError(e: Throwable) {
                        log(" " + e.message)
                    }

                    override fun onComplete() {
                        log("onCompleted")
                    }
                })
        cleanup(disposableObserver)

        assertTrue(disposableObserver.isDisposed)
    }

    @Test
    fun justTest2() {
        val testCall = Observable.just("some string")
                .compose(loggingTransformer())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue("some string")

        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxJustLongOperation() {
        val testCall = Observable.just(someLongOperationThatReturnInteger())
                .compose(loggingTransformer())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(123)
                .assertNoErrors()

        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun justLongBackgroundOperationOperationRight() {

        val testCall = Observable.just(someLongOperationThatReturnInteger())
                .compose(loggingTransformer())
                .subscribeOn(IoScheduler())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(123)
                .assertNoErrors()


        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxJustLongBackgroundOperationThatReturnsObservable() {

        val testCall = Observable.defer { Observable.just(someLongOperationThatReturnInteger()) }
                .compose(loggingTransformer())
                .subscribeOn(IoScheduler())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(123)
                .assertNoErrors()


        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxJustLongBackgroundOperationThatReturnsSingle() {

        val testCall = Observable.fromCallable { someLongOperationThatReturnInteger() }
                .compose(loggingTransformer())
                .subscribeOn(IoScheduler())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(123)
                .assertNoErrors()


        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxTimer() {
        //read postDelay() aka timer(...)
        val timerObservable = Observable.timer(3, TimeUnit.SECONDS)

        val testCall = timerObservable
                .compose(loggingTransformer())
                .test()

        testCall.awaitTerminalEvent()
        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(0L)
                .assertNoErrors()

        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxTimerSwitchThreads() {
        val testCall = Observable.timer(3, TimeUnit.SECONDS)
                .doOnNext { log(it) }
                .observeOn(Schedulers.newThread())
                .compose(loggingTransformer())
                .test()

        testCall.awaitTerminalEvent()

        testCall.assertComplete()
                .assertValueCount(1)
                .assertValue(0L)
                .assertNoErrors()

        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }

    @Test
    fun rxInterval() {
        val testCall = Observable.interval(1000, 499, TimeUnit.MILLISECONDS)
                .compose(loggingTransformer())
                .test()

        testCall.await(5, TimeUnit.SECONDS)

        testCall.assertNotComplete()
                .assertValueCount(9)
                .assertValueSequence(listOf<Long>(0, 1, 2, 3, 4, 5, 6, 7, 8))
                .assertNoErrors()

        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }
}