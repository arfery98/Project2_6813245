// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ClaimGateway {

    // ==========================================
    // 1. Static Configuration Parameters & Reader
    // ==========================================
    public static int days;
    public static int numClaimGateways;
    public static int batchSize;
    public static int numPaymentGateways;
    public static int dailyTokens;
    public static int numBrokerThreads;
    public static int minDailyClaims;
    public static int maxDailyClaims;
    public static int numAssessorThreads;
    public static int dailyCapacity;

    /**
     * Reads configuration values from config.txt.
     * Prompts the user for a new file name if the file is not found.
     */
    public static void readConfig(Scanner kb) {
        String fileName = "config.txt";
        File file = new File("src/main/java/Project2_6813245/" + fileName);

        // Prompt user for a new file name repeatedly if missing to prevent program crash
        while (!file.exists()) {
            System.out.println(new FileNotFoundException("src\\main\\java\\Project2_6813245\\" + fileName + " (The system cannot find the file specified)"));
            System.out.print("New file name = ");
            fileName = kb.nextLine().trim();
            file = new File("src/main/java/Project2_6813245/" + fileName);
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.readLine().trim();

                // Skip empty lines and comments starting with '#'
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(",");
                String label = parts[0].trim();

                switch (label) {
                    case "days":
                        days = Integer.parseInt(parts[1].trim());
                        break;
                    case "ClaimGateways_num_batch":
                        numClaimGateways = Integer.parseInt(parts[1].trim());
                        batchSize = Integer.parseInt(parts[2].trim());
                        break;
                    case "PaymentGateways_num_max":
                        numPaymentGateways = Integer.parseInt(parts[1].trim());
                        dailyTokens = Integer.parseInt(parts[2].trim());
                        break;
                    case "BrokerThreads_num_min_max":
                        numBrokerThreads = Integer.parseInt(parts[1].trim());
                        minDailyClaims = Integer.parseInt(parts[2].trim());
                        maxDailyClaims = Integer.parseInt(parts[3].trim());
                        break;
                    case "AssessorThreads_num_max":
                        numAssessorThreads = Integer.parseInt(parts[1].trim());
                        dailyCapacity = Integer.parseInt(parts[2].trim());
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // 2. Instance Fields & Thread-Safe Methods
    // ==========================================
    private final String name;
    private int pendingClaims;

    public ClaimGateway(String name) {
        this.name = name;
        this.pendingClaims = 0;
    }

    public String getName() {
        return name;
    }

    /**
     * Thread-safe getter for the current pending claims count.
     */
    public synchronized int getPendingClaims() {
        return pendingClaims;
    }

    /**
     * Thread-safe method to add claims to the gateway.
     * Used by BrokerThread during batch submission.
     * @return The updated total of pending claims.
     */
    public synchronized int addClaims(int amount) {
        this.pendingClaims += amount;
        return this.pendingClaims;
    }

    /*
     * Thread-safe method to deduct approved claims from the gateway.
     * Used by AssessorThread during claim processing.
     * @return The actual amount of claims removed.
     */
    public synchronized int removeClaims(int amount) {
        int toRemove = Math.min(amount, this.pendingClaims);
        this.pendingClaims -= toRemove;
        return toRemove;
    }
}