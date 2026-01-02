public class ArraySearch {
    public static int findIndex(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) {
                return i;
            }
        }
        return -1;
    }

    public static boolean contains(int[] array, int value) {
        return findIndex(array, value) >= 0;
    }
}

