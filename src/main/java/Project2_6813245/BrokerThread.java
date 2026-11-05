// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;

public class BrokerThread extends Thread {
    private final int days;
    private final int minDailyClaims;
    private final int maxDailyClaims;
    private final int batchSize;

    private final List<ClaimGateway> claimGateways;
    private final CyclicBarrier phaseBarrier;

    public BrokerThread(String name, int days, int minDailyClaims,
            int maxDailyClaims, int batchSize,
            List<ClaimGateway> claimGateways,
            CyclicBarrier phaseBarrier) {
        super(name);

        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative");
        }
        if (minDailyClaims < 0 || minDailyClaims > maxDailyClaims) {
            throw new IllegalArgumentException("daily claim range is invalid");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        this.days = days;

        if (claimGateways == null || claimGateways.isEmpty()) {
            throw new IllegalArgumentException("claimGateways must not be empty");
        }
        if (phaseBarrier == null) {
            throw new IllegalArgumentException("phaseBarrier must not be null");
        }

        this.minDailyClaims = minDailyClaims;
        this.maxDailyClaims = maxDailyClaims;
        this.batchSize = batchSize;
        this.claimGateways = claimGateways;
        this.phaseBarrier = phaseBarrier;
    }

    @Override
    public void run() {

        for (int day = 1; day <= days; day++) {
            try {
                phaseBarrier.await(); // Main has printed the day and reset tokens.
                int newClaims = (int) ThreadLocalRandom.current()
                        .nextLong(minDailyClaims, (long) maxDailyClaims + 1);
                String threadName = Thread.currentThread().getName();
                List<Integer> batches = new ArrayList<>();
                for (int remaining = newClaims; remaining > 0;) {
                    int batch = Math.min(remaining, batchSize);
                    batches.add(batch);
                    remaining -= batch;
                }
                System.out.printf("%s >> Day %d new claims = %d; split to %s%n",
                        threadName, day, newClaims, batches);
                ClaimGateway gateway = claimGateways.get(ThreadLocalRandom.current().nextInt(claimGateways.size()));
                for (int batchClaims : batches) {
                    synchronized (gateway) {
                        int pending = gateway.addClaims(batchClaims);
                        System.out.printf("%s >> submit %d claims to %s; pending claims = %d%n",
                                threadName, batchClaims, gateway.getName(), pending);
                    }
                }
                phaseBarrier.await(); // All brokers finished; assessors may work.
                phaseBarrier.await(); // All assessors finished; next day may start.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException e) {
                throw new IllegalStateException("Phase barrier failed", e);
            }
        }
    }
}

