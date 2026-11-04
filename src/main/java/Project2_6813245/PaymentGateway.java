// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

/** Integration implementation for Part 4; agree this API with its owner. */
public class PaymentGateway {
    private final String name;
    private final int dailyTokens;
    private int remainingTokens;

    public PaymentGateway(String name, int dailyTokens) {
        if (dailyTokens < 0) throw new IllegalArgumentException("dailyTokens must not be negative");
        this.name = name;
        this.dailyTokens = dailyTokens;
        this.remainingTokens = dailyTokens;
    }

    public String getName() { return name; }

    public synchronized void resetTokens() { remainingTokens = dailyTokens; }

    public synchronized int getRemainingTokens() { return remainingTokens; }

    public synchronized int issuePayments(long requested) {
        if (requested < 0) throw new IllegalArgumentException("requested must not be negative");
        int paid = (int) Math.min(requested, remainingTokens);
        remainingTokens -= paid;
        return paid;
    }
}
