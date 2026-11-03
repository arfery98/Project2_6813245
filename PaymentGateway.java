// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813246 Premprecha Pliensuwan
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

    public synchronized void resetDailyTokens() {
        remainingTokens = dailyTokens;
    }

    public synchronized PaymentResult issuePayments(int pendingPayments) {
        if (pendingPayments < 0) {
            throw new IllegalArgumentException("Pending payments cannot be negative");
        }
        int paid = Math.min(pendingPayments, remainingTokens);
        remainingTokens -= paid;
        return new PaymentResult(paid, remainingTokens);
    }

    public static final class PaymentResult {
        private final int paid;
        private final int remainingTokens;

        private PaymentResult(int paid, int remainingTokens) {
            this.paid = paid;
            this.remainingTokens = remainingTokens;
        }

        public int getPaid() {
            return paid;
        }

        public int getRemainingTokens() {
            return remainingTokens;
        }
    }
}
