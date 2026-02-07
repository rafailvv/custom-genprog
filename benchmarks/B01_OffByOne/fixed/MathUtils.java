public class MathUtils {
    public static int sum(int... numbers) {
        int total = 0;
        int i = 0;
        while (i < numbers.length) {
            total += numbers[i];
            i++;
        }
        return total;
    }

    public static int multiply(int... numbers) {
        int product = 1;
        int i = 0;
        while (i < numbers.length) {
            product *= numbers[i];
            i++;
        }
        return product;
    }
}
