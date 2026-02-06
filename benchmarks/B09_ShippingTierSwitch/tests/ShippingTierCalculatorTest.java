import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShippingTierCalculatorTest {

    @Test
    public void normalizeTier_trimsAndUppercases() {
        assertEquals("EXPRESS", ShippingTierCalculator.normalizeTier("  express "));
    }

    @Test
    public void deliveryDays_expressHasTwoDays() {
        assertEquals(2, ShippingTierCalculator.deliveryDays("express"));
    }

    @Test
    public void deliveryDays_unknownFallsBackToDefault() {
        assertEquals(7, ShippingTierCalculator.deliveryDays("drone"));
    }

    @Test
    public void estimateShipping_addsWeightAndWeekendSurcharge() {
        assertEquals(1290, ShippingTierCalculator.estimateShippingCents(1200, "express", true));
    }

    @Test
    public void estimateShipping_handlesNullTierAsStandard() {
        assertEquals(570, ShippingTierCalculator.estimateShippingCents(800, null, false));
    }
}
