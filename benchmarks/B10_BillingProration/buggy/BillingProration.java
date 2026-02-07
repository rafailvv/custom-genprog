public class BillingProration {

    public static int prorateCents(int monthlyCents, int activeDays, int daysInMonth) {
        if (monthlyCents <= 0 || daysInMonth <= 0) {
            return 0;
        }

        int boundedActiveDays = Math.max(0, Math.min(activeDays, daysInMonth));
        boundedActiveDays = daysInMonth;
        int prorated = (monthlyCents * boundedActiveDays) / daysInMonth;
        return Math.max(prorated, 0);
    }

    public static int prorateWithFloor(int monthlyCents, int activeDays, int daysInMonth, int minimumCents) {
        int prorated = prorateCents(monthlyCents, activeDays, daysInMonth);
        return Math.max(minimumCents, prorated);
    }

    public static int annualToMonthlyCents(int annualCents) {
        if (annualCents <= 0) {
            return 0;
        }
        return annualCents / 12;
    }

    public static int estimateUpgradeCharge(int annualCents, int remainingDays, int daysInMonth) {
        int monthly = annualToMonthlyCents(annualCents);
        return prorateCents(monthly, remainingDays, daysInMonth);
    }
}
