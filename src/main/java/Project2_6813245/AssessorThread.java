// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

/** Integration implementation for Part 3; agree this API with its owner. */
public class AssessorThread extends Thread {
    private final int days;
    private final int dailyCapacity;
    private final List<ClaimGateway> claims;
    private final List<PaymentGateway> payments;
    private final CyclicBarrier phases;
    private long pendingPayments;
    private long successfulPayments;

    public AssessorThread(String name, int days, int dailyCapacity,
            List<ClaimGateway> claims, List<PaymentGateway> payments, CyclicBarrier phases) {
        super(name);
        if (days < 0 || dailyCapacity < 0 || claims == null || claims.isEmpty()
                || payments == null || payments.isEmpty() || phases == null) {
            throw new IllegalArgumentException("Invalid assessor parameters");
        }
        this.days = days;
        this.dailyCapacity = dailyCapacity;
        this.claims = claims;
        this.payments = payments;
        this.phases = phases;
    }

    // Main reads these only after join(), which makes completed worker writes visible.
    public long getSuccessfulPayments() { return successfulPayments; }
    public long getPendingPayments() { return pendingPayments; }

    @Override
    public void run() {
        try {
            for (int day = 1; day <= days; day++) {
                phases.await(); // Daily header and tokens are ready.
                phases.await(); // All brokers finished submitting.
                ClaimGateway gateway = claims.get(ThreadLocalRandom.current().nextInt(claims.size()));
                synchronized (gateway) {
                    int available = Math.min(dailyCapacity, gateway.getPendingClaims());
                    int approved = available == 0 ? 0
                            : (int) ThreadLocalRandom.current().nextLong(1, (long) available + 1);
                    pendingPayments += gateway.removeClaims(approved);
                    log("approve " + approved + " claims from " + gateway.getName()
                            + "; pending claims = " + gateway.getPendingClaims());
                }
                log("pending payments = " + pendingPayments);
                if (pendingPayments > 0) {
                    PaymentGateway payment = payments.get(ThreadLocalRandom.current().nextInt(payments.size()));
                    synchronized (payment) {
                        int paid = payment.issuePayments(pendingPayments);
                        pendingPayments -= paid;
                        successfulPayments += paid;
                        log("issue " + paid + " payments to " + payment.getName()
                                + "; tokens = " + payment.getRemainingTokens());
                    }
                }
                phases.await(); // Every assessor must arrive, including those with no work.
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException("Phase barrier failed", e);
        }
    }

    private void log(String message) {
        System.out.printf("%s >> %s%n", Thread.currentThread().getName(), message);
    }
}
