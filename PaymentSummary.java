// 6813110 Pattavee Nimkulrat
// 6813113 Rachata Charoenboophon
// 6813245 Thanaporn Songthong
// 6813246 Premprecha Pliensuwan
// 6813257 Sarochphan Nikrothanonda
package Project2_6813245;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

public final class PaymentSummary {
    private PaymentSummary() { }

    public static <T> List<T> sorted(List<T> assessors,
            Function<T, String> name, ToIntFunction<T> successfulPayments) {
        List<T> result = new ArrayList<>(assessors);
        result.sort(Comparator.<T>comparingInt(successfulPayments)
                .reversed().thenComparing(name));
        return result;
    }
}
