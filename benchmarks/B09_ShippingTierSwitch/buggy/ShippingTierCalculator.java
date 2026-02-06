import java.util.Locale;

public class ShippingTierCalculator {

    public static String normalizeTier(String tier) {
        if (tier == null) {
            return "STANDARD";
        }

        String normalized = tier.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "STANDARD";
        }
        return normalized;
    }

    public static int deliveryDays(String tier) {
        String normalized = normalizeTier(tier);
        int days;

        switch (normalized) {
            case "STANDARD":
                days = 5;
                break;
            case "EXPRESS":
                days = 2;
                break;
            case "OVERNIGHT":
                days = 1;
                break;
            default:
                days = 7;
                break;
        }

        return days;
    }

    public static int baseFeeCents(String tier) {
        switch (normalizeTier(tier)) {
            case "STANDARD":
                return 450;
            case "EXPRESS":
                return 800;
            case "OVERNIGHT":
                return 1300;
            default:
                return 600;
        }
    }

    public static int estimateShippingCents(int grams, String tier, boolean weekendDelivery) {
        int normalizedWeight = Math.max(1, grams);
        int blocks = (normalizedWeight + 499) / 500;

        int price = baseFeeCents(tier);
        price += (blocks - 1) * 120;
        if (weekendDelivery == false) {
            price += 250;
        }

        return price;
    }
}
