6813110 Pattavee Nimkulrat
6813113 Rachata Charoenboophon
6813245 Thanaporn Songthong
6813246 Premprecha Pliensuwan
6813257 Sarochphan Nikrothanonda

PART 5 - MAIN AND THREAD COORDINATION

Run the following commands from the project root (the folder containing src):
  javac -d build/classes src/main/java/Project2_6813245/*.java
  java -cp build/classes Project2_6813245.Main config_1.txt

Without an argument Main first looks for config.txt. If it is missing, enter
config_1.txt (or another file name). Files are read relative to
src/main/java/Project2_6813245, as required by the assignment.

Main creates the shared gateway lists and all threads from config values.
All workers and Main share ONE CyclicBarrier. Every day ALL participants
must call await() exactly THREE times in this order:
  1. Main prints the day, pending claims, and resets payment tokens, then
     everyone meets at the first barrier.
  2. Brokers submit all batches. Assessors and Main wait at the second barrier.
  3. Assessors approve claims and issue payments. Brokers and Main wait at
     the third barrier before the next day.
Threads are created once, started together, and joined after all days.
Main sorts the final report by successful payments descending, then thread
name ascending for ties. Workers sharing a gateway synchronize on that
gateway; different gateways can operate concurrently.

INTEGRATION CHANGES TO PARTS 1 AND 2
BrokerThread now waits at all three barriers and actually calls addClaims.
ClaimGateway uses Scanner.nextLine() and reports missing files with the
current thread name. EOF and configuration read errors stop cleanly.

PARTS 3 AND 4 - SUPPORT IMPLEMENTATIONS
AssessorThread.java and PaymentGateway.java were not present in the linked
repositories. These local implementations allow Part 5 to be compiled and
tested. Agree their API and the three-barrier protocol with the owners of
Parts 3 and 4 before replacing them with the team's implementations.

Expected API for Part 3:
  AssessorThread(String name, int days, int dailyCapacity,
      List<ClaimGateway> claims, List<PaymentGateway> payments,
      CyclicBarrier phases)
  long getSuccessfulPayments()  // read by Main after join()
  long getPendingPayments()     // optional inspection after join()
  getName(), start(), join() inherited from Thread

Expected API for Part 4:
  PaymentGateway(String name, int dailyTokens)
  String getName()
  synchronized void resetTokens()
  synchronized int getRemainingTokens()
  synchronized int issuePayments(long requested) // returns actual paid count

Integration verification (Python standard library and a JDK required):
  python tests/verify_simulation.py

Submit the Project2_6813245 package folder including the Java files, config
with its original comment lines, and this readme.
