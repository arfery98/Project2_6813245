// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

public final class PaymentGateway {
    private final String name;
    private final int dailyTokens;
    private int remainingTokens;

    public PaymentGateway(String name, int dailyTokens) {
        if (name == null || name.trim().isEmpty() || dailyTokens < 0) {
            throw new IllegalArgumentException("Invalid gateway name or token count");
        }
        this.name = name;
        this.dailyTokens = dailyTokens;
        this.remainingTokens = dailyTokens;
    }

    public String getName() {
        return name;
    }

    public synchronized int getRemainingTokens() {
        return remainingTokens;
    }

    public synchronized void resetTokens() {
        remainingTokens = dailyTokens;
    }

    public synchronized int issuePayments(long requested) {
        if (requested < 0) {
            throw new IllegalArgumentException("Pending payments cannot be negative");
        }

        int paid = (int) Math.min(requested, remainingTokens);
        remainingTokens -= paid;
        return paid;
    }
}
