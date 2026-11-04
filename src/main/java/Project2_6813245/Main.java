// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
    public static void main(String[] args) {
        try (Scanner keyboard = new Scanner(System.in)) {
            ClaimGateway.readConfig(keyboard, args.length == 0 ? "config.txt" : args[0]);
            runSimulation();
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.printf("%s >> Simulation failed: %s%n",
                    Thread.currentThread().getName(), e.getMessage());
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.printf("%s >> Simulation interrupted%n", Thread.currentThread().getName());
            System.exit(1);
        }
    }

    static void runSimulation() throws InterruptedException {
        validateConfig();
        List<ClaimGateway> claims = new ArrayList<>();
        List<PaymentGateway> payments = new ArrayList<>();
        List<BrokerThread> brokers = new ArrayList<>();
        List<AssessorThread> assessors = new ArrayList<>();
        List<Thread> workers = new ArrayList<>();

        // Everyone calls await three times per day, in the same order:
        // day ready -> brokers complete -> assessors complete.
        CyclicBarrier phases = new CyclicBarrier(1 + ClaimGateway.numBrokerThreads
                + ClaimGateway.numAssessorThreads);
        for (int i = 0; i < ClaimGateway.numClaimGateways; i++) {
            claims.add(new ClaimGateway("ClaimGateway_" + i));
        }
        for (int i = 0; i < ClaimGateway.numPaymentGateways; i++) {
            payments.add(new PaymentGateway("PaymentGateway_" + i, ClaimGateway.dailyTokens));
        }
        for (int i = 0; i < ClaimGateway.numBrokerThreads; i++) {
            brokers.add(new BrokerThread("BrokerThread_" + i, ClaimGateway.days,
                    ClaimGateway.minDailyClaims, ClaimGateway.maxDailyClaims,
                    ClaimGateway.batchSize, claims, phases));
        }
        for (int i = 0; i < ClaimGateway.numAssessorThreads; i++) {
            assessors.add(new AssessorThread("AssessorThread_" + i, ClaimGateway.days,
                    ClaimGateway.dailyCapacity, claims, payments, phases));
        }
        workers.addAll(brokers);
        workers.addAll(assessors);

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread coordinator = Thread.currentThread();
        for (Thread worker : workers) {
            // Wake Main if a worker fails instead of leaving it at a barrier forever.
            worker.setUncaughtExceptionHandler((thread, error) -> {
                failure.compareAndSet(null, error);
                coordinator.interrupt();
            });
        }
        printParameters(claims, payments, brokers, assessors);
        try {
            for (Thread worker : workers) {
                worker.start();
            }
            for (int day = 1; day <= ClaimGateway.days; day++) {
                log("==================================================");
                log("Day " + day);
                for (ClaimGateway gateway : claims) {
                    log(gateway.getName() + " pending claims = " + gateway.getPendingClaims());
                }
                for (PaymentGateway gateway : payments) {
                    gateway.resetTokens();
                    log("reset " + gateway.getName() + " tokens = " + gateway.getRemainingTokens());
                }
                phases.await(); // Release all workers after the daily header.
                phases.await(); // Wait for every broker to finish submitting.
                phases.await(); // Wait for every assessor to finish payment work.
            }
            for (Thread worker : workers) {
                worker.join();
            }
        } catch (InterruptedException e) {
            if (failure.get() != null) {
                throw new IllegalStateException("A worker failed", failure.get());
            }
            throw e;
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException("Daily synchronization failed", e);
        } finally {
            // Interrupt first, then join all workers, even when Main is interrupted.
            for (Thread worker : workers) {
                if (worker.isAlive()) worker.interrupt();
            }
            boolean interrupted = Thread.interrupted();
            for (Thread worker : workers) {
                while (worker.isAlive()) {
                    try {
                        worker.join();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
        if (failure.get() != null) {
            throw new IllegalStateException("A worker failed", failure.get());
        }
        assessors.sort(Comparator.comparingLong(AssessorThread::getSuccessfulPayments)
                .reversed().thenComparing(AssessorThread::getName));
        log("==================================================");
        log("Summary");
        for (AssessorThread assessor : assessors) {
            log(assessor.getName() + " successful payments = " + assessor.getSuccessfulPayments());
        }
    }

    private static void validateConfig() {
        if (ClaimGateway.days < 0 || ClaimGateway.numClaimGateways <= 0
                || ClaimGateway.numPaymentGateways <= 0 || ClaimGateway.batchSize <= 0
                || ClaimGateway.dailyTokens < 0 || ClaimGateway.numBrokerThreads < 0
                || ClaimGateway.numAssessorThreads < 0 || ClaimGateway.minDailyClaims < 0
                || ClaimGateway.maxDailyClaims < ClaimGateway.minDailyClaims
                || ClaimGateway.dailyCapacity < 0) {
            throw new IllegalArgumentException("Invalid simulation parameters");
        }
    }

    private static void printParameters(List<ClaimGateway> claims, List<PaymentGateway> payments,
            List<BrokerThread> brokers, List<AssessorThread> assessors) {
        log("Parameters");
        log("Days of simulation = " + ClaimGateway.days);
        List<String> names = new ArrayList<>();
        for (ClaimGateway gateway : claims) names.add(gateway.getName());
        log("ClaimGateways = " + names);
        log("Batch size = " + ClaimGateway.batchSize);
        names.clear();
        for (PaymentGateway gateway : payments) names.add(gateway.getName());
        log("PaymentGateways = " + names);
        log("Daily tokens = " + ClaimGateway.dailyTokens);
        names.clear();
        for (Thread broker : brokers) names.add(broker.getName());
        log("BrokerThreads = " + names);
        log("Daily claims: min = " + ClaimGateway.minDailyClaims + ", max = " + ClaimGateway.maxDailyClaims);
        names.clear();
        for (Thread assessor : assessors) names.add(assessor.getName());
        log("AssessorThreads = " + names);
        log("Daily capacity = " + ClaimGateway.dailyCapacity);
    }

    private static void log(String message) {
        System.out.printf("%s >> %s%n", Thread.currentThread().getName(), message);
    }
}
