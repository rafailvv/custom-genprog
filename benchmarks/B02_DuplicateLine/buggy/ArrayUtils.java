public class ArrayUtils {
    public static int findIndexOf(int value, int... array) {
        int index = 0;
        while (index < array.length) {
            if (array[index] == value) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int countOccurrences(int value, int... array) {
        int count = 0;
        int index = 0;
        while (index < array.length) {
            if (array[index] == value) {
                count++;
                count++;
            }
            index++;
        }
        return count;
    }
}
