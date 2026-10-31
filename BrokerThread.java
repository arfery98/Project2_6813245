// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

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
            // TODO: wait for the coordinator's start-of-day signal.
            ClaimGateway gateway = claimGateways.get(ThreadLocalRandom.current().nextInt(claimGateways.size()));

            int newClaims = ThreadLocalRandom.current().nextInt(minDailyClaims, maxDailyClaims + 1);

            String threadName = Thread.currentThread().getName();

            System.out.printf("%s >> Day %d new claims = %d%n",
                    threadName, day, newClaims);

            int remainingClaims = newClaims;
            while (remainingClaims > 0) {
                int batchClaims = Math.min(remainingClaims, batchSize);

                // TODO: submit batchClaims to the selected ClaimGateway.
                System.out.printf("%s >> prepared batch = %d%n", threadName, batchClaims);
                remainingClaims -= batchClaims;

            }

            // TODO: signal completion and wait for the next phase/day.
            try {
                phaseBarrier.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (BrokenBarrierException e) {
                throw new IllegalStateException("Phase barrier failed", e);
            }
        }
    }
}
