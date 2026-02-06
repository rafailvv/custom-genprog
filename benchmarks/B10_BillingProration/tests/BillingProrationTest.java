import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BillingProrationTest {

    @Test
    public void prorateCents_handlesNonEvenDivision() {
        assertEquals(233, BillingProration.prorateCents(1000, 7, 30));
    }

    @Test
    public void prorateCents_clampsActiveDaysToMonthLength() {
        assertEquals(1000, BillingProration.prorateCents(1000, 44, 30));
    }

    @Test
    public void prorateCents_handlesInvalidInputs() {
        assertEquals(0, BillingProration.prorateCents(-10, 10, 30));
        assertEquals(0, BillingProration.prorateCents(1000, 10, 0));
    }

    @Test
    public void prorateWithFloor_appliesMinimum() {
        assertEquals(300, BillingProration.prorateWithFloor(1000, 1, 30, 300));
    }

    @Test
    public void estimateUpgradeCharge_usesAnnualConversion() {
        assertEquals(1000, BillingProration.estimateUpgradeCharge(12000, 30, 30));
    }
}
