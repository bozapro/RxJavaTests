package com.example.bajob.rxjavatests

import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.Disposable
import io.reactivex.internal.schedulers.IoScheduler
import io.reactivex.observers.DisposableObserver
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
    fun justLongBackgroundOperationOperationRight(){

        val just = Observable.just(someLongOperationThatReturnInteger())
                .compose(loggingTransformer())
                .subscribeOn(IoScheduler())

        val testCall = just.test()
        testCall.await(5, TimeUnit.SECONDS)


        cleanup(testCall)
        assertTrue(testCall.isDisposed)
    }
}